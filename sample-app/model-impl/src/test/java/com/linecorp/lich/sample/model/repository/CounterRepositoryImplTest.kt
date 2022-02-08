/*
 * Copyright 2021 LINE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linecorp.lich.sample.model.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.component.test.mockitokotlin.mockComponent
import com.linecorp.lich.sample.model.db.CounterDao
import com.linecorp.lich.sample.model.db.CounterDatabase
import com.linecorp.lich.sample.model.entity.Counter
import com.linecorp.lich.sample.model.remote.CounterServiceClient
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class CounterRepositoryImplTest {

    private lateinit var mockCounterServiceClient: CounterServiceClient

    private lateinit var mockCounterDao: CounterDao

    private lateinit var counterRepository: CounterRepositoryImpl

    @Before
    fun setUp() {
        // Initializes mocks to be injected into CounterRepositoryImpl.
        mockCounterServiceClient = mockComponent(CounterServiceClient)
        mockCounterDao = mock()
        mockComponent(CounterDatabase) {
            on { counterDao } doReturn mockCounterDao
        }

        counterRepository = CounterRepositoryImpl().also {
            it.init(ApplicationProvider.getApplicationContext())
        }
    }

    @Test
    fun getCounterExisting() = runBlocking<Unit> {
        val counterName = "foo"
        val counter = Counter(counterName, 20)
        whenever(mockCounterDao.find(counterName)) doReturn counter

        val actual = counterRepository.getCounter(counterName)

        assertEquals(CounterResult.Success(counter), actual)
        verify(mockCounterServiceClient, never()).getInitialCounterValue(any())
    }

    @Test
    fun getCounterNew() = runBlocking {
        val counterName = "foo"
        whenever(mockCounterDao.find(any())) doReturn null
        whenever(mockCounterServiceClient.getInitialCounterValue(counterName)) doReturn 42

        val actual = counterRepository.getCounter(counterName)

        val counter = Counter(counterName, 42)
        assertEquals(CounterResult.Success(counter), actual)
        verify(mockCounterDao).replace(counter)
    }

    @Test
    fun getCounterNetworkError() = runBlocking {
        whenever(mockCounterDao.find(any())) doReturn null
        whenever(mockCounterServiceClient.getInitialCounterValue(any())) doAnswer { throw IOException() }

        val actual = counterRepository.getCounter("foo")

        assertEquals(CounterResult.NetworkError, actual)
    }
}

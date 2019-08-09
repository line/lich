/*
 * Copyright 2019 LINE Corporation
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
package com.linecorp.lich.sample.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.sample.db.CounterDao
import com.linecorp.lich.sample.entity.Counter
import com.linecorp.lich.sample.remote.CounterServiceClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class CounterRepositoryTest {

    private lateinit var counterServiceClient: CounterServiceClient

    private lateinit var counterDao: CounterDao

    private lateinit var counterRepository: CounterRepository

    @Before
    fun setUp() {
        counterServiceClient = mock()
        counterDao = mock()
        counterRepository = CounterRepository(counterServiceClient, counterDao)
    }

    @Test
    fun getCounterExisting() = runBlocking<Unit> {
        val counterName = "foo"
        val counter = Counter(counterName, 20)
        whenever(counterDao.find(eq(counterName))).thenReturn(counter)

        val actual = counterRepository.getCounter(counterName)

        assertEquals(CounterResult.Success(counter), actual)
        verify(counterServiceClient, never()).getInitialCounterValue(any())
    }

    @Test
    fun getCounterNew() = runBlocking {
        val counterName = "foo"
        whenever(counterDao.find(any())).thenReturn(null)
        whenever(counterServiceClient.getInitialCounterValue(eq(counterName))).thenReturn(42)

        val actual = counterRepository.getCounter(counterName)

        val counter = Counter(counterName, 42)
        assertEquals(CounterResult.Success(counter), actual)
        verify(counterDao).replace(eq(counter))
    }

    @Test
    fun getCounterNetworkError() = runBlocking {
        whenever(counterDao.find(any())).thenReturn(null)
        whenever(counterServiceClient.getInitialCounterValue(any())).thenThrow(IOException())

        val actual = counterRepository.getCounter("foo")

        assertEquals(CounterResult.NetworkError, actual)
    }
}

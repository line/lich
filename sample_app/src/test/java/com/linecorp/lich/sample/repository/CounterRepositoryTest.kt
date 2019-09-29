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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
        counterServiceClient = mockk()
        counterDao = mockk(relaxUnitFun = true)
        counterRepository = CounterRepository(counterServiceClient, counterDao)
    }

    @Test
    fun getCounterExisting() = runBlocking {
        val counterName = "foo"
        val counter = Counter(counterName, 20)
        coEvery { counterDao.find(counterName) } returns counter

        val actual = counterRepository.getCounter(counterName)

        assertEquals(CounterResult.Success(counter), actual)
        coVerify(exactly = 0) { counterServiceClient.getInitialCounterValue(any()) }
    }

    @Test
    fun getCounterNew() = runBlocking {
        val counterName = "foo"
        coEvery { counterDao.find(any()) } returns null
        coEvery { counterServiceClient.getInitialCounterValue(counterName) } returns 42

        val actual = counterRepository.getCounter(counterName)

        val counter = Counter(counterName, 42)
        assertEquals(CounterResult.Success(counter), actual)
        coVerify(exactly = 1) { counterDao.replace(counter) }
    }

    @Test
    fun getCounterNetworkError() = runBlocking {
        coEvery { counterDao.find(any()) } returns null
        coEvery { counterServiceClient.getInitialCounterValue(any()) } throws IOException()

        val actual = counterRepository.getCounter("foo")

        assertEquals(CounterResult.NetworkError, actual)
    }
}

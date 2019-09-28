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
package com.linecorp.lich.sample.mvvm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.component.test.mockitokotlin.mockComponent
import com.linecorp.lich.sample.entity.Counter
import com.linecorp.lich.sample.repository.CounterRepository
import com.linecorp.lich.sample.repository.CounterResult
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verifyBlocking
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class CounterUseCaseTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun loadCounterSuccess() {
        mockComponent(CounterRepository) {
            onBlocking { getCounter("foo") } doReturn
                CounterResult.Success(Counter("foo", 42))
        }
        val counterUseCase = CounterUseCase(context, "foo")

        assertEquals(null, counterUseCase.liveCounter.value)

        runBlocking { counterUseCase.loadCounter() }

        assertEquals(Counter("foo", 42), counterUseCase.liveCounter.value)
    }

    @Test
    fun loadCounterNetworkError() {
        mockComponent(CounterRepository) {
            onBlocking { getCounter("foo") } doReturn CounterResult.NetworkError
        }
        val counterUseCase = CounterUseCase(context, "foo")

        assertEquals(null, counterUseCase.liveCounter.value)

        runBlocking { counterUseCase.loadCounter() }

        assertEquals(null, counterUseCase.liveCounter.value)
    }

    @Test
    fun loadCounterIsLoading() {
        val counterUseCase = CounterUseCase(context, "foo")
        mockComponent(CounterRepository) {
            onBlocking { getCounter(any()) } doAnswer {
                assertEquals(true, counterUseCase.isLoading.value)
                CounterResult.NetworkError
            }
        }

        assertEquals(false, counterUseCase.isLoading.value)

        runBlocking { counterUseCase.loadCounter() }

        assertEquals(false, counterUseCase.isLoading.value)
    }

    @Test
    fun changeCounterValue() {
        val counterRepository = mockComponent(CounterRepository)
        val counterUseCase = CounterUseCase(context, "foo")
        counterUseCase.liveCounter.value = Counter("foo", 42)

        runBlocking { counterUseCase.changeCounterValue(1) }

        val expected = Counter("foo", 43)
        assertEquals(expected, counterUseCase.liveCounter.value)
        verifyBlocking(counterRepository) { storeCounter(expected) }
    }

    @Test
    fun deleteCounter() {
        val counterRepository = mockComponent(CounterRepository)
        val counterUseCase = CounterUseCase(context, "foo")
        counterUseCase.liveCounter.value = Counter("foo", 42)

        runBlocking { counterUseCase.deleteCounter() }

        assertEquals(null, counterUseCase.liveCounter.value)
        verifyBlocking(counterRepository) { deleteCounter(Counter("foo", 42)) }
    }
}

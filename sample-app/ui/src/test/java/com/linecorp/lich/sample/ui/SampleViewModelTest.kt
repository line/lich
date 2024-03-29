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
package com.linecorp.lich.sample.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.sample.model.entity.Counter
import com.linecorp.lich.savedstate.createSavedStateHandleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoMoreInteractions

@ExperimentalCoroutinesApi // For UnconfinedTestDispatcher, etc.
@RunWith(AndroidJUnit4::class)
class SampleViewModelTest {

    private lateinit var context: Context

    private lateinit var mockLiveCounter: MutableLiveData<Counter?>

    private lateinit var mockIsLoading: MutableLiveData<Boolean>

    private lateinit var mockCounterUseCase: CounterUseCase

    private lateinit var sampleViewModel: SampleViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        context = ApplicationProvider.getApplicationContext()

        mockLiveCounter = MutableLiveData(null)
        mockIsLoading = MutableLiveData(false)
        mockCounterUseCase = mock {
            on { liveCounter } doReturn mockLiveCounter
            on { isLoading } doReturn mockIsLoading
        }

        val savedState = createSavedStateHandleForTesting(
            SampleViewModelArgs(counterName = "foo")
        )
        sampleViewModel = SampleViewModel(context, savedState, mockCounterUseCase)
    }

    @After
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLiveData() = runTest {
        mockIsLoading.value = true

        val counterTextObserver = sampleViewModel.counterText.observeWithMock()
        val isOperationEnabledObserver = sampleViewModel.isOperationEnabled.observeWithMock()
        val isLoadingObserver = sampleViewModel.isLoading.observeWithMock()

        verify(counterTextObserver).onChanged("No value.")
        verify(isOperationEnabledObserver).onChanged(false)
        verify(isLoadingObserver).onChanged(true)
        verifyNoMoreInteractions(counterTextObserver, isOperationEnabledObserver, isLoadingObserver)

        mockLiveCounter.value = Counter("foo", 42)
        mockIsLoading.value = false

        verify(counterTextObserver).onChanged("42")
        verify(isOperationEnabledObserver).onChanged(true)
        verify(isLoadingObserver).onChanged(false)
        verifyNoMoreInteractions(counterTextObserver, isOperationEnabledObserver, isLoadingObserver)
    }

    private fun <T> LiveData<T>.observeWithMock(): Observer<T> =
        mock<Observer<T>>().also {
            this.observeForever(it)
        }

    @Test
    fun loadData() = runTest {
        sampleViewModel.loadData()
        runCurrent()

        verifyBlocking(mockCounterUseCase) { loadCounter("foo") }
    }

    @Test
    fun countUp() = runTest {
        sampleViewModel.countUp()
        runCurrent()

        verifyBlocking(mockCounterUseCase) { changeCounterValue(1) }
    }

    @Test
    fun countDown() = runTest {
        sampleViewModel.countDown()
        runCurrent()

        verifyBlocking(mockCounterUseCase) { changeCounterValue(-1) }
    }

    @Test
    fun deleteCounter() = runTest {
        sampleViewModel.deleteCounter()
        runCurrent()

        verifyBlocking(mockCounterUseCase) { deleteCounter() }
    }
}

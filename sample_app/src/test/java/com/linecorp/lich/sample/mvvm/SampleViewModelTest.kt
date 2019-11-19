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
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.sample.entity.Counter
import com.linecorp.lich.viewmodel.test.createSavedStateForTesting
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi // For TestCoroutineDispatcher
@RunWith(AndroidJUnit4::class)
class SampleViewModelTest {

    private val testDispatcher = TestCoroutineDispatcher()

    private lateinit var context: Context

    private lateinit var mockLiveCounter: MutableLiveData<Counter?>

    private lateinit var mockIsLoading: MutableLiveData<Boolean>

    private lateinit var mockCounterUseCase: CounterUseCase

    private lateinit var sampleViewModel: SampleViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        context = ApplicationProvider.getApplicationContext()

        mockLiveCounter = MutableLiveData(null)
        mockIsLoading = MutableLiveData(false)
        mockCounterUseCase = mockk(relaxUnitFun = true) {
            every { liveCounter } returns mockLiveCounter
            every { isLoading } returns mockIsLoading
        }

        val savedState = createSavedStateForTesting(
            SampleViewModelArgs(counterName = "foo")
        )
        sampleViewModel = SampleViewModel(context, savedState, mockCounterUseCase)
    }

    @After
    fun cleanUp() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun testLiveData() {
        mockIsLoading.value = true

        val counterTextObserver = sampleViewModel.counterText.observeWithMockk()
        val loadingVisibilityObserver = sampleViewModel.loadingVisibility.observeWithMockk()
        val isOperationEnabledObserver = sampleViewModel.isOperationEnabled.observeWithMockk()

        verify(exactly = 1) { counterTextObserver.onChanged("No value.") }
        verify(exactly = 1) { loadingVisibilityObserver.onChanged(View.VISIBLE) }
        verify(exactly = 1) { isOperationEnabledObserver.onChanged(false) }
        confirmVerified(counterTextObserver, loadingVisibilityObserver, isOperationEnabledObserver)

        mockLiveCounter.value = Counter("foo", 42)
        mockIsLoading.value = false

        verify(exactly = 1) { counterTextObserver.onChanged("42") }
        verify(exactly = 1) { loadingVisibilityObserver.onChanged(View.GONE) }
        verify(exactly = 1) { isOperationEnabledObserver.onChanged(true) }
        confirmVerified(counterTextObserver, loadingVisibilityObserver, isOperationEnabledObserver)
    }

    private fun <T> LiveData<T>.observeWithMockk(): Observer<T> =
        mockk<Observer<T>>(relaxUnitFun = true).also {
            this.observeForever(it)
        }

    @Test
    fun loadData() {
        sampleViewModel.loadData()

        coVerify { mockCounterUseCase.loadCounter("foo") }
    }

    @Test
    fun countUp() {
        sampleViewModel.countUp()

        coVerify { mockCounterUseCase.changeCounterValue(1) }
    }

    @Test
    fun countDown() {
        sampleViewModel.countDown()

        coVerify { mockCounterUseCase.changeCounterValue(-1) }
    }

    @Test
    fun deleteCounter() {
        sampleViewModel.deleteCounter()

        coVerify { mockCounterUseCase.deleteCounter() }
    }
}

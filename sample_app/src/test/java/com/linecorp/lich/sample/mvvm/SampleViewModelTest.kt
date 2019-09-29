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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.sample.entity.Counter
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SampleViewModelTest {

    private lateinit var context: Context

    private lateinit var liveCounter: MutableLiveData<Counter?>

    private lateinit var isLoading: MutableLiveData<Boolean>

    private lateinit var counterUseCase: CounterUseCase

    private lateinit var sampleViewModel: SampleViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        liveCounter = MutableLiveData(null)
        isLoading = MutableLiveData(false)
        counterUseCase = mockk(relaxUnitFun = true) {
            every { liveCounter } returns this@SampleViewModelTest.liveCounter
            every { isLoading } returns this@SampleViewModelTest.isLoading
        }

        sampleViewModel = SampleViewModel(context, counterUseCase)
    }

    @Test
    fun testLiveData() {
        isLoading.value = true

        val counterTextObserver = mockk<Observer<String>>(relaxUnitFun = true).also {
            sampleViewModel.counterText.observeForever(it)
        }
        val loadingVisibilityObserver = mockk<Observer<Int>>(relaxUnitFun = true).also {
            sampleViewModel.loadingVisibility.observeForever(it)
        }
        val isOperationEnabledObserver = mockk<Observer<Boolean>>(relaxUnitFun = true).also {
            sampleViewModel.isOperationEnabled.observeForever(it)
        }

        verify(exactly = 1) { counterTextObserver.onChanged("No value.") }
        verify(exactly = 1) { loadingVisibilityObserver.onChanged(View.VISIBLE) }
        verify(exactly = 1) { isOperationEnabledObserver.onChanged(false) }
        confirmVerified(counterTextObserver, loadingVisibilityObserver, isOperationEnabledObserver)

        liveCounter.value = Counter("foo", 42)
        isLoading.value = false

        verify(exactly = 1) { counterTextObserver.onChanged("42") }
        verify(exactly = 1) { loadingVisibilityObserver.onChanged(View.GONE) }
        verify(exactly = 1) { isOperationEnabledObserver.onChanged(true) }
        confirmVerified(counterTextObserver, loadingVisibilityObserver, isOperationEnabledObserver)
    }

    @Test
    fun loadData() {
        sampleViewModel.loadData()

        coVerify { counterUseCase.loadCounter() }
    }

    @Test
    fun countUp() {
        sampleViewModel.countUp()

        coVerify { counterUseCase.changeCounterValue(1) }
    }

    @Test
    fun countDown() {
        sampleViewModel.countDown()

        coVerify { counterUseCase.changeCounterValue(-1) }
    }

    @Test
    fun deleteCounter() {
        sampleViewModel.deleteCounter()

        coVerify { counterUseCase.deleteCounter() }
    }
}

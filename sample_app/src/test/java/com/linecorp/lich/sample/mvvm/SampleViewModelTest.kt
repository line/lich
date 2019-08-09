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
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.only
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyBlocking
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
        counterUseCase = mock {
            on { liveCounter } doReturn liveCounter
            on { isLoading } doReturn isLoading
        }

        sampleViewModel = SampleViewModel(context, counterUseCase)
    }

    @Test
    fun testLiveData() {
        isLoading.value = true

        val counterTextObserver = mock<Observer<String>>().also {
            sampleViewModel.counterText.observeForever(it)
        }
        val loadingVisibilityObserver = mock<Observer<Int>>().also {
            sampleViewModel.loadingVisibility.observeForever(it)
        }
        val isOperationEnabledObserver = mock<Observer<Boolean>>().also {
            sampleViewModel.isOperationEnabled.observeForever(it)
        }

        verify(counterTextObserver, only()).onChanged("No value.")
        verify(loadingVisibilityObserver, only()).onChanged(View.VISIBLE)
        verify(isOperationEnabledObserver, only()).onChanged(false)

        reset(counterTextObserver, loadingVisibilityObserver, isOperationEnabledObserver)
        liveCounter.value = Counter("foo", 42)
        isLoading.value = false

        verify(counterTextObserver, only()).onChanged("42")
        verify(loadingVisibilityObserver, only()).onChanged(View.GONE)
        verify(isOperationEnabledObserver, only()).onChanged(true)
    }

    @Test
    fun loadData() {
        sampleViewModel.loadData()

        verifyBlocking(counterUseCase) { loadCounter() }
    }

    @Test
    fun countUp() {
        sampleViewModel.countUp()

        verifyBlocking(counterUseCase) { changeCounterValue(1) }
    }

    @Test
    fun countDown() {
        sampleViewModel.countDown()

        verifyBlocking(counterUseCase) { changeCounterValue(-1) }
    }

    @Test
    fun deleteCounter() {
        sampleViewModel.deleteCounter()

        verifyBlocking(counterUseCase) { deleteCounter() }
    }
}

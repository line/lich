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
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.linecorp.lich.sample.R
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

@MainThread
class SampleViewModel @VisibleForTesting internal constructor(
    private val context: Context,
    private val counterUseCase: CounterUseCase = CounterUseCase(context, "mvvm")
) : AbstractViewModel() {

    val counterText: LiveData<String> = counterUseCase.liveCounter.map { counter ->
        counter?.value?.toString() ?: context.getString(R.string.counter_no_value)
    }

    val loadingVisibility: LiveData<Int> = counterUseCase.isLoading.map { isLoading ->
        if (isLoading) View.VISIBLE else View.GONE
    }

    val isOperationEnabled: LiveData<Boolean> = counterUseCase.liveCounter.map { counter ->
        counter != null
    }

    fun loadData() {
        launch {
            counterUseCase.loadCounter()
        }
    }

    fun countUp() {
        launch {
            counterUseCase.changeCounterValue(1)
        }
    }

    fun countDown() {
        launch {
            counterUseCase.changeCounterValue(-1)
        }
    }

    fun deleteCounter() {
        launch {
            counterUseCase.deleteCounter()
        }
    }

    companion object : ViewModelFactory<SampleViewModel>() {
        override fun createViewModel(context: Context): SampleViewModel =
            SampleViewModel(context).also {
                it.loadData()
            }
    }
}

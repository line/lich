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
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import com.linecorp.lich.component.component
import com.linecorp.lich.sample.entity.Counter
import com.linecorp.lich.sample.repository.CounterRepository
import com.linecorp.lich.sample.repository.CounterResult

@MainThread
class CounterUseCase(context: Context, private val counterName: String) {

    private val counterRepository by context.component(CounterRepository)

    val liveCounter: MutableLiveData<Counter?> = MutableLiveData(null)

    val isLoading: MutableLiveData<Boolean> = MutableLiveData(false)

    suspend fun loadCounter() {
        isLoading.value = true

        val result = counterRepository.getCounter(counterName)
        when (result) {
            is CounterResult.Success ->
                liveCounter.value = result.counter
            CounterResult.NetworkError -> {
                liveCounter.value = null
                // TODO: Show error message.
            }
        }

        isLoading.value = false
    }

    suspend fun changeCounterValue(delta: Int) {
        liveCounter.value?.let { counter ->
            val newCounter = counter.copy(value = counter.value + delta)
            liveCounter.value = newCounter
            counterRepository.storeCounter(newCounter)
        }
    }

    suspend fun deleteCounter() {
        liveCounter.value?.let { counter ->
            liveCounter.value = null
            counterRepository.deleteCounter(counter)
        }
    }
}

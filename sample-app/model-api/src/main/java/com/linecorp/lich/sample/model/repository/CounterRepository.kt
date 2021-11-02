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

import android.content.Context
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.sample.model.entity.Counter

interface CounterRepository {
    /**
     * Returns the [Counter] for the given name.
     *
     * If the Counter is in the local storage, just returns it. Otherwise, fetches the initial value
     * of the Counter from the server, then stores it into the local storage.
     */
    suspend fun getCounter(counterName: String): CounterResult

    /**
     * Stores the given counter into the local storage.
     */
    suspend fun storeCounter(counter: Counter)

    /**
     * Deletes the given counter from the local storage.
     */
    suspend fun deleteCounter(counter: Counter)

    companion object : ComponentFactory<CounterRepository>() {
        override fun createComponent(context: Context): CounterRepository =
            // Creates the actual instance of this component using `ServiceLoader`.
            delegateToServiceLoader(context)
    }
}

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
package com.linecorp.lich.sample.lifecyclescope

import android.content.Context
import android.util.Log
import com.linecorp.lich.component.ComponentFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS

/**
 * A repository that provides a [Flow] backed by a [Channel].
 */
class ChannelFlowRepository private constructor() {

    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    /**
     * Returns a cold [Flow] that emits a string value every second.
     * The string value consists of [counterName] and a counter.
     */
    fun counterFlow(counterName: String): Flow<String> = callbackFlow {
        log("Start flow: $counterName")

        var counter = 0
        val callback = Runnable {
            val value = "$counterName(${counter++})"
            log("Emitting a value: $value")
            trySend(value).onFailure { e ->
                log("Failed to emit: exception=$e")
            }
        }
        val future = scheduledExecutorService.scheduleWithFixedDelay(callback, 0, 1, SECONDS)

        awaitClose {
            future.cancel(false)
            log("Finish flow: $counterName")
        }
    }

    private fun log(message: String) {
        Log.i("ChannelFlowRepository", message)
    }

    companion object : ComponentFactory<ChannelFlowRepository>() {
        override fun createComponent(context: Context): ChannelFlowRepository =
            ChannelFlowRepository()
    }
}

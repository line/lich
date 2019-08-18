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
package com.linecorp.lich.lifecycle

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/**
 * Lifecycle-aware [CoroutineScope].
 *
 * Like [kotlinx.coroutines.MainScope], this scope has [SupervisorJob] and [Dispatchers.Main]
 * context elements. In addition, this scope has "auto-reset" and "auto-cancel" features.
 *
 * When a AutoResetLifecycleScope has been "reset", all jobs launched from the scope will be cancelled.
 * But, after the reset, the coroutineContext of the scope will be recreated immediately. The timing
 * of "auto-reset" can be specified with the `resetPolicy` parameter.
 *
 * And, this scope will be automatically cancelled when [Lifecycle.Event.ON_DESTROY] event occurred.
 * After `ON_DESTROY`, the scope is no longer active.
 *
 * Example of use:
 * ```
 * class FooActivity : AppCompatActivity() {
 *
 *     // This coroutineScope is automatically reset ON_STOP, and cancelled ON_DESTROY.
 *     private val coroutineScope: CoroutineScope = AutoResetLifecycleScope(this)
 *
 *     fun loadDataThenDraw() {
 *         // The launched job will be automatically cancelled ON_STOP and ON_DESTROY.
 *         coroutineScope.launch {
 *             try {
 *                 val fooData = fooServiceClient.fetchFooData()
 *                 drawData(fooData)
 *             } catch (e: IOException) {
 *                 Toast.makeText(this, "Failed to fetch data.", Toast.LENGTH_SHORT).show()
 *             }
 *         }
 *     }
 *
 *     @MainThread
 *     private fun drawData(fooData: FooData) {
 *         // snip...
 *     }
 * }
 * ```
 */
class AutoResetLifecycleScope @JvmOverloads @MainThread constructor(
    lifecycle: Lifecycle,
    resetPolicy: ResetPolicy = ResetPolicy.ON_STOP
) : CoroutineScope {

    @JvmOverloads
    @MainThread
    constructor(
        lifecycleOwner: LifecycleOwner,
        resetPolicy: ResetPolicy = ResetPolicy.ON_STOP
    ) : this(lifecycleOwner.lifecycle, resetPolicy)

    /**
     * The policy when this [AutoResetLifecycleScope] will be reset.
     *
     * Note that the scope will be always cancelled when [Lifecycle.Event.ON_DESTROY] event occurred,
     * regardless of [ResetPolicy].
     */
    enum class ResetPolicy {
        /**
         * The scope will be reset when [Lifecycle.Event.ON_PAUSE] event occurred.
         */
        ON_PAUSE,
        /**
         * The scope will be reset when [Lifecycle.Event.ON_STOP] event occurred.
         */
        ON_STOP,
        /**
         * Auto-reset is not performed.
         * But, the scope is still cancelled when [Lifecycle.Event.ON_DESTROY] event occurred.
         */
        NONE
    }

    private val currentContextRef: AtomicReference<CoroutineContext> =
        AtomicReference(newCoroutineContext())

    private fun newCoroutineContext(): CoroutineContext =
        SupervisorJob() + Dispatchers.Main

    override val coroutineContext: CoroutineContext
        get() = currentContextRef.get()

    init {
        lifecycle.run {
            if (currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                addObserver(AutoResetObserver(resetPolicy))
            } else {
                cancelContext()
            }
        }
    }

    private fun resetContext() {
        currentContextRef.getAndSet(newCoroutineContext()).cancel()
    }

    private fun cancelContext() {
        coroutineContext.cancel()
    }

    private inner class AutoResetObserver(private val resetPolicy: ResetPolicy) :
        LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_PAUSE && resetPolicy == ResetPolicy.ON_PAUSE ||
                event == Lifecycle.Event.ON_STOP && resetPolicy == ResetPolicy.ON_STOP
            ) {
                resetContext()
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                // Regardless of `resetPolicy`, AutoResetLifecycleScope is always cancelled
                // when Lifecycle.Event.ON_DESTROY event occurred.
                cancelContext()
                source.lifecycle.removeObserver(this)
            }
        }
    }
}

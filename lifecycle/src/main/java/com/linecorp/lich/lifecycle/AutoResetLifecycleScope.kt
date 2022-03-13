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
 * A safer alternative to AndroidX `lifecycleScope`.
 *
 * This scope is bound to [Dispatchers.Main], and will be cancelled when the [Lifecycle] is DESTROYED.
 * In addition, any coroutines launched from this scope are automatically cancelled when the
 * [Lifecycle] is STOPPED. (If [ResetPolicy.ON_STOP] is specified for the `resetPolicy` parameter.)
 *
 * This "auto-reset" feature is useful for collecting `Flow`s safely.
 * It is known that collecting `Flow`s with `lifecycleScope.launchWhenStarted` can lead to resource leaks.
 * (cf. https://link.medium.com/OR5ePKTGthb)
 * This is because coroutines launched by `lifecycleScope.launchWhenStarted` will not be cancelled
 * even if the [Lifecycle] is stopped.
 * On the other hand, coroutines launched from [AutoResetLifecycleScope] are automatically cancelled
 * when the [Lifecycle] is stopped. This avoids resource leaks.
 *
 * The following code is an example of using [AutoResetLifecycleScope] to collect a `Flow` safely.
 *
 * ```
 * class FooFragment : Fragment() {
 *
 *     // Any coroutines launched from this scope are automatically cancelled when FooFragment is STOPPED.
 *     // NOTE: Don't use `viewLifecycleOwner` here.
 *     private val autoResetLifecycleScope: CoroutineScope = AutoResetLifecycleScope(this)
 *
 *     // A repository that provides some data as a Flow.
 *     private val fooRepository = FooRepository()
 *
 *     override fun onStart() {
 *         super.onStart()
 *
 *         // It is SAFE to collect a flow with AutoResetLifecycleScope.
 *         // The coroutine launched here will be automatically cancelled just before `FooFragment.onStop()`.
 *         autoResetLifecycleScope.launch {
 *             fooRepository.dataFlow().collect { value ->
 *                 textView.text = value
 *             }
 *         }
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
     * When an [AutoResetLifecycleScope] is reset, any coroutines that were previously launched from
     * the scope will be automatically cancelled.
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

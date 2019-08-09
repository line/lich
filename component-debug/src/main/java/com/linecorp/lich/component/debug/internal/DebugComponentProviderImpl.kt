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
package com.linecorp.lich.component.debug.internal

import android.content.Context
import android.os.Looper
import android.os.StrictMode
import android.os.SystemClock
import android.util.Log
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.debug.DebugComponentProvider
import com.linecorp.lich.component.provider.internal.ComponentCreator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CountDownLatch

/**
 * The implementation of [DebugComponentProvider].
 * This provider also performs additional error checking such as detecting circular dependency.
 */
internal class DebugComponentProviderImpl(private val creator: ComponentCreator) :
    DebugComponentProvider {

    private val components: ConcurrentMap<ComponentFactory<*>, DebugComponentHolder> =
        ConcurrentHashMap()

    override fun <T : Any> getComponent(context: Context, factory: ComponentFactory<T>): T {
        components[factory]?.let {
            return it.await()
        }
        val newHolder = DebugComponentHolder(factory)
        components.putIfAbsent(factory, newHolder)?.let {
            return it.await()
        }

        val previousHolder = newHolder.setCurrentThreadIsDependingOnThis()
        currentComponentHolder.set(newHolder)
        val result = runCatching {
            val startTime = SystemClock.elapsedRealtime()
            val component = creator.create(context, factory)
            val creationTime = SystemClock.elapsedRealtime() - startTime
            Log.i("DebugComponentProvider", "Created $component in $creationTime ms.")
            component
        }
        newHolder.setResult(result)
        currentComponentHolder.set(previousHolder)
        previousHolder?.resetDependingHolder()

        if (result.isFailure) {
            components.remove(factory)
        }

        return result.getOrThrow()
    }

    override fun <T : Any> setComponent(factory: ComponentFactory<T>, component: T) {
        val newHolder = DebugComponentHolder(factory)
        components[factory] = newHolder
        newHolder.setResult(Result.success(component))
    }

    override fun <T : Any> clearComponent(factory: ComponentFactory<T>) {
        components.remove(factory)
    }

    override fun clearAllComponents() {
        components.clear()
    }

    private class DebugComponentHolder(private val factory: ComponentFactory<*>) {

        private val latch: CountDownLatch = CountDownLatch(1)

        @Volatile
        private var component: Result<Any>? = null

        /**
         * If non-zero, it indicates that the main thread is blocked in the [await] function since
         * this time. The time is `SystemClock.elapsedRealtime()` based.
         */
        @Volatile
        private var mainThreadIsBlockedSince: Long = 0L

        /**
         * [DebugComponentHolder] that the thread creating a component for this holder is now
         * depending on.
         */
        @Volatile
        private var dependingHolder: DebugComponentHolder? = null

        /**
         * Marks [currentComponentHolder] as now depending on this ComponentHolder.
         * Then, checks the dependency graph.
         *
         * @return if not null, the caller must call [resetDependingHolder] for it later.
         */
        fun setCurrentThreadIsDependingOnThis(): DebugComponentHolder? {
            val currentHolder = currentComponentHolder.get() ?: return null
            currentHolder.dependingHolder = this

            val dependencyList: MutableList<DebugComponentHolder> = mutableListOf(this)
            var dependencyCheckingHolder: DebugComponentHolder = this
            while (true) {
                val nextHolder = dependencyCheckingHolder.dependingHolder ?: break
                if (dependencyList.contains(nextHolder)) {
                    // Detected circular dependency!
                    currentHolder.resetDependingHolder()
                    val dependencyGraph =
                        dependencyList.apply { add(nextHolder) }.map { it.factory }.toString()
                    throw IllegalStateException("Detected circular dependency!: $dependencyGraph")
                }
                dependencyList.add(nextHolder)
                dependencyCheckingHolder = nextHolder
            }

            return currentHolder
        }

        fun resetDependingHolder() {
            dependingHolder = null
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> await(): T {
            val c1 = component
            if (c1 != null) {
                return c1.getOrThrow() as T
            }

            val currentHolder = setCurrentThreadIsDependingOnThis()
            try {
                if (Thread.currentThread() === Looper.getMainLooper().thread) {
                    mainThreadIsBlockedSince = SystemClock.elapsedRealtime()
                }
                StrictMode.noteSlowCall("Waiting for component creation.")
                latch.await()
            } finally {
                currentHolder?.resetDependingHolder()
            }

            val c2 = component
            return checkNotNull(c2).getOrThrow() as T
        }

        fun setResult(result: Result<Any>) {
            component = result
            latch.countDown()

            val blockedSince = mainThreadIsBlockedSince
            if (blockedSince != 0L) {
                val blockedTime = SystemClock.elapsedRealtime() - blockedSince
                Log.w(
                    "DebugComponentProvider",
                    "Component creation for $factory blocked the main thread for $blockedTime ms.",
                    RuntimeException()
                )
            }
        }
    }

    companion object {
        /**
         * [DebugComponentHolder] for which the current thread is now creating a component.
         */
        private val currentComponentHolder: ThreadLocal<DebugComponentHolder?> = ThreadLocal()
    }
}

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
import com.linecorp.lich.component.debug.DebugComponentManager
import com.linecorp.lich.component.internal.ComponentAccessor
import com.linecorp.lich.component.internal.ComponentProvider
import java.util.concurrent.CountDownLatch

/**
 * An implementation of [ComponentProvider] that performs additional error checking and log output.
 */
open class DebugComponentProvider : ComponentProvider {

    private lateinit var _defaultAccessor: ComponentAccessor

    protected val defaultAccessor
        get() = _defaultAccessor

    override val loadPriority: Int
        get() = 10

    final override fun init(accessor: ComponentAccessor) {
        _defaultAccessor = accessor
    }

    override fun <T : Any> getComponent(context: Context, factory: ComponentFactory<T>): T =
        getOrCreateComponent(context.applicationContext, defaultAccessor, factory)

    protected fun <T : Any> getOrCreateComponent(
        applicationContext: Context,
        accessor: ComponentAccessor,
        factory: ComponentFactory<T>
    ): T {
        accessor.getComponent(factory)?.let {
            @Suppress("UNCHECKED_CAST")
            return if (it is Creating) it.await() else it as T
        }

        val creating = Creating(factory)
        while (!accessor.compareAndSetComponent(factory, null, creating)) {
            accessor.getComponent(factory)?.let {
                @Suppress("UNCHECKED_CAST")
                return if (it is Creating) it.await() else it as T
            }
        }

        val parentCreating = currentlyCreating.getAndSet(creating)
        val result = runCatching {
            parentCreating?.addDependencyThenCheckGraph(creating)

            val startTime = SystemClock.elapsedRealtime()
            val component = accessor.createComponent(factory, applicationContext)
            val creationTime = SystemClock.elapsedRealtime() - startTime
            Log.i("DebugComponentProvider", "Created $component in $creationTime ms.")
            component
        }
        parentCreating?.removeDependency()
        currentlyCreating.set(parentCreating)

        result.onFailure { e ->
            Log.e("DebugComponentProvider", "Failed to create a component for $factory.", e)
        }
        creating.setResult(result)

        accessor.compareAndSetComponent(factory, creating, result.getOrNull())
        return result.getOrThrow()
    }

    /**
     * A special value indicating a component is now being created.
     * Other threads can wait on this object.
     *
     * This class also supports detecting circular dependency.
     */
    private class Creating(private val factory: ComponentFactory<*>) {

        private var result: Result<Any>? = null

        private val latch: CountDownLatch = CountDownLatch(1)

        /**
         * If non-zero, it indicates that the main thread is blocked in the [await] function since
         * this time. The time is `SystemClock.elapsedRealtime()` based.
         */
        @Volatile
        private var mainThreadIsBlockedSince: Long = 0L

        /**
         * A [Creating] object that this instance is now depending on.
         */
        @Volatile
        private var dependee: Creating? = null

        /**
         * Marks the given [Creating] object as now depended by this instance.
         * Then, checks the dependency graph.
         * If circular dependency is detected, throws IllegalStateException.
         */
        fun addDependencyThenCheckGraph(depending: Creating) {
            dependee = depending

            var nextDependency: Creating = depending.dependee ?: return
            val dependencyList: MutableList<Creating> = mutableListOf(depending)
            while (true) {
                if (nextDependency in dependencyList) {
                    // Detected circular dependency!
                    val dependencyGraph =
                        dependencyList.apply { add(nextDependency) }.map { it.factory }.toString()
                    throw IllegalStateException("Detected circular dependency!: $dependencyGraph")
                }
                dependencyList += nextDependency
                nextDependency = nextDependency.dependee ?: break
            }
        }

        fun removeDependency() {
            dependee = null
        }

        fun <T : Any> await(): T {
            val parentCreating = currentlyCreating.get()
            try {
                parentCreating?.addDependencyThenCheckGraph(this)

                if (Thread.currentThread() === Looper.getMainLooper().thread) {
                    mainThreadIsBlockedSince = SystemClock.elapsedRealtime()
                }
                StrictMode.noteSlowCall("Waiting for component creation.")
                latch.await()
            } finally {
                parentCreating?.removeDependency()
            }

            @Suppress("UNCHECKED_CAST")
            return checkNotNull(result).getOrThrow() as T
        }

        fun setResult(result: Result<Any>) {
            this.result = result
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

    protected fun <T : Any> getComponentIfAlreadyCreated(
        accessor: ComponentAccessor,
        factory: ComponentFactory<T>
    ): T? {
        val component = accessor.getComponent(factory)
        @Suppress("UNCHECKED_CAST")
        return if (component is Creating) null else component as T?
    }

    override fun getManager(applicationContext: Context): Any =
        DebugComponentManagerImpl()

    private inner class DebugComponentManagerImpl : DebugComponentManager {

        override fun <T : Any> setComponent(factory: ComponentFactory<T>, component: T) {
            defaultAccessor.setComponent(factory, component)
        }

        override fun <T : Any> clearComponent(factory: ComponentFactory<T>) {
            defaultAccessor.setComponent(factory, null)
        }

        override fun <T : Any> getComponentIfAlreadyCreated(factory: ComponentFactory<T>): T? =
            getComponentIfAlreadyCreated(defaultAccessor, factory)
    }

    private fun <T> ThreadLocal<T>.getAndSet(newValue: T): T? {
        val prevValue = get()
        set(newValue)
        return prevValue
    }

    private companion object {
        /**
         * A [Creating] object for which the current thread is now creating a component.
         */
        private val currentlyCreating: ThreadLocal<Creating?> = ThreadLocal()
    }
}

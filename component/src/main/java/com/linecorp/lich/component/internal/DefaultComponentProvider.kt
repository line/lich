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
package com.linecorp.lich.component.internal

import android.content.Context
import android.os.StrictMode
import com.linecorp.lich.component.ComponentFactory
import java.util.concurrent.CountDownLatch

/**
 * A simple and fast implementation of [ComponentProvider].
 */
internal class DefaultComponentProvider : ComponentProvider {

    override val loadPriority: Int
        get() = 0

    override fun init(accessor: ComponentAccessor) = Unit

    override fun <T : Any> getComponent(context: Context, factory: ComponentFactory<T>): T {
        val accessor = ComponentFactory.Accessor
        accessor.getComponent(factory)?.let {
            @Suppress("UNCHECKED_CAST")
            return if (it is Creating) it.await() else it as T
        }

        val creating = Creating()
        while (!accessor.compareAndSetComponent(factory, null, creating)) {
            accessor.getComponent(factory)?.let {
                @Suppress("UNCHECKED_CAST")
                return if (it is Creating) it.await() else it as T
            }
        }

        val result = runCatching { accessor.createComponent(factory, context.applicationContext) }
        creating.setResult(result)

        accessor.setComponent(factory, result.getOrNull())
        return result.getOrThrow()
    }

    /**
     * A special value indicating a component is now being created.
     * Other threads can wait on this object.
     */
    private class Creating {

        private var result: Result<Any>? = null

        private val latch: CountDownLatch = CountDownLatch(1)

        fun <T : Any> await(): T {
            StrictMode.noteSlowCall("Waiting for component creation.")
            latch.await()

            @Suppress("UNCHECKED_CAST")
            return checkNotNull(result).getOrThrow() as T
        }

        fun setResult(result: Result<Any>) {
            this.result = result
            latch.countDown()
        }
    }

    override fun getManager(applicationContext: Context): Any =
        throw UnsupportedOperationException()
}

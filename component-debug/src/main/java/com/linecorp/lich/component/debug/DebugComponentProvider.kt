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
@file:JvmName("DebugComponentProviders")

package com.linecorp.lich.component.debug

import android.content.Context
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.provider.ComponentProvider
import com.linecorp.lich.component.provider.ComponentProviderOwner

/**
 * A [ComponentProvider] that supports several methods useful for debugging.
 *
 * Note that the modification to this provider don't affect already acquired components.
 */
interface DebugComponentProvider : ComponentProvider {
    /**
     * Sets the given instance as a component for the [factory].
     *
     * If already there is another component for the [factory] in this provider,
     * it will be replaced.
     */
    fun <T : Any> setComponent(factory: ComponentFactory<T>, component: T)

    /**
     * Removes the component for the [factory] from this provider, if any.
     */
    fun <T : Any> clearComponent(factory: ComponentFactory<T>)

    /**
     * Clears all components in this provider.
     */
    fun clearAllComponents()
}

/**
 * Obtains the [DebugComponentProvider] from a [Context].
 *
 * For example, you can use [DebugComponentProvider] to replace components.
 * ```
 * interface FooComponent {
 *
 *     fun doSomething()
 *
 *     companion object : ComponentFactory<FooComponent>() {
 *         override fun createComponent(context: Context): FooComponent =
 *             TODO("Create FooComponentImpl.")
 *     }
 * }
 * ```
 *
 * ```
 * fun initFooComponentForDebug(context: Context) {
 *     val baseFoo = context.getComponent(FooComponent)
 *     if (baseFoo is FooComponentForDebug) {
 *         throw IllegalStateException("FooComponentForDebug is already set.")
 *     }
 *     context.debugComponentProvider.setComponent(FooComponent, FooComponentForDebug(baseFoo))
 * }
 *
 * private class FooComponentForDebug(private val baseFoo: FooComponent) : FooComponent {
 *
 *     override fun doSomething() {
 *         showDebugInformation()
 *         baseFoo.doSomething()
 *     }
 *
 *     private fun showDebugInformation() {
 *         // ...
 *     }
 * }
 * ```
 *
 * Note that the modification to [DebugComponentProvider] don't affect already acquired components.
 * So, the above `initFooComponentForDebug(context)` should be called prior to any acquisition of
 * `FooComponent`.
 */
@get:JvmName("from")
val Context.debugComponentProvider: DebugComponentProvider
    get() {
        val providerOwner = applicationContext as? ComponentProviderOwner
            ?: throw RuntimeException(
                "The applicationContext isn't implementing ComponentProviderOwner. " +
                    "Please refer to the document of ComponentProviderOwner."
            )
        return providerOwner.componentProvider as? DebugComponentProvider
            ?: throw RuntimeException(
                "The componentProvider isn't DebugComponentProvider. " +
                    "Please ensure that the componentProvider is properly initialized."
            )
    }

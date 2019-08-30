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
@file:JvmName("DebugComponentManagers")

package com.linecorp.lich.component.debug

import android.content.Context
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.internal.getComponentManager

/**
 * An API to modify components directly.
 *
 * The modification to this manager will affect subsequent calls of `context.getComponent(factory)`.
 */
interface DebugComponentManager {
    /**
     * Sets the given instance as a component for the [factory].
     *
     * If already there is another component for the [factory], it will be replaced.
     */
    fun <T : Any> setComponent(factory: ComponentFactory<T>, component: T)

    /**
     * Removes a component for the [factory], if any.
     *
     * It means the next call of `context.getComponent(factory)` will create a new instance of the
     * component.
     */
    fun <T : Any> clearComponent(factory: ComponentFactory<T>)

    /**
     * Returns a component instance for the [factory] only if it is already created.
     * Otherwise, returns null.
     */
    fun <T : Any> getComponentIfAlreadyCreated(factory: ComponentFactory<T>): T?
}

/**
 * Obtains a [DebugComponentManager] from a [Context].
 *
 * For example, you can use [DebugComponentManager] to replace components.
 * ```
 * interface FooComponent {
 *
 *     fun doSomething()
 *
 *     companion object : ComponentFactory<FooComponent>() {
 *         override fun createComponent(context: Context): FooComponent =
 *             // snip...
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
 *     context.debugComponentManager.setComponent(FooComponent, FooComponentForDebug(baseFoo))
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
 * Note that modifications to [DebugComponentManager] don't affect already acquired components.
 * So, the above `initFooComponentForDebug(context)` should be called prior to any acquisition of
 * `FooComponent`.
 */
@get:JvmName("from")
val Context.debugComponentManager: DebugComponentManager
    get() = getComponentManager(applicationContext) as DebugComponentManager

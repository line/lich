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
@file:JvmName("MockComponentManagers")

package com.linecorp.lich.component.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.debug.DebugComponentManager
import com.linecorp.lich.component.internal.getComponentManager

/**
 * An API to mock components for tests.
 *
 * If the `component-test` module is in the runtime classpath, every component is tied to an
 * `applicationContext`. That is, a different instance is created for each `applicationContext`.
 */
interface MockComponentManager : DebugComponentManager {
    /**
     * Sets [mock] as a mock component for [factory].
     *
     * After calling this method, `context.getComponent(factory)` will return the [mock] for [factory].
     */
    fun <T : Any> setMockComponent(factory: ComponentFactory<T>, mock: T)

    /**
     * Clears the mock component that was previously set via [setMockComponent].
     *
     * After calling this method, `context.getComponent(factory)` will return the real component for
     * [factory].
     */
    fun <T : Any> clearMockComponent(factory: ComponentFactory<T>)

    /**
     * Gets a component for the given [factory] ignoring mocks.
     *
     * This function returns a "real" instance of the component regardless of [setMockComponent].
     * For example, you can use this function for "spying" a real component.
     */
    fun <T : Any> getRealComponent(factory: ComponentFactory<T>): T

    /**
     * Clears all real/mock components.
     */
    fun clearAllComponents()
}

/**
 * Obtains a [MockComponentManager] for the given [applicationContext].
 *
 * By default, the applicationContext is obtained from `ApplicationProvider.getApplicationContext()`.
 * It is initialized by AndroidX Test framework.
 */
@JvmName("of")
fun getMockComponentManager(
    applicationContext: Context = ApplicationProvider.getApplicationContext()
): MockComponentManager = getComponentManager(applicationContext) as MockComponentManager

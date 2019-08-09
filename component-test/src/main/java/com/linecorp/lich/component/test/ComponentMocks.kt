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
@file:JvmName("ComponentMocks")

package com.linecorp.lich.component.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.test.internal.mockComponentProvider

/**
 * Sets [mock] as a mock component for [factory].
 * After calling this method, `Context.getComponent(factory)` will return the [mock] for [factory].
 */
fun <T : Any> setMockComponent(factory: ComponentFactory<T>, mock: T) {
    mockComponentProvider.setMockComponent(factory, mock)
}

/**
 * Clears the mock component that was previously set via [setMockComponent].
 * After calling this method, `Context.getComponent(factory)` will return the real component for
 * [factory].
 */
fun <T : Any> clearMockComponent(factory: ComponentFactory<T>) {
    mockComponentProvider.clearMockComponent(factory)
}

/**
 * Gets a singleton instance of component for the given [factory].
 * This function returns a "real" instance of the component regardless of [setMockComponent].
 *
 * For example, you can use this function for "spying" a real component.
 */
fun <T : Any> getRealComponent(factory: ComponentFactory<T>): T {
    val context: Context = ApplicationProvider.getApplicationContext()
    return mockComponentProvider.getRealComponent(context, factory)
}

/**
 * Clears all real/mock components.
 *
 * In Android instrumentation tests, `applicationContext` is shared between test methods.
 * So, you should clear all components after each test like this:
 * ```
 * @After
 * fun tearDown() {
 *     clearAllComponents()
 * }
 * ```
 *
 * You don't need to call this function in Robolectric tests, because Robolectric recreates
 * `applicationContext` for each test.
 */
fun clearAllComponents() {
    mockComponentProvider.clearAllComponents()
}

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
package com.linecorp.lich.component.test.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.debug.DebugComponentProvider
import com.linecorp.lich.component.provider.ComponentProviderOwner
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * A [DebugComponentProvider] that supports mocking components.
 *
 * The mock components are held separately from real objects and are returned preferentially.
 */
internal class MockComponentProvider(private val baseComponentProvider: DebugComponentProvider) :
    DebugComponentProvider {

    private val mocks: ConcurrentMap<ComponentFactory<*>, Any> = ConcurrentHashMap()

    /**
     * Sets [mock] as a mock component for [factory].
     * After calling this method, [getComponent] will return the [mock] for [factory].
     */
    fun <T : Any> setMockComponent(factory: ComponentFactory<T>, mock: T) {
        mocks[factory] = mock
    }

    /**
     * Clears the mock component that was previously set via [setMockComponent].
     * After calling this method, [getComponent] will return the real component for [factory].
     */
    fun <T : Any> clearMockComponent(factory: ComponentFactory<T>) {
        mocks.remove(factory)
    }

    /**
     * Gets a singleton instance of component for the given [factory].
     * This function returns a "real" instance of the component regardless of [setMockComponent].
     */
    fun <T : Any> getRealComponent(context: Context, factory: ComponentFactory<T>): T =
        baseComponentProvider.getComponent(context, factory)

    override fun <T : Any> getComponent(context: Context, factory: ComponentFactory<T>): T {
        // If there is a mock for the factory, returns it.
        mocks[factory]?.let {
            @Suppress("UNCHECKED_CAST")
            return it as T
        }
        // Otherwise, falls back to baseComponentProvider.
        return baseComponentProvider.getComponent(context, factory)
    }

    override fun <T : Any> setComponent(factory: ComponentFactory<T>, component: T) {
        baseComponentProvider.setComponent(factory, component)
    }

    override fun <T : Any> clearComponent(factory: ComponentFactory<T>) {
        baseComponentProvider.clearComponent(factory)
    }

    override fun clearAllComponents() {
        mocks.clear()
        baseComponentProvider.clearAllComponents()
    }
}

/**
 * An instance of [MockComponentProvider] obtained from [ApplicationProvider.getApplicationContext].
 */
internal val mockComponentProvider: MockComponentProvider
    get() {
        val applicationContext: Context = ApplicationProvider.getApplicationContext()
        val providerOwner = applicationContext as? ComponentProviderOwner
            ?: throw RuntimeException(
                "The applicationContext isn't implementing ComponentProviderOwner. " +
                    "Please refer to the document of ComponentProviderOwner."
            )
        return providerOwner.componentProvider as? MockComponentProvider
            ?: throw RuntimeException(
                "The componentProvider isn't MockComponentProvider. " +
                    "Please ensure that the componentProvider is properly initialized."
            )
    }

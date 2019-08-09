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
package com.linecorp.lich.viewmodel.test.internal

import android.content.Context
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import com.linecorp.lich.viewmodel.provider.BridgeViewModelProviderOwner
import com.linecorp.lich.viewmodel.provider.internal.DefaultBridgeViewModelProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private typealias MockFactory = (ViewModelStoreOwner) -> AbstractViewModel

/**
 * A `BridgeViewModelProvider` that supports mocking ViewModels.
 */
internal class MockBridgeViewModelProvider : DefaultBridgeViewModelProvider() {

    private val mockFactories: ConcurrentMap<ViewModelFactory<*>, MockFactory> = ConcurrentHashMap()

    /**
     * Sets [mockFactory] as a factory function of mock ViewModels for [factory].
     *
     * When creating a new ViewModel instance, [mockFactory] will be called with the
     * [ViewModelStoreOwner] (e.g. Activity, Fragment).
     *
     * @param factory a [ViewModelFactory] to be mocked.
     * @param mockFactory a function that returns a mock ViewModel for the given [ViewModelStoreOwner].
     */
    fun <T : AbstractViewModel> setMockViewModel(
        factory: ViewModelFactory<T>,
        mockFactory: (ViewModelStoreOwner) -> T
    ) {
        mockFactories[factory] = mockFactory
    }

    /**
     * Clears the `mockFactory` that was previously set via [setMockViewModel].
     *
     * @param factory a [ViewModelFactory] that will be no longer mocked.
     */
    fun <T : AbstractViewModel> clearMockViewModel(factory: ViewModelFactory<T>) {
        mockFactories.remove(factory)
    }

    /**
     * Creates a new ViewModel for the given [factory].
     * This function returns a "real" instance of the ViewModel regardless of [setMockViewModel].
     */
    fun <T : AbstractViewModel> createRealViewModel(
        context: Context,
        factory: ViewModelFactory<T>
    ): T = createViewModel(context, factory)

    /**
     * Clears all mocks that were previously set via [setMockViewModel].
     * Note that this function doesn't clear ViewModels that are already in `ViewModelStore`.
     */
    fun clearAllMockViewModels() {
        mockFactories.clear()
    }

    override fun <T : AbstractViewModel> newViewModelFor(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner,
        viewModelFactory: ViewModelFactory<T>
    ): T {
        // If there is a mockFactory, creates a mock and returns it.
        mockFactories[viewModelFactory]?.let { mockFactory ->
            @Suppress("UNCHECKED_CAST")
            return mockFactory(viewModelStoreOwner) as T
        }
        // Otherwise, returns a real ViewModel.
        return createViewModel(context, viewModelFactory)
    }
}

/**
 * An instance of [MockBridgeViewModelProvider] obtained from
 * [ApplicationProvider.getApplicationContext].
 */
internal val mockBridgeViewModelProvider: MockBridgeViewModelProvider
    get() {
        val applicationContext: Context = ApplicationProvider.getApplicationContext()
        val providerOwner = applicationContext as? BridgeViewModelProviderOwner
            ?: throw RuntimeException(
                "The applicationContext isn't implementing BridgeViewModelProviderOwner. " +
                    "Please refer to the document of BridgeViewModelProviderOwner."
            )
        return providerOwner.bridgeViewModelProvider as? MockBridgeViewModelProvider
            ?: throw RuntimeException(
                "The bridgeViewModelProvider isn't MockBridgeViewModelProvider. " +
                    "Please ensure that the bridgeViewModelProvider is properly initialized."
            )
    }

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
@file:JvmName("ViewModelMocks")

package com.linecorp.lich.viewmodel.test

import android.content.Context
import androidx.lifecycle.ViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import com.linecorp.lich.viewmodel.test.internal.mockBridgeViewModelProvider

/**
 * Sets [mockFactory] as a factory function of mock ViewModels for [factory].
 *
 * When creating a new ViewModel instance, [mockFactory] will be called with the [ViewModelStoreOwner]
 * (e.g. Activity, Fragment). Then, the instance returned by [mockFactory] is registered to the
 * [ViewModelStoreOwner].
 *
 * @param factory a [ViewModelFactory] to be mocked.
 * @param mockFactory a function that returns a mock ViewModel for the given [ViewModelStoreOwner].
 * @see MockViewModelHandle
 */
fun <T : AbstractViewModel> setMockViewModel(
    factory: ViewModelFactory<T>,
    mockFactory: (ViewModelStoreOwner) -> T
) {
    mockBridgeViewModelProvider.setMockViewModel(factory, mockFactory)
}

/**
 * Clears the `mockFactory` that was previously set via [setMockViewModel].
 *
 * @param factory a [ViewModelFactory] that will be no longer mocked.
 */
fun <T : AbstractViewModel> clearMockViewModel(factory: ViewModelFactory<T>) {
    mockBridgeViewModelProvider.clearMockViewModel(factory)
}

/**
 * Creates a new ViewModel for the given [factory].
 * This function returns a "real" instance of the ViewModel regardless of [setMockViewModel].
 *
 * For example, a `mockFactory` of [setMockViewModel] may use this function for "spying" a real
 * ViewModel.
 */
fun <T : AbstractViewModel> createRealViewModel(factory: ViewModelFactory<T>): T {
    val context: Context = ApplicationProvider.getApplicationContext()
    return mockBridgeViewModelProvider.createRealViewModel(context, factory)
}

/**
 * Clears all mocks that were previously set via [setMockViewModel].
 * Note that this function doesn't clear ViewModels that are already in `ViewModelStore`.
 *
 * In Android instrumentation tests, `applicationContext` is shared between test methods.
 * So, you should clear all mock ViewModel factories after each test like this:
 * ```
 * @After
 * fun tearDown() {
 *     clearAllMockViewModels()
 * }
 * ```
 *
 * You don't need to call this function in Robolectric tests, because Robolectric recreates
 * `applicationContext` for each test.
 */
fun clearAllMockViewModels() {
    mockBridgeViewModelProvider.clearAllMockViewModels()
}

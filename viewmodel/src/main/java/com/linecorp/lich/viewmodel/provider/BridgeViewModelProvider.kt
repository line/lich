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
package com.linecorp.lich.viewmodel.provider

import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModelStoreOwner
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import com.linecorp.lich.viewmodel.provider.internal.bridgeViewModelProviderFactory

/**
 * A class that provides ViewModels for [ViewModelFactory].
 *
 * @see BridgeViewModelProviderOwner
 */
interface BridgeViewModelProvider {
    /**
     * Returns an existing ViewModel or creates a new one, associated with the given
     * [viewModelStoreOwner].
     *
     * This method is intended for internal use of the ViewModel framework.
     * Instead, you should use such as `Fragment.viewModel(factory)`.
     */
    @MainThread
    fun <T : AbstractViewModel> getViewModel(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner,
        viewModelFactory: ViewModelFactory<T>
    ): T
}

/**
 * Creates a new `BridgeViewModelProvider`.
 */
@Suppress("FunctionName") // factory function
fun BridgeViewModelProvider(): BridgeViewModelProvider =
    bridgeViewModelProviderFactory.newBridgeViewModelProvider()

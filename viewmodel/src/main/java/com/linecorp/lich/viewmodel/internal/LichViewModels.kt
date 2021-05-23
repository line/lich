/*
 * Copyright 2021 LINE Corporation
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
package com.linecorp.lich.viewmodel.internal

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory

/**
 * Internal endpoint of Lich ViewModels.
 */
object LichViewModels {

    fun <T : AbstractViewModel> getViewModelWithDefaultArguments(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner,
        hasDefaultViewModelProviderFactory: HasDefaultViewModelProviderFactory,
        factory: ViewModelFactory<T>
    ): T = lichViewModelProvider.getViewModel(
        context,
        viewModelStoreOwner,
        hasDefaultViewModelProviderFactory.defaultViewModelProviderFactory,
        factory
    )

    fun <T : AbstractViewModel> getViewModelWithExplicitArguments(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner,
        savedStateRegistryOwner: SavedStateRegistryOwner,
        factory: ViewModelFactory<T>,
        arguments: Bundle?
    ): T = lichViewModelProvider.getViewModel(
        context,
        viewModelStoreOwner,
        BridgeViewModelFactory(savedStateRegistryOwner, arguments),
        factory
    )

    private class BridgeViewModelFactory(
        savedStateRegistryOwner: SavedStateRegistryOwner,
        arguments: Bundle?
    ) : AbstractSavedStateViewModelFactory(savedStateRegistryOwner, arguments) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T = BridgeViewModel(handle) as T
    }
}

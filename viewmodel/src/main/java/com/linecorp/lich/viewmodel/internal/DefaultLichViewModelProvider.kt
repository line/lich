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
package com.linecorp.lich.viewmodel.internal

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.SavedState
import com.linecorp.lich.viewmodel.ViewModelFactory

/**
 * The default implementation of [LichViewModelProvider].
 */
open class DefaultLichViewModelProvider : LichViewModelProvider {

    override val loadPriority: Int
        get() = 0

    final override fun <T : AbstractViewModel> getViewModel(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner,
        savedStateRegistryOwner: SavedStateRegistryOwner,
        factory: ViewModelFactory<T>,
        arguments: Bundle?
    ): T {
        // The key to use to identify the BridgeViewModel in a ViewModelStore.
        val key = factory.javaClass.name

        // We always create a BridgeViewModel object for ViewModelStores of Android Architecture Components.
        val bridgeViewModelFactory = BridgeViewModelFactory(savedStateRegistryOwner, arguments)
        val viewModelProvider = ViewModelProvider(viewModelStoreOwner, bridgeViewModelFactory)
        val bridgeViewModel = viewModelProvider.get(key, BridgeViewModel::class.java)

        // Then, populate our ViewModel object to bridgeViewModel.viewModel.
        bridgeViewModel.viewModel?.let { viewModel ->
            @Suppress("UNCHECKED_CAST")
            return viewModel as T
        }
        return newViewModelFor(
            factory,
            context.applicationContext,
            bridgeViewModel.savedState,
            viewModelStoreOwner
        ).also { viewModel ->
            bridgeViewModel.viewModel = viewModel
        }
    }

    // TestLichViewModelProvider will override this function.
    protected open fun <T : AbstractViewModel> newViewModelFor(
        factory: ViewModelFactory<T>,
        applicationContext: Context,
        savedState: SavedState,
        viewModelStoreOwner: ViewModelStoreOwner
    ): T = createViewModel(factory, applicationContext, savedState)

    protected fun <T : AbstractViewModel> createViewModel(
        factory: ViewModelFactory<T>,
        applicationContext: Context,
        savedState: SavedState
    ): T = factory.create(applicationContext, savedState)

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

    override fun getManager(applicationContext: Context): Any =
        throw UnsupportedOperationException()
}

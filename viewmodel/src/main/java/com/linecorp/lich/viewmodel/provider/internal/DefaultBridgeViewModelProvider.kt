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
package com.linecorp.lich.viewmodel.provider.internal

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import com.linecorp.lich.viewmodel.provider.BridgeViewModelProvider

/**
 * The default implementation of [BridgeViewModelProvider].
 */
open class DefaultBridgeViewModelProvider : BridgeViewModelProvider {

    private val bridgeViewModelFactory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BridgeViewModel() as T
        }

    final override fun <T : AbstractViewModel> getViewModel(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner,
        viewModelFactory: ViewModelFactory<T>
    ): T {
        // The key to use to identify the BridgeViewModel in a ViewModelStore.
        val key = viewModelFactory.javaClass.name

        // We always create a BridgeViewModel object for ViewModelStores of Android Architecture Components.
        val viewModelProvider = ViewModelProvider(viewModelStoreOwner, bridgeViewModelFactory)
        val bridgeViewModel = viewModelProvider.get(key, BridgeViewModel::class.java)

        // Then, populate our ViewModel object to bridgeViewModel.viewModel.
        bridgeViewModel.viewModel?.let { viewModel ->
            @Suppress("UNCHECKED_CAST")
            return viewModel as T
        }
        return newViewModelFor(context, viewModelStoreOwner, viewModelFactory).also { viewModel ->
            bridgeViewModel.viewModel = viewModel
        }
    }

    // MockBridgeViewModelProvider will override this function.
    protected open fun <T : AbstractViewModel> newViewModelFor(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner,
        viewModelFactory: ViewModelFactory<T>
    ): T = createViewModel(context, viewModelFactory)

    protected fun <T : AbstractViewModel> createViewModel(
        context: Context,
        factory: ViewModelFactory<T>
    ): T = factory.create(context.applicationContext)

    companion object : BridgeViewModelProviderFactory {
        override fun newBridgeViewModelProvider(): BridgeViewModelProvider =
            DefaultBridgeViewModelProvider()
    }
}

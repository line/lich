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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory

/**
 * The default implementation of [LichViewModelProvider].
 */
open class DefaultLichViewModelProvider : LichViewModelProvider {

    override val loadPriority: Int
        get() = 0

    private val bridgeViewModelFactory: ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BridgeViewModel() as T
        }

    final override fun <T : AbstractViewModel> getViewModel(
        context: Context,
        viewModelStoreOwner: ViewModelStoreOwner,
        factory: ViewModelFactory<T>
    ): T {
        // The key to use to identify the BridgeViewModel in a ViewModelStore.
        val key = factory.javaClass.name

        // We always create a BridgeViewModel object for ViewModelStores of Android Architecture Components.
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
            viewModelStoreOwner
        ).also { viewModel ->
            bridgeViewModel.viewModel = viewModel
        }
    }

    // TestLichViewModelProvider will override this function.
    protected open fun <T : AbstractViewModel> newViewModelFor(
        factory: ViewModelFactory<T>,
        applicationContext: Context,
        viewModelStoreOwner: ViewModelStoreOwner
    ): T = createViewModel(factory, applicationContext)

    protected fun <T : AbstractViewModel> createViewModel(
        factory: ViewModelFactory<T>,
        applicationContext: Context
    ): T = factory.create(applicationContext)

    override fun getManager(applicationContext: Context): Any =
        throw UnsupportedOperationException()
}

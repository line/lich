/*
 * Copyright 2022 LINE Corporation
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
package com.linecorp.lich.viewmodel.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import com.linecorp.lich.viewmodel.internal.LichViewModels

/**
 * Returns an existing ViewModel or creates a new one, associated with the given
 * [viewModelStoreOwner].
 *
 * By default, the owner is provided by [LocalViewModelStoreOwner]. It is a Navigation back stack if
 * the current composable is in a [NavHost](https://developer.android.com/jetpack/compose/navigation).
 * Otherwise, the owner is usually a Fragment or an Activity.
 *
 * The created ViewModel is associated with the given [viewModelStoreOwner] and will be retained
 * as long as the owner is alive (e.g. if it is an Activity, until it is finished or process is
 * killed).
 *
 * This is a sample code:
 * ```
 * @Composable
 * fun FooScreen(fooViewModel: FooViewModel = lichViewModel(FooViewModel)) {
 *     // Use fooViewModel here.
 * }
 * ```
 *
 * @param factory A [ViewModelFactory] to create the ViewModel.
 * @param viewModelStoreOwner The owner of the ViewModel that controls the scope and lifetime of
 * the returned ViewModel. Defaults to using [LocalViewModelStoreOwner].
 */
@Composable
fun <T : AbstractViewModel> lichViewModel(
    factory: ViewModelFactory<T>,
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
): T {
    val context = LocalContext.current
    return if (viewModelStoreOwner is HasDefaultViewModelProviderFactory) {
        LichViewModels.getViewModelWithDefaultArguments(
            context,
            viewModelStoreOwner,
            viewModelStoreOwner,
            factory
        )
    } else {
        val savedStateRegistryOwner = viewModelStoreOwner as? SavedStateRegistryOwner
            ?: LocalSavedStateRegistryOwner.current
        LichViewModels.getViewModelWithExplicitArguments(
            context,
            viewModelStoreOwner,
            savedStateRegistryOwner,
            factory,
            null
        )
    }
}

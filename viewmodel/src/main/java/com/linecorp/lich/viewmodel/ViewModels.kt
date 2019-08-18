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
@file:JvmName("ViewModels")

package com.linecorp.lich.viewmodel

import android.content.Context
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelStoreOwner
import com.linecorp.lich.viewmodel.provider.BridgeViewModelProviderOwner

/**
 * Returns an existing ViewModel or creates a new one, associated with this Activity.
 *
 * In most cases, we recommend to use [FragmentActivity.viewModel] instead.
 *
 * This is a sample code:
 * ```
 * class FooActivity : FragmentActivity() {
 *
 *     private lateinit var fooViewModel: FooViewModel
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         fooViewModel = getViewModel(FooViewModel)
 *     }
 *
 *     // snip...
 * }
 * ```
 *
 * @param factory [ViewModelFactory] to create the ViewModel.
 */
@MainThread
fun <T : AbstractViewModel> FragmentActivity.getViewModel(factory: ViewModelFactory<T>): T =
    getViewModel(this, factory)

/**
 * Returns an existing ViewModel or creates a new one, associated with this Fragment.
 *
 * In most cases, we recommend to use [Fragment.viewModel] instead.
 *
 * This is a sample code:
 * ```
 * class FooFragment : Fragment() {
 *
 *     private lateinit var fooViewModel: FooViewModel
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         fooViewModel = getViewModel(FooViewModel)
 *     }
 *
 *     // snip...
 * }
 * ```
 *
 * @param factory [ViewModelFactory] to create the ViewModel.
 */
@MainThread
fun <T : AbstractViewModel> Fragment.getViewModel(factory: ViewModelFactory<T>): T =
    requireContext().getViewModel(this, factory)

/**
 * Returns an existing ViewModel or creates a new one, associated with the Activity hosting this
 * Fragment.
 * You can use this ViewModel to share data between Fragments and their host Activity.
 *
 * In most cases, we recommend to use [Fragment.activityViewModel] instead.
 *
 * This is a sample code:
 * ```
 * class FooFragment : Fragment() {
 *
 *     private lateinit var fooActivityViewModel: FooViewModel
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         fooActivityViewModel = getActivityViewModel(FooViewModel)
 *     }
 *
 *     // snip...
 * }
 * ```
 *
 * @param factory [ViewModelFactory] to create the ViewModel.
 */
@MainThread
fun <T : AbstractViewModel> Fragment.getActivityViewModel(factory: ViewModelFactory<T>): T =
    requireActivity().getViewModel(factory)

private fun <T : AbstractViewModel> Context.getViewModel(
    viewModelStoreOwner: ViewModelStoreOwner,
    viewModelFactory: ViewModelFactory<T>
): T {
    val providerOwner = applicationContext as? BridgeViewModelProviderOwner
        ?: throw RuntimeException(
            "The applicationContext isn't implementing BridgeViewModelProviderOwner. " +
                "Please refer to the document of BridgeViewModelProviderOwner."
        )
    return providerOwner.bridgeViewModelProvider.getViewModel(
        this,
        viewModelStoreOwner,
        viewModelFactory
    )
}

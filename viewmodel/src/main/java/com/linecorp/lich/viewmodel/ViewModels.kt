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
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.linecorp.lich.viewmodel.internal.lichViewModelProvider

/**
 * Returns an existing ViewModel or creates a new one, associated with this Activity.
 *
 * In most cases, we recommend to use [ComponentActivity.viewModel] instead.
 *
 * This is a sample code:
 * ```
 * class FooActivity : AppCompatActivity() {
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
 * @param arguments initial values for a new `SavedStateHandle` passed down to the ViewModel.
 * If omitted, the value of `getIntent()?.getExtras()` for this Activity is used.
 */
@JvmOverloads
@MainThread
fun <T : AbstractViewModel> ComponentActivity.getViewModel(
    factory: ViewModelFactory<T>,
    arguments: Bundle? = intent?.extras
): T = getViewModel(this, this, factory, arguments)

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
 * @param arguments initial values for a new `SavedStateHandle` passed down to the ViewModel.
 * If omitted, the value of `getArguments()` for this Fragment is used.
 */
@JvmOverloads
@MainThread
fun <T : AbstractViewModel> Fragment.getViewModel(
    factory: ViewModelFactory<T>,
    arguments: Bundle? = this.arguments
): T = requireContext().getViewModel(this, this, factory, arguments)

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
 * @param arguments initial values for a new `SavedStateHandle` passed down to the ViewModel.
 * If omitted, the value of `getIntent()?.getExtras()` for the host Activity is used.
 */
@JvmOverloads
@MainThread
fun <T : AbstractViewModel> Fragment.getActivityViewModel(
    factory: ViewModelFactory<T>,
    arguments: Bundle? = requireActivity().intent?.extras
): T = requireActivity().getViewModel(factory, arguments)

@MainThread
internal fun <T : AbstractViewModel> Context.getViewModel(
    viewModelStoreOwner: ViewModelStoreOwner,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    factory: ViewModelFactory<T>,
    arguments: Bundle?
): T = lichViewModelProvider.getViewModel(
    this,
    viewModelStoreOwner,
    savedStateRegistryOwner,
    factory,
    arguments
)

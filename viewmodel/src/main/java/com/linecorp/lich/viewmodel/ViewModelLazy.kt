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
package com.linecorp.lich.viewmodel

import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment

/**
 * Creates a new instance of a [Lazy] that returns an existing ViewModel or creates a new one,
 * associated with this Activity.
 *
 * This is a sample code:
 * ```
 * class FooActivity : AppCompatActivity() {
 *
 *     // An instance of FooViewModel associated with FooActivity.
 *     private val fooViewModel by viewModel(FooViewModel)
 *
 *     // snip...
 * }
 * ```
 *
 * @param factory [ViewModelFactory] to create the ViewModel.
 */
@MainThread
fun <T : AbstractViewModel> ComponentActivity.viewModel(factory: ViewModelFactory<T>): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE) { getViewModel(factory) }

/**
 * Creates a new instance of a [Lazy] that returns an existing ViewModel or creates a new one,
 * associated with this Fragment.
 *
 * This is a sample code:
 * ```
 * class FooFragment : Fragment() {
 *
 *     // An instance of FooViewModel associated with FooFragment.
 *     private val fooViewModel by viewModel(FooViewModel)
 *
 *     // snip...
 * }
 * ```
 *
 * @param factory [ViewModelFactory] to create the ViewModel.
 */
@MainThread
fun <T : AbstractViewModel> Fragment.viewModel(factory: ViewModelFactory<T>): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE) { getViewModel(factory) }

/**
 * Creates a new instance of a [Lazy] that returns an existing ViewModel or creates a new one,
 * associated with the Activity hosting this Fragment.
 * You can use this ViewModel to share data between Fragments and their host Activity.
 *
 * This is a sample code:
 * ```
 * class FooFragment : Fragment() {
 *
 *     // A shared instance of FooViewModel associated with the Activity hosting this FooFragment.
 *     private val fooActivityViewModel by activityViewModel(FooViewModel)
 *
 *     // snip...
 * }
 * ```
 *
 * @param factory [ViewModelFactory] to create the ViewModel.
 */
@MainThread
fun <T : AbstractViewModel> Fragment.activityViewModel(factory: ViewModelFactory<T>): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE) { getActivityViewModel(factory) }

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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment

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
 * @param argumentsFunction a function that returns initial values for a new `SavedStateHandle`
 * passed down to the ViewModel. If omitted, `getIntent()?.getExtras()` for this Activity is used.
 */
@MainThread
fun <T : AbstractViewModel> ComponentActivity.viewModel(
    factory: ViewModelFactory<T>,
    argumentsFunction: ComponentActivity.() -> Bundle? = { intent?.extras }
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    getViewModel(factory, argumentsFunction())
}

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
 * @param argumentsFunction a function that returns initial values for a new `SavedStateHandle`
 * passed down to the ViewModel. If omitted, `getArguments()` for this Fragment is used.
 */
@MainThread
fun <T : AbstractViewModel> Fragment.viewModel(
    factory: ViewModelFactory<T>,
    argumentsFunction: Fragment.() -> Bundle? = { arguments }
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    getViewModel(factory, argumentsFunction())
}

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
 * @param argumentsFunction a function that returns initial values for a new `SavedStateHandle`
 * passed down to the ViewModel. If omitted, `getIntent()?.getExtras()` for the host Activity is used.
 */
@MainThread
fun <T : AbstractViewModel> Fragment.activityViewModel(
    factory: ViewModelFactory<T>,
    argumentsFunction: FragmentActivity.() -> Bundle? = { intent?.extras }
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    requireActivity().let { it.getViewModel(factory, it.argumentsFunction()) }
}

/**
 * Creates a new instance of a [Lazy] that returns an existing ViewModel or creates a new one,
 * scoped to a navigation graph present on the `NavController` back stack.
 * You can use this ViewModel to share data between Fragments in the same navigation graph.
 *
 * This is a sample code:
 * ```
 * class FooFragment : Fragment() {
 *
 *     // A shared instance of FooViewModel scoped to the `foo_nav_graph` navigation graph.
 *     private val fooNavGraphViewModel by navGraphViewModel(FooViewModel, R.id.foo_nav_graph)
 *
 *     // snip...
 * }
 * ```
 *
 * @param factory [ViewModelFactory] to create the ViewModel.
 * @param navGraphId the ID of a `NavGraph` that exists on the `NavController` back stack.
 */
@MainThread
fun <T : AbstractViewModel> Fragment.navGraphViewModel(
    factory: ViewModelFactory<T>,
    @IdRes navGraphId: Int
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    NavHostFragment.findNavController(this).getBackStackEntry(navGraphId).let {
        requireContext().getViewModel(it, it, factory, it.arguments)
    }
}

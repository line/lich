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

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment

/**
 * A class to declare a factory of a ViewModel.
 * Inherit this class for the companion object of a ViewModel class.
 *
 * This is a sample code:
 * ```
 * class FooViewModel(private val context: Context, savedState: SavedState) : AbstractViewModel() {
 *
 *     // snip...
 *
 *     companion object : ViewModelFactory<FooViewModel>() {
 *         override fun createViewModel(context: Context, savedState: SavedState): FooViewModel =
 *             FooViewModel(context, savedState)
 *     }
 * }
 * ```
 *
 * Then, you can obtain an instance of this ViewModel using [ComponentActivity.viewModel] or
 * [Fragment.viewModel] like this:
 *
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
 * You can also use [Fragment.activityViewModel] to obtain a ViewModel associated with the Activity
 * hosting the current Fragment. This ViewModel can be used to share data between Fragments and
 * their host Activity.
 *
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
 * @param T the type of the ViewModel that will be created by this factory.
 */
abstract class ViewModelFactory<T : AbstractViewModel> {
    /**
     * Creates a ViewModel for this factory.
     *
     * @param context the application context.
     * @param savedState a handle to saved state.
     */
    @MainThread
    protected open fun createViewModel(context: Context, savedState: SavedState): T {
        @Suppress("DEPRECATION")
        return createViewModel(context)
    }

    @Deprecated(
        message = "Only for backward compatibility. Will be removed in 2.1.0",
        replaceWith = ReplaceWith("createViewModel(context, handle)")
    )
    protected open fun createViewModel(context: Context): T {
        throw NotImplementedError()
    }

    internal fun create(context: Context, savedState: SavedState): T =
        createViewModel(context, savedState)
}

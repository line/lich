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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment

/**
 * A base interface for classes generated from [GenerateArgs].
 */
interface ViewModelArgs {
    /**
     * Returns a [Bundle] that contains the values of this class.
     */
    fun toBundle(): Bundle
}

/**
 * Adds the given [viewModelArgs] to the intent.
 *
 * The [viewModelArgs] will be the default argument of [ComponentActivity.viewModel],
 * [ComponentActivity.getViewModel], [Fragment.activityViewModel] and [Fragment.getActivityViewModel].
 */
fun Intent.putViewModelArgs(viewModelArgs: ViewModelArgs): Intent =
    putExtras(viewModelArgs.toBundle())

/**
 * Sets the given [viewModelArgs] to the Fragment.
 *
 * The [viewModelArgs] will be the default argument of [Fragment.viewModel] and [Fragment.getViewModel].
 */
fun Fragment.setViewModelArgs(viewModelArgs: ViewModelArgs) {
    arguments = viewModelArgs.toBundle()
}

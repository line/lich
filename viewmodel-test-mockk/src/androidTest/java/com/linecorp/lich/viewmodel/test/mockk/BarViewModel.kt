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
package com.linecorp.lich.viewmodel.test.mockk

import android.content.Context
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.SavedState
import com.linecorp.lich.viewmodel.ViewModelFactory
import com.linecorp.lich.viewmodel.initial

// Mocking of final classes is only supported for Android P or later.
// https://mockk.io/ANDROID.html
class BarViewModel(savedState: SavedState) : AbstractViewModel() {

    private var itemCount: Int by savedState.initial(0)

    fun greeting(): String {
        return "Hello, I'm Bar."
    }

    fun countItem(): Int {
        return itemCount
    }

    companion object : ViewModelFactory<BarViewModel>() {
        override fun createViewModel(context: Context, savedState: SavedState): BarViewModel =
            BarViewModel(savedState)
    }
}

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
import androidx.lifecycle.MutableLiveData

@GenerateArgs
class TestViewModel(val context: Context, savedState: SavedState) : AbstractViewModel() {

    @Argument
    val param1: String by savedState.required()

    @Argument(isOptional = true)
    val param2: Int by savedState.initial(42)

    @Argument(isOptional = true)
    val param3: MutableLiveData<String> by savedState.liveData()

    var isCleared: Boolean = false

    override fun onCleared() {
        isCleared = true
        println("TestViewModel: cleared.")
    }

    companion object : ViewModelFactory<TestViewModel>() {
        override fun createViewModel(context: Context, savedState: SavedState): TestViewModel =
            TestViewModel(context, savedState)
    }
}

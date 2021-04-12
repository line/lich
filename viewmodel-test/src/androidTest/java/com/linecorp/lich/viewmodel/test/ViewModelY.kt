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
package com.linecorp.lich.viewmodel.test

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.linecorp.lich.savedstate.Argument
import com.linecorp.lich.savedstate.GenerateArgs
import com.linecorp.lich.savedstate.required
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory

@GenerateArgs
class ViewModelY(savedState: SavedStateHandle) : AbstractViewModel() {

    @Argument
    val message: String by savedState.required()

    companion object : ViewModelFactory<ViewModelY>() {
        override fun createViewModel(
            context: Context,
            savedStateHandle: SavedStateHandle
        ): ViewModelY = ViewModelY(savedStateHandle)
    }
}

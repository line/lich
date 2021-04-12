/*
 * Copyright 2020 LINE Corporation
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
package com.linecorp.lich.sample.navgraph

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import com.linecorp.lich.savedstate.Argument
import com.linecorp.lich.savedstate.GenerateArgs
import com.linecorp.lich.savedstate.liveDataWithInitial
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import java.util.concurrent.atomic.AtomicInteger

@GenerateArgs
class NavHostViewModel private constructor(
    instanceName: String,
    savedState: SavedStateHandle
) : AbstractViewModel() {

    @Argument(isOptional = true)
    private val argument: MutableLiveData<String> by savedState.liveDataWithInitial("not set")

    val message: LiveData<String> = argument.map { "$instanceName ($it)" }

    companion object : ViewModelFactory<NavHostViewModel>() {
        override fun createViewModel(
            context: Context,
            savedStateHandle: SavedStateHandle
        ): NavHostViewModel = NavHostViewModel(
            "NavHost #${instanceCounter.incrementAndGet()}",
            savedStateHandle
        )

        private val instanceCounter: AtomicInteger = AtomicInteger()
    }
}

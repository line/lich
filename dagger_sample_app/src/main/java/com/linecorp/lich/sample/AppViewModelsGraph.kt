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
package com.linecorp.lich.sample

import androidx.lifecycle.SavedStateHandle
import com.linecorp.lich.sample.mvvm.SampleViewModel
import dagger.BindsInstance
import dagger.Subcomponent

/**
 * A Dagger [Subcomponent] that instantiates ViewModels in the `dagger_sample_app` subproject.
 */
@Subcomponent
interface AppViewModelsGraph {

    fun sampleViewModel(): SampleViewModel

    @Subcomponent.Factory
    interface Factory {
        fun create(@BindsInstance savedStateHandle: SavedStateHandle): AppViewModelsGraph
    }
}

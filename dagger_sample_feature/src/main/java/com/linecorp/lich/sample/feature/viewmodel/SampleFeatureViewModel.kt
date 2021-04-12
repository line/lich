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
package com.linecorp.lich.sample.feature.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import com.linecorp.lich.component.getComponent
import com.linecorp.lich.sample.feature.SampleFeatureGraph
import com.linecorp.lich.sample.feature.repository.SampleFeatureRepository
import com.linecorp.lich.savedstate.Argument
import com.linecorp.lich.savedstate.GenerateArgs
import com.linecorp.lich.savedstate.required
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import javax.inject.Inject

@GenerateArgs
class SampleFeatureViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val sampleFeatureRepository: SampleFeatureRepository
) : AbstractViewModel() {

    @Argument
    private val activityName: String by savedState.required()

    val message: LiveData<String>
        get() = sampleFeatureRepository.getMessageAsLiveData(activityName)

    companion object : ViewModelFactory<SampleFeatureViewModel>() {
        override fun createViewModel(
            context: Context,
            savedStateHandle: SavedStateHandle
        ): SampleFeatureViewModel =
            context.getComponent(SampleFeatureGraph).viewModelsGraphFactory()
                .create(savedStateHandle)
                .sampleFeatureViewModel()
    }
}

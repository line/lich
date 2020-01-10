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
import com.linecorp.lich.component.component
import com.linecorp.lich.sample.feature.repository.SampleFeatureRepository
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.Argument
import com.linecorp.lich.viewmodel.GenerateArgs
import com.linecorp.lich.viewmodel.SavedState
import com.linecorp.lich.viewmodel.ViewModelFactory

@GenerateArgs
class SampleFeatureViewModel private constructor(
    context: Context,
    savedState: SavedState
) : AbstractViewModel() {

    private val sampleFeatureRepository by context.component(SampleFeatureRepository)

    @Argument
    private val activityName: String by savedState.required()

    val message: LiveData<String>
        get() = sampleFeatureRepository.getMessageAsLiveData(activityName)

    companion object : ViewModelFactory<SampleFeatureViewModel>() {
        override fun createViewModel(
            context: Context,
            savedState: SavedState
        ): SampleFeatureViewModel = SampleFeatureViewModel(context, savedState)
    }
}

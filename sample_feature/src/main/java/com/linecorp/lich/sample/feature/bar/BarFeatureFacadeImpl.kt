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
package com.linecorp.lich.sample.feature.bar

import android.content.Context
import android.content.Intent
import com.google.auto.service.AutoService
import com.linecorp.lich.component.ServiceLoaderComponent

/**
 * The implementation of [BarFeatureFacade].
 */
@AutoService(BarFeatureFacade::class)
class BarFeatureFacadeImpl : BarFeatureFacade, ServiceLoaderComponent {

    private lateinit var context: Context

    override fun init(context: Context) {
        this.context = context
    }

    override fun launchBarFeatureActivity() {
        val intent = Intent(context, BarFeatureActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

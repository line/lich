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
package com.linecorp.lich.sample

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.linecorp.lich.component.component
import com.linecorp.lich.sample.feature.bar.BarFeatureFacade
import com.linecorp.lich.sample.feature.foo.FooFeatureFacade
import com.linecorp.lich.sample.mvvm.MvvmSampleActivity
import com.linecorp.lich.sample.simplecoroutine.SimpleCoroutineActivity

class MainActivity : AppCompatActivity() {

    private val fooFeatureFacade by component(FooFeatureFacade)

    private val barFeatureFacade by component(BarFeatureFacade)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
    }

    fun launchSimpleCoroutineActivity(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, SimpleCoroutineActivity::class.java))
    }

    fun launchMvvmSampleActivity(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, MvvmSampleActivity::class.java))
    }

    fun launchFooFeatureActivity(@Suppress("UNUSED_PARAMETER") view: View) {
        fooFeatureFacade.launchFooFeatureActivity()
    }

    fun launchBarFeatureActivity(@Suppress("UNUSED_PARAMETER") view: View) {
        barFeatureFacade.launchBarFeatureActivity()
    }
}

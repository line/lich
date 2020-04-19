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
import androidx.appcompat.app.AppCompatActivity
import com.linecorp.lich.component.component
import com.linecorp.lich.sample.databinding.MainActivityBinding
import com.linecorp.lich.sample.feature.bar.BarFeatureFacade
import com.linecorp.lich.sample.feature.foo.FooFeatureFacade
import com.linecorp.lich.sample.lifecyclescope.LifecycleScopeDemoActivity
import com.linecorp.lich.sample.mvvm.MvvmSampleActivity
import com.linecorp.lich.sample.simplecoroutine.SimpleCoroutineActivity

class MainActivity : AppCompatActivity() {

    private val fooFeatureFacade by component(FooFeatureFacade)

    private val barFeatureFacade by component(BarFeatureFacade)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.launchSimpleCoroutine.setOnClickListener {
            startActivity(Intent(this, SimpleCoroutineActivity::class.java))
        }
        binding.launchLifecyclescopeDemo.setOnClickListener {
            startActivity(Intent(this, LifecycleScopeDemoActivity::class.java))
        }
        binding.launchMvvmSample.setOnClickListener {
            startActivity(MvvmSampleActivity.newIntent(this, "mvvm"))
        }
        binding.launchFooFeature.setOnClickListener {
            fooFeatureFacade.launchFooFeatureActivity()
        }
        binding.launchBarFeature.setOnClickListener {
            barFeatureFacade.launchBarFeatureActivity()
        }
    }
}

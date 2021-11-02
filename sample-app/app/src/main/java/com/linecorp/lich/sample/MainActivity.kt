/*
 * Copyright 2021 LINE Corporation
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
import com.linecorp.lich.sample.databinding.MainActivityBinding
import com.linecorp.lich.sample.lifecyclescope.LifecycleScopeDemoActivity
import com.linecorp.lich.sample.lifecyclescope.SimpleCoroutineActivity
import com.linecorp.lich.sample.navgraph.NavHostActivity
import com.linecorp.lich.sample.ui.MvvmSampleActivity

class MainActivity : AppCompatActivity() {

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
        binding.launchNavHost.setOnClickListener {
            startActivity(NavHostActivity.newIntent(this, "From MainActivity"))
        }
        binding.launchNavDeepLink.setOnClickListener {
            NavHostActivity.launchDeepLink(this, R.id.third_fragment, "From DeepLink")
        }
    }
}

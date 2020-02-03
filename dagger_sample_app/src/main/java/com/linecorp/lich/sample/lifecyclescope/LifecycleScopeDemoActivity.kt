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
package com.linecorp.lich.sample.lifecyclescope

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.linecorp.lich.sample.R

/**
 * An Activity to demonstrate the difference between `lifecycleScope` and `AutoResetLifecycleScope`.
 *
 * For details, please refer [LifecycleScopeDemo1Fragment].
 */
class LifecycleScopeDemoActivity : AppCompatActivity(R.layout.lifecycle_scope_demo_activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                add<LifecycleScopeDemo1Fragment>(R.id.fragment_container)
            }
        }
    }
}

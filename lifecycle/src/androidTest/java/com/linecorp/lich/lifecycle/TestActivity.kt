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
package com.linecorp.lich.lifecycle

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class TestActivity : FragmentActivity() {

    private val scope: CoroutineScope = AutoResetLifecycleScope(this)

    lateinit var job: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("TestActivity.onCreate: currentState = ${lifecycle.currentState}")
    }

    override fun onStart() {
        super.onStart()

        println("TestActivity.onStart: currentState = ${lifecycle.currentState}")

        job = scope.launch {
            delay(Long.MAX_VALUE)
            fail()
        }
    }

    override fun onStop() {
        println("TestActivity.onStop: currentState = ${lifecycle.currentState}")

        assertTrue(job.isCancelled)
        assertTrue(scope.isActive)

        super.onStop()
    }

    override fun onDestroy() {
        println("TestActivity.onDestroy: currentState = ${lifecycle.currentState}")

        assertFalse(scope.isActive)

        val scopeCreatedAfterDestroy = AutoResetLifecycleScope(this)
        assertFalse(scopeCreatedAfterDestroy.isActive)

        super.onDestroy()
    }
}

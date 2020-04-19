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
package com.linecorp.lich.sample.simplecoroutine

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.linecorp.lich.lifecycle.AutoResetLifecycleScope
import com.linecorp.lich.sample.databinding.SimpleCoroutineActivityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SimpleCoroutineActivity : AppCompatActivity() {

    // Declare a CoroutineScope associated with the lifecycle of this activity.
    private val coroutineScope: CoroutineScope = AutoResetLifecycleScope(this)

    private lateinit var messageText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = SimpleCoroutineActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        messageText = binding.coroutineMessage

        binding.startCoroutineButton.setOnClickListener {
            startSomeTask()
        }
    }

    private fun startSomeTask() {
        // Since this coroutine is launched from AutoResetLifecycleScope, it will be automatically
        // cancelled ON_STOP and ON_DESTROY.
        coroutineScope.launch {
            // This task repeats one-second delay and text update.
            for (i in 1..10) {
                // `delay` *suspends* the current coroutine. It doesn't *block* the main thread.
                delay(1000L)
                // Since this coroutine is executed on the main UI thread, you can safely access to
                // any UI components.
                messageText.text = "Iteration = $i"
            }
        }
    }
}

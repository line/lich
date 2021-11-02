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
package com.linecorp.lich.sample.lifecyclescope

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.linecorp.lich.component.component
import com.linecorp.lich.lifecycle.AutoResetLifecycleScope
import com.linecorp.lich.sample.databinding.LifecycleScopeDemoActivityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * An activity to demonstrate the pitfall of using `lifecycleScope.launchWhenStarted` to collect
 * a [Flow] backed by a [Channel].
 */
class LifecycleScopeDemoActivity : AppCompatActivity() {

    private val autoResetLifecycleScope: CoroutineScope = AutoResetLifecycleScope(this)

    private val channelFlowRepository by component(ChannelFlowRepository)

    private lateinit var binding: LifecycleScopeDemoActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log("onCreate()")

        binding = LifecycleScopeDemoActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // DON'T DO THIS!!
        // A flow backed by a channel is NOT SAFE to collect with `launchWhenStarted`.
        // It will keep the underlying flow producer active while emitting items into the buffer
        // in the background, and thus wasting resources.
        // cf. https://link.medium.com/OR5ePKTGthb
        lifecycleScope.launchWhenStarted {
            channelFlowRepository.counterFlow("launchWhen").collect { value ->
                log("launchWhenStarted.collect: value=$value")
                binding.launchwhenstartedValue.text = value
            }
        }

        // It is SAFE to collect a flow with `lifecycleScope.launch` + `repeatOnLifecycle`.
        // The block passed to `repeatOnLifecycle` is executed when the lifecycle is at least
        // `STARTED` and is cancelled when the lifecycle is `STOPPED`.
        // It automatically restarts the block when the lifecycle is `STARTED` again.
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                channelFlowRepository.counterFlow("repeatOn").collect { value ->
                    log("repeatOnLifecycle.collect: value=$value")
                    binding.repeatonlifecycleValue.text = value
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        log("onStart()")

        // It is also SAFE to collect a flow with `AutoResetLifecycleScope`.
        // The coroutine launched from the `autoResetLifecycleScope` is automatically cancelled
        // when the lifecycle is `STOPPED`. In other words, this code is effectively equivalent to
        // the above code using `repeatOnLifecycle`.
        autoResetLifecycleScope.launch {
            channelFlowRepository.counterFlow("autoReset").collect { value ->
                log("autoResetLifecycleScope.collect: value=$value")
                binding.autoresetlifecyclescopeValue.text = value
            }
        }
    }

    override fun onStop() {
        super.onStop()
        log("onStop()")
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy()")
    }

    private fun log(message: String) {
        Log.i("LifecycleScopeDemo", message)
    }
}

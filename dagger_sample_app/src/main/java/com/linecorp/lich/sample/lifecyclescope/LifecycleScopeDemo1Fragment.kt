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
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.lifecycleScope
import com.linecorp.lich.lifecycle.AutoResetLifecycleScope
import com.linecorp.lich.sample.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This Fragment demonstrates the difference between Jetpack's [lifecycleScope] and
 * Lich's [AutoResetLifecycleScope].
 *
 * For [lifecycleScope], please read this document first.
 * https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope
 *
 * `lifecycleScope` has useful functions like `launchWhenStarted`, but these functions
 * must be used with care. Here are some things to keep in mind when using `lifecycleScope`:
 *
 * - Avoid using `Fragment.lifecycleScope.launch*`. Coroutines launched from
 * `Fragment.lifecycleScope` stay alive even after `onDestroyView()`. So, you need to
 * be careful about View recreation.
 * - `Fragment.viewLifecycleOwner.lifecycleScope.launchWhenStarted` is somewhat safe,
 * but its coroutines stay alive even after `onStop()` and restart execution after
 * the next `onStart()`. So, you shouldn't use it for tasks launched on `onStart()`.
 *
 * Thus, `lifecycleScope` has some pitfalls. So, you should consider using alternatives.
 *
 * The best way is to use coroutines only in ViewModels. All asynchronous tasks are launched
 * in ViewModels, and Fragment/Activity only observes the result via LiveData.
 * See [com.linecorp.lich.sample.mvvm.SampleViewModel] for a working example.
 *
 * Another alternative is using [AutoResetLifecycleScope]. Any coroutines launched from it
 * are just cancelled when `onStop()` is called. So it is relatively safe to use.
 */
class LifecycleScopeDemo1Fragment : Fragment(R.layout.lifecycle_scope_demo1_fragment) {

    private val autoResetLifecycleScope = AutoResetLifecycleScope(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Demo1Fragment", "onCreate")

        // This coroutine is scheduled to run while this Fragment is in the STARTED state,
        // and will be cancelled when onDestroy() is called. It means that the coroutine
        // stays alive even after onDestroyView(). Therefore, we must be aware that
        // Views may be recreated while the coroutine is alive.
        lifecycleScope.launchWhenStarted {
            incrementValuePeriodically(R.id.lifecyclescope_value)
        }.invokeOnCompletion {
            Log.i("Demo1Fragment", "lifecycleScope has been cancelled.")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("Demo1Fragment", "onViewCreated")

        view.findViewById<Button>(R.id.launch_fragment2_button).setOnClickListener {
            parentFragmentManager.commit {
                replace<LifecycleScopeDemo2Fragment>(R.id.fragment_container)
                addToBackStack(null)
            }
        }

        // This coroutine is scheduled to run while this Fragment is in the STARTED state,
        // and will be cancelled when onDestroyView() is called.
        // It means that the coroutine stays alive even after onStop() and restarts execution
        // after the next onStart().
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            incrementValuePeriodically(R.id.viewlifecyclescope_value)
        }.invokeOnCompletion {
            Log.i("Demo1Fragment", "viewLifecycleOwner.lifecycleScope has been cancelled.")
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i("Demo1Fragment", "onStart")

        // This coroutine is just cancelled when onStop() is called.
        autoResetLifecycleScope.launch {
            incrementValuePeriodically(R.id.autoresetlifecyclescope_value)
        }.invokeOnCompletion {
            Log.i("Demo1Fragment", "autoResetLifecycleScope has been reset.")
        }
    }

    /**
     * Continues incrementing the value of the given TextView every second until canceled.
     */
    @MainThread
    private suspend fun incrementValuePeriodically(@IdRes textViewId: Int): Nothing {
        var value = 0
        var previousView: TextView? = null

        while (true) {
            val textView: TextView = requireView().findViewById(textViewId)
            if (previousView != null && previousView !== textView) {
                textView.text = "This View has been recreated."
            } else {
                textView.text = value++.toString()
            }

            previousView = textView
            delay(1000L)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.i("Demo1Fragment", "onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i("Demo1Fragment", "onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("Demo1Fragment", "onDestroy")
    }
}

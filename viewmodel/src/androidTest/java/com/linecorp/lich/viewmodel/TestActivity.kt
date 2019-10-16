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
package com.linecorp.lich.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class TestActivity : FragmentActivity() {

    lateinit var testFragment: TestFragment

    val testViewModel by viewModel(TestViewModel)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        testFragment = supportFragmentManager.run {
            findFragmentByTag(FRAGMENT_TAG) as? TestFragment
                ?: TestFragment().also {
                    it.arguments = Bundle().apply {
                        putString("param1", "abc")
                        putInt("param2", 100)
                        putString("param3", "fooBar")
                    }
                    beginTransaction().add(it, FRAGMENT_TAG).commit()
                }
        }
    }

    override fun onStart() {
        super.onStart()
        println("TestActivity.onStart: testViewModel = $testViewModel")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("TestActivity.onDestroy: isChangingConfigurations = $isChangingConfigurations")
    }

    companion object {
        const val FRAGMENT_TAG: String = "TestFragment"

        fun newIntent(context: Context): Intent =
            Intent(context, TestActivity::class.java).apply {
                putExtra("param1", "xyz")
            }
    }
}

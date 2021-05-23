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
package com.linecorp.lich.savedstate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit

class TestActivity : FragmentActivity() {

    lateinit var testFragment: TestFragment

    val testViewModel: TestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        testFragment = supportFragmentManager.run {
            findFragmentByTag(FRAGMENT_TAG) as? TestFragment
                ?: newTestFragment().also { commit { add(it, FRAGMENT_TAG) } }
        }
    }

    private fun newTestFragment(): TestFragment =
        TestFragment().also {
            it.setViewModelArgs(
                TestViewModelArgs(
                    param1 = Uri.parse("http://example.jp"),
                    param2 = "TestFragment",
                    param3 = 200,
                    param4 = Uri.EMPTY,
                    param5 = "TestLiveData"
                )
            )
        }

    companion object {
        private const val FRAGMENT_TAG: String = "TestFragment"

        fun newIntent(context: Context): Intent =
            Intent(context, TestActivity::class.java).putViewModelArgs(
                TestViewModelArgs(
                    param1 = Uri.parse("http://example.com"),
                    param2 = "example"
                )
            )
    }
}

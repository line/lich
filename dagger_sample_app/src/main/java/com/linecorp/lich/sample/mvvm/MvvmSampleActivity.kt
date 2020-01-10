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
package com.linecorp.lich.sample.mvvm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.linecorp.lich.sample.R
import com.linecorp.lich.sample.databinding.MvvmSampleActivityBinding
import com.linecorp.lich.viewmodel.putViewModelArgs
import com.linecorp.lich.viewmodel.viewModel

class MvvmSampleActivity : AppCompatActivity() {

    private val sampleViewModel by viewModel(SampleViewModel)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: MvvmSampleActivityBinding =
            DataBindingUtil.setContentView(this, R.layout.mvvm_sample_activity)
        binding.lifecycleOwner = this
        binding.viewModel = sampleViewModel
    }

    companion object {
        fun newIntent(context: Context, counterName: String): Intent =
            Intent(context, MvvmSampleActivity::class.java).putViewModelArgs(
                SampleViewModelArgs(counterName)
            )
    }
}

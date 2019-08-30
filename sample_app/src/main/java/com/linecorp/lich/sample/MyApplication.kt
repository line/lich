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

import android.app.Application
import android.os.StrictMode
import com.linecorp.lich.viewmodel.provider.BridgeViewModelProvider
import com.linecorp.lich.viewmodel.provider.BridgeViewModelProviderOwner

class MyApplication : Application(), BridgeViewModelProviderOwner {

    override val bridgeViewModelProvider: BridgeViewModelProvider = BridgeViewModelProvider()

    override fun onCreate() {
        super.onCreate()

        StrictMode.enableDefaults()
    }
}

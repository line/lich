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
package com.linecorp.lich.sample.base

import android.content.Context
import android.os.Build
import com.linecorp.lich.component.DelegatedComponentFactory
import com.linecorp.lich.sample.BuildConfig
import okhttp3.OkHttpClient

/**
 * The actual factory class for [GlobalOkHttpClient].
 *
 * This class creates an [OkHttpClient] with an interceptor that sets a `User-Agent` request header.
 *
 * @see com.linecorp.lich.component.ComponentFactory.delegateCreation
 */
// NOTE: Since this class is instantiated using reflection, it must have a public empty constructor.
class GlobalOkHttpClientFactory : DelegatedComponentFactory<OkHttpClient>() {
    override fun createComponent(context: Context): OkHttpClient {
        val userAgentValue =
            "LichSample/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}; ${Build.MODEL})"
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Sets a `User-Agent` header to the request.
                val newRequest = chain.request().newBuilder()
                    .header("User-Agent", userAgentValue)
                    .build()
                chain.proceed(newRequest)
            }.build()
    }
}

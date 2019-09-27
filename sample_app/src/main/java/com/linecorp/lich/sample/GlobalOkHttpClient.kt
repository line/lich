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

import android.content.Context
import com.linecorp.lich.component.ComponentFactory
import okhttp3.OkHttpClient

/**
 * The application-wide singleton of [OkHttpClient].
 *
 * Since each [OkHttpClient] instance holds its own connection pool and thread pools, we should
 * share a single [OkHttpClient] instance across the app and reuse it for all of HTTP calls.
 * For details, please refer this document:
 * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/
 *
 * Here is an example:
 * ```
 * val okHttpClient = context.getComponent(GlobalOkHttpClient)
 * val result = okHttpClient.call(request) { response ->
 *     if (!response.isSuccessful) {
 *         throw IOException("HTTP Response code: ${response.code()}")
 *     }
 *     checkNotNull(response.body()).string()
 * }
 * ```
 *
 * If you want to customize an [OkHttpClient] instance, use [OkHttpClient.newBuilder] like this:
 * ```
 * // Creates a new OkHttpClient that shares connection pool and thread pools with `GlobalOkHttpClient`.
 * val customizedOkHttpClient = context.getComponent(GlobalOkHttpClient).newBuilder()
 *     .readTimeout(500, TimeUnit.MILLISECONDS)
 *     .build()
 * ```
 */
object GlobalOkHttpClient : ComponentFactory<OkHttpClient>() {
    override fun createComponent(context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
        //// Apply custom settings.
        //builder.addInterceptor(LoggingInterceptor())
        return builder.build()
    }
}

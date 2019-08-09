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
import com.linecorp.lich.okhttp.call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * A component that makes HTTP calls upon a single [OkHttpClient] instance.
 *
 * Since each [OkHttpClient] instance holds its own connection pool and thread pools, we should
 * share a single [OkHttpClient] instance across the app and reuse it for all of HTTP calls.
 *
 * If you want to customize a [OkHttpClient] instance, use [OkHttpClient.newBuilder] like this:
 * ```
 * // Creates a new OkHttpClient that shares connection pool and thread pools with the HttpClient component.
 * val customizedOkHttpClient = context.getComponent(HttpClient).okHttpClient.newBuilder()
 *     .readTimeout(500, TimeUnit.MILLISECONDS)
 *     .build()
 * ```
 *
 * For details, please refer this document:
 * https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/
 *
 * @param okHttpClient the application-wide single instance of [OkHttpClient].
 */
class HttpClient(val okHttpClient: OkHttpClient) {
    /**
     * Executes the given [request] and converts its response using [responseMapper], then returns
     * the result.
     *
     * Since all I/O operations are executed on OkHttp's background threads, you can call this
     * function from any threads.
     *
     * @param request an HTTP request to be performed.
     * @param responseMapper a function to convert an HTTP response to your desired object. The
     * [Response] object will be automatically closed when this function has finished. This function
     * will be called from OkHttp's background threads.
     * @throws IOException
     */
    @Throws(IOException::class)
    suspend fun <T> call(request: Request, responseMapper: (Response) -> T): T =
        okHttpClient.call(request, responseMapper)

    companion object : ComponentFactory<HttpClient>() {
        override fun createComponent(context: Context): HttpClient =
            HttpClient(OkHttpClient())
    }
}

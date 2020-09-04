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
package com.linecorp.lich.okhttp

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

/**
 * Executes the given OkHttp [request] and then handles its response with the [responseHandler].
 *
 * This extension function is semantically equivalent to
 * `OkHttpClient.newCall(request).execute().use(responseHandler)`,
 * but with the following advantages:
 *
 * - All I/O operations are executed on OkHttp's background threads, and the current thread is not
 * blocked. So you can call this function from any coroutines without using
 * `withContext(Dispatchers.IO) { ... }`.
 * - Handles coroutine cancellations properly. If the current coroutine is cancelled, it immediately
 * cancels the HTTP call and throws a `CancellationException`.
 *
 * This is a sample code that fetches a content of the given URL as `String`.
 * ```
 * suspend fun fetchContentAsString(url: String): String {
 *     val request = Request.Builder().url(url).build()
 *     return okHttpClient.call(request) { response ->
 *         if (!response.isSuccessful) {
 *             throw IOException("HTTP Response code: ${response.code()}")
 *         }
 *         checkNotNull(response.body()).string()
 *     }
 * }
 * ```
 *
 * @param request an OkHttp [Request] to be executed.
 * @param responseHandler a function to process an OkHttp [Response]. The response object will be
 * closed automatically after the function call. This function is called from a background thread.
 * @return the result of [responseHandler].
 * @throws IOException
 */
suspend fun <T> OkHttpClient.call(request: Request, responseHandler: (Response) -> T): T =
    suspendCancellableCoroutine { cont ->
        val call = newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val result = runCatching { response.use(responseHandler) }
                cont.resumeWith(result)
            }

            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }
        })
    }

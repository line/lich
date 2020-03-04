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
 * Executes the given [request] and converts its response using [responseMapper], then returns
 * the result.
 *
 * Since all I/O operations are executed on OkHttp's background threads, you can call this function
 * from any threads.
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
 * @param request an HTTP request to be performed.
 * @param responseMapper a function to convert an HTTP response to your desired object. The
 * [Response] object will be automatically closed when this function has finished. This function
 * will be called from OkHttp's background threads.
 * @throws IOException
 */
suspend fun <T> OkHttpClient.call(request: Request, responseMapper: (Response) -> T): T =
    suspendCancellableCoroutine { cont ->
        val call = newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val result = runCatching { response.use(responseMapper) }
                cont.resumeWith(result)
            }

            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }
        })
    }

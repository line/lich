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
package com.linecorp.lich.sample.remote

import android.content.Context
import android.util.Log
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.component
import com.linecorp.lich.okhttp.ResponseStatusException
import com.linecorp.lich.okhttp.call
import com.linecorp.lich.sample.GlobalOkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

class CounterServiceClient private constructor(context: Context) {

    private val okHttpClient by context.component(GlobalOkHttpClient)

    /**
     * Query the server for the initial value of the given `Counter`.
     *
     * @param counterName the name of the `Counter`.
     * @return the initial value for the counter.
     * @throws IOException
     */
    suspend fun getInitialCounterValue(counterName: String): Int {
        Log.i("CounterServiceClient", "getInitialCounterValue: counterName = $counterName")
        // We use "jsonplaceholder.typicode.com" for a fake API server.
        // In this function, we use the number of the entries for FAKE_SERVICE_URL as the counter's
        // initial value.
        val request = Request.Builder().url(FAKE_SERVICE_URL).build()
        return okHttpClient.call(request) { response ->
            if (!response.isSuccessful) {
                throw ResponseStatusException(response.code)
            }
            try {
                val entries = JSONArray(checkNotNull(response.body).string())
                entries.length()
            } catch (e: JSONException) {
                throw IOException(e)
            }
        }
    }

    companion object : ComponentFactory<CounterServiceClient>() {
        override fun createComponent(context: Context): CounterServiceClient =
            CounterServiceClient(context)

        private const val FAKE_SERVICE_URL = "https://jsonplaceholder.typicode.com/photos?albumId=1"
    }
}

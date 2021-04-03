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
package com.linecorp.lich.okhttp

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class CallTest {

    private lateinit var server: MockWebServer

    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        okHttpClient = OkHttpClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testDownload() = runBlocking {
        val data = ByteArray(20000) { it.toByte() }

        server.enqueue(MockResponse().apply {
            setBody(Buffer().write(data))
            throttleBody(1000, 50, TimeUnit.MILLISECONDS)
        })
        server.start()

        val request = Request.Builder().url(server.url("/foo")).build()
        val result = okHttpClient.call(request) { response ->
            assertTrue(response.isSuccessful)
            checkNotNull(response.body).bytes()
        }
        server.takeRequest()

        assertTrue(data contentEquals result)
    }

    @Test
    fun testUpload() = runBlocking {
        val data = ByteArray(20000) { it.toByte() }

        server.enqueue(MockResponse().apply {
            setBody("DONE.")
            throttleBody(1000, 50, TimeUnit.MILLISECONDS)
        })
        server.start()

        val request = Request.Builder()
            .url(server.url("/foo"))
            .post(data.toRequestBody())
            .build()
        val result = okHttpClient.call(request) { response ->
            assertTrue(response.isSuccessful)
            checkNotNull(response.body).string()
        }

        assertEquals("DONE.", result)
        assertTrue(data contentEquals server.takeRequest().body.readByteArray())
    }

    @Test
    fun testCancel() = runBlocking {
        val data = ByteArray(20000) { it.toByte() }

        server.enqueue(MockResponse().apply {
            setBody(Buffer().write(data))
            throttleBody(1000, 100, TimeUnit.MILLISECONDS)
        })
        server.start()

        val request = Request.Builder().url(server.url("/foo")).build()
        val result = withTimeoutOrNull(200) {
            okHttpClient.call(request) { response ->
                assertTrue(response.isSuccessful)
                checkNotNull(response.body).bytes()
                fail("This call should be cancelled before `ResponseBody.bytes()` completes.")
            }
        }
        server.takeRequest()

        assertNull(result)
    }

    @Test
    fun testErrorStatusCode() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        server.start()

        val request = Request.Builder().url(server.url("/foo")).build()
        assertFailsWith<ResponseStatusException> {
            okHttpClient.call(request) { response ->
                if (!response.isSuccessful) {
                    throw ResponseStatusException(response.code)
                }
                fail("Not reached.")
            }
        }.let { exception ->
            assertEquals(500, exception.code)
        }
    }

    @Test
    fun testRuntimeException() = runBlocking {
        server.enqueue(MockResponse())
        server.start()

        val request = Request.Builder().url(server.url("/foo")).build()
        assertFailsWith<RuntimeException> {
            okHttpClient.call(request) {
                throw RuntimeException("FOO")
            }
        }.let { exception ->
            assertEquals("FOO", exception.message)
        }
    }
}

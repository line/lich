/*
 * Copyright 2020 LINE Corporation
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

import com.linecorp.lich.okhttp.CallState.Downloading
import com.linecorp.lich.okhttp.CallState.Failure
import com.linecorp.lich.okhttp.CallState.Success
import com.linecorp.lich.okhttp.CallState.Uploading
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.BufferedSink
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class CallWithCountingTest {

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
            body = Buffer().write(data)
            throttleBody(1000, 50, TimeUnit.MILLISECONDS)
        })
        server.start()

        val request = Request.Builder().url(server.url("/foo")).build()
        val callStateList = okHttpClient.callWithCounting(request) { response ->
            assertTrue(response.isSuccessful)
            checkNotNull(response.body()).bytes()
        }.toList()

        callStateList.subList(0, callStateList.size - 1).forEach { state ->
            assertTrue(state is Downloading)
            assertEquals(20000, state.bytesTotal)
        }
        callStateList.last().let { lastState ->
            assertTrue(lastState is Success)
            assertTrue(data contentEquals lastState.data)
            assertEquals(-1, lastState.bytesUploaded)
            assertEquals(20000, lastState.bytesDownloaded)
        }
    }

    @Test
    fun testChunkedDownload() = runBlocking {
        val data = ByteArray(20000) { it.toByte() }

        server.enqueue(MockResponse().apply {
            setChunkedBody(Buffer().write(data), 300)
            throttleBody(1000, 50, TimeUnit.MILLISECONDS)
        })
        server.start()

        val request = Request.Builder().url(server.url("/foo")).build()
        val callStateList = okHttpClient.callWithCounting(request) { response ->
            assertTrue(response.isSuccessful)
            checkNotNull(response.body()).bytes()
        }.toList()

        callStateList.subList(0, callStateList.size - 1).forEach { state ->
            assertTrue(state is Downloading)
            assertEquals(-1, state.bytesTotal)
        }
        callStateList.last().let { lastState ->
            assertTrue(lastState is Success)
            assertTrue(data contentEquals lastState.data)
            assertEquals(-1, lastState.bytesUploaded)
            assertEquals(20000, lastState.bytesDownloaded)
        }
    }

    @Test
    fun testPartialContent() = runBlocking {
        val data = ByteArray(10000) { it.toByte() }

        server.enqueue(MockResponse().apply {
            setResponseCode(StatusCode.PARTIAL_CONTENT)
            setHeader("Content-Range", "bytes 10000-19999/20000")
            body = Buffer().write(data)
            throttleBody(1000, 50, TimeUnit.MILLISECONDS)
        })
        server.start()

        val request = Request.Builder()
            .url(server.url("/foo"))
            .setRangeHeader(10000)
            .build()
        assertEquals("bytes=10000-", request.header("Range"))
        val callStateList = okHttpClient.callWithCounting(request) { response ->
            assertEquals(StatusCode.PARTIAL_CONTENT, response.code())
            checkNotNull(response.body()).bytes()
        }.toList()

        callStateList.subList(0, callStateList.size - 1).forEach { state ->
            assertTrue(state is Downloading)
            assertTrue(state.bytesTransferred >= 10000)
            assertEquals(20000, state.bytesTotal)
        }
        callStateList.last().let { lastState ->
            assertTrue(lastState is Success)
            assertTrue(data contentEquals lastState.data)
            assertEquals(-1, lastState.bytesUploaded)
            assertEquals(20000, lastState.bytesDownloaded)
        }
    }

    @Test
    fun testRangeNotSatisfiable() = runBlocking {
        server.enqueue(MockResponse().apply {
            setResponseCode(StatusCode.RANGE_NOT_SATISFIABLE)
            setHeader("Content-Range", "bytes */20000")
            setBody("Range Not Satisfiable.")
        })
        server.start()

        val request = Request.Builder()
            .url(server.url("/foo"))
            .setRangeHeader(20000)
            .build()
        assertEquals("bytes=20000-", request.header("Range"))
        val callStateList = okHttpClient.callWithCounting(request) { response ->
            assertEquals(StatusCode.RANGE_NOT_SATISFIABLE, response.code())
            checkNotNull(response.body()).string()
        }.toList()

        assertEquals(1, callStateList.size)
        callStateList.last().let { lastState ->
            assertTrue(lastState is Success)
            assertEquals("Range Not Satisfiable.", lastState.data)
            assertEquals(-1, lastState.bytesUploaded)
            assertEquals(20000, lastState.bytesDownloaded)
        }
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
            .post(RequestBody.create(null, data))
            .build()
        val callStateList = okHttpClient.callWithCounting(
            request,
            countDownload = false
        ) { response ->
            assertTrue(response.isSuccessful)
            checkNotNull(response.body()).string()
        }.toList()

        callStateList.subList(0, callStateList.size - 1).forEach { state ->
            assertTrue(state is Uploading)
            assertEquals(20000, state.bytesTotal)
        }
        callStateList.last().let { lastState ->
            assertTrue(lastState is Success)
            assertEquals("DONE.", lastState.data)
            assertEquals(20000, lastState.bytesUploaded)
            assertEquals(-1, lastState.bytesDownloaded)
        }
        assertTrue(data contentEquals server.takeRequest().body.readByteArray())
    }

    @Test
    fun testChunkedUpload() = runBlocking {
        val data = ByteArray(20000) { it.toByte() }

        server.enqueue(MockResponse().apply {
            setBody("DONE.")
            throttleBody(1000, 50, TimeUnit.MILLISECONDS)
        })
        server.start()

        val chunkedRequestBody = object : RequestBody() {
            override fun contentType(): MediaType? = null

            override fun writeTo(sink: BufferedSink) {
                val buffer = Buffer().write(data)
                while (!buffer.exhausted()) {
                    sink.write(buffer, min(buffer.size(), 300L))
                    sink.emit()
                }
            }
        }
        val request = Request.Builder()
            .url(server.url("/foo"))
            .post(chunkedRequestBody)
            .build()
        val callStateList = okHttpClient.callWithCounting(
            request,
            countDownload = false
        ) { response ->
            assertTrue(response.isSuccessful)
            checkNotNull(response.body()).string()
        }.toList()

        callStateList.subList(0, callStateList.size - 1).forEach { state ->
            assertTrue(state is Uploading)
            assertEquals(-1, state.bytesTotal)
        }
        callStateList.last().let { lastState ->
            assertTrue(lastState is Success)
            assertEquals("DONE.", lastState.data)
            assertEquals(20000, lastState.bytesUploaded)
            assertEquals(-1, lastState.bytesDownloaded)
        }
        assertTrue(data contentEquals server.takeRequest().body.readByteArray())
    }

    @Test
    fun testCancel() = runBlocking {
        val data = ByteArray(20000) { it.toByte() }

        server.enqueue(MockResponse().apply {
            setBody("DONE.")
            throttleBody(1000, 100, TimeUnit.MILLISECONDS)
        })
        server.start()

        val request = Request.Builder()
            .url(server.url("/foo"))
            .post(RequestBody.create(null, data))
            .build()
        val callStateListOrNull = withTimeoutOrNull(200) {
            okHttpClient.callWithCounting(
                request,
                countDownload = false
            ) { response ->
                assertTrue(response.isSuccessful)
                checkNotNull(response.body()).string()
            }.toList()
        }
        server.takeRequest()

        assertNull(callStateListOrNull)
    }

    @Test
    fun testErrorStatusCode() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        server.start()

        val request = Request.Builder().url(server.url("/foo")).build()
        val callStateList = okHttpClient.callWithCounting(request) { response ->
            if (!response.isSuccessful) {
                throw ResponseStatusException(response.code())
            }
            fail("Not reached.")
        }.toList()

        assertEquals(1, callStateList.size)
        callStateList.last().let { lastState ->
            assertTrue(lastState is Failure)
            assertTrue(lastState.exception is ResponseStatusException)
        }
    }

    @Test
    fun testRuntimeException() = runBlocking {
        server.enqueue(MockResponse())
        server.start()

        val request = Request.Builder().url(server.url("/foo")).build()
        assertFailsWith<RuntimeException> {
            okHttpClient.callWithCounting(request) {
                throw RuntimeException("FOO")
            }.toList()
        }.let { exception ->
            assertEquals("FOO", exception.message)
        }
    }

    @Test
    fun testProgressPercentage() {
        assertEquals(50, Uploading(Int.MAX_VALUE.toLong(), Int.MAX_VALUE * 2L).progressPercentage)
        assertEquals(100, Uploading(Long.MAX_VALUE, Long.MAX_VALUE).progressPercentage)
        assertEquals(null, Uploading(1000, -1).progressPercentage)
    }
}

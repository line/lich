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

import com.linecorp.lich.okhttp.WritableResource.Companion.asWritableResource
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResumableDownloadTest {

    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

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
    fun testResumedDownload() = runBlocking {
        val file = tempFolder.newFile().apply {
            writeText("0123456789")
        }

        server.enqueue(MockResponse().apply {
            setResponseCode(StatusCode.PARTIAL_CONTENT)
            setHeader("Content-Range", "bytes 10-15/16")
            setBody("ABCDEF")
        })
        server.start()

        val resource = file.asWritableResource()
        val request = Request.Builder()
            .url(server.url("/foo"))
            .setRangeHeader(resource.length)
            .build()
        val result = okHttpClient.call(request) { response ->
            response.saveToResourceWithSupportingResumption(resource)
        }

        assertEquals(true, result)
        assertEquals("0123456789ABCDEF", file.readText())
    }

    @Test
    fun testResumedDownloadUnknownTotal() = runBlocking {
        val file = tempFolder.newFile().apply {
            writeText("0123456789")
        }

        server.enqueue(MockResponse().apply {
            setResponseCode(StatusCode.PARTIAL_CONTENT)
            setHeader("Content-Range", "bytes 10-15/*")
            setBody("ABCDEF")
        })
        server.start()

        val resource = file.asWritableResource()
        val request = Request.Builder()
            .url(server.url("/foo"))
            .setRangeHeader(resource.length)
            .build()
        val result = okHttpClient.call(request) { response ->
            response.saveToResourceWithSupportingResumption(resource)
        }

        assertEquals(false, result)
        assertEquals("0123456789ABCDEF", file.readText())
    }

    @Test
    fun testAlreadyDownloaded() = runBlocking {
        val file = tempFolder.newFile().apply {
            writeText("0123456789ABCDEF")
        }

        server.enqueue(MockResponse().apply {
            setResponseCode(StatusCode.RANGE_NOT_SATISFIABLE)
            setHeader("Content-Range", "bytes */16")
        })
        server.start()

        val resource = file.asWritableResource()
        val request = Request.Builder()
            .url(server.url("/foo"))
            .setRangeHeader(resource.length)
            .build()
        val result = okHttpClient.call(request) { response ->
            response.saveToResourceWithSupportingResumption(resource)
        }

        assertEquals(true, result)
        assertEquals("0123456789ABCDEF", file.readText())
    }

    @Test
    fun testFallbackEntire() = runBlocking {
        val file = tempFolder.newFile().apply {
            writeText("0123456789")
        }

        server.enqueue(MockResponse().apply {
            setResponseCode(StatusCode.OK)
            setBody("0123456789ABCDEF")
        })
        server.start()

        val resource = file.asWritableResource()
        val request = Request.Builder()
            .url(server.url("/foo"))
            .setRangeHeader(resource.length)
            .build()
        val result = okHttpClient.call(request) { response ->
            response.saveToResourceWithSupportingResumption(resource)
        }

        assertEquals(true, result)
        assertEquals("0123456789ABCDEF", file.readText())
    }

    @Test
    fun testInconsistentPartialResponse() = runBlocking {
        val file = tempFolder.newFile().apply {
            writeText("0123456789")
        }

        server.enqueue(MockResponse().apply {
            setResponseCode(StatusCode.PARTIAL_CONTENT)
            setHeader("Content-Range", "bytes 0-15/16")
            setBody("0123456789ABCDEF")
        })
        server.start()

        val resource = file.asWritableResource()
        val request = Request.Builder()
            .url(server.url("/foo"))
            .setRangeHeader(resource.length)
            .build()

        assertFailsWith<InconsistentContentRangeException> {
            okHttpClient.call(request) { response ->
                response.saveToResourceWithSupportingResumption(resource)
            }
        }

        assertEquals("0123456789", file.readText())
    }

    @Test
    fun testInconsistentUnsatisfiedRangeResponse() = runBlocking {
        val file = tempFolder.newFile().apply {
            writeText("0123456789")
        }

        server.enqueue(MockResponse().apply {
            setResponseCode(StatusCode.RANGE_NOT_SATISFIABLE)
            setHeader("Content-Range", "bytes */16")
        })
        server.start()

        val resource = file.asWritableResource()
        val request = Request.Builder()
            .url(server.url("/foo"))
            .setRangeHeader(resource.length)
            .build()

        assertFailsWith<InconsistentContentRangeException> {
            okHttpClient.call(request) { response ->
                response.saveToResourceWithSupportingResumption(resource)
            }
        }

        assertEquals("0123456789", file.readText())
    }

    @Test
    fun testServiceUnavailable() = runBlocking {
        val file = tempFolder.newFile().apply {
            writeText("0123456789")
        }

        server.enqueue(MockResponse().setResponseCode(503))
        server.start()

        val resource = file.asWritableResource()
        val request = Request.Builder()
            .url(server.url("/foo"))
            .setRangeHeader(resource.length)
            .build()

        assertFailsWith<ResponseStatusException> {
            okHttpClient.call(request) { response ->
                response.saveToResourceWithSupportingResumption(resource)
            }
        }.let { exception ->
            assertEquals(503, exception.code)
        }
    }
}

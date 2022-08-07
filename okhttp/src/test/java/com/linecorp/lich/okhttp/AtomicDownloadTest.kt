/*
 * Copyright 2022 LINE Corporation
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

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AtomicDownloadTest {

    @get:Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    private lateinit var server: MockWebServer

    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        okHttpClient = OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun downloadToNewFile() = runBlocking {
        val fileToSave = File(tempFolder.root, "new_file.txt")
        assertFalse(fileToSave.exists())

        server.enqueue(MockResponse().apply {
            setBody("ABCDEF")
        })
        server.start()

        performDownloadAtomically(fileToSave)

        assertEquals("ABCDEF", fileToSave.readText())
        assertFalse(File(fileToSave.path + ".new").exists())
    }

    @Test
    fun downloadToExistingFile() = runBlocking {
        val fileToSave = tempFolder.newFile().apply {
            writeText("0123456789")
        }
        assertEquals("0123456789", fileToSave.readText())

        server.enqueue(MockResponse().apply {
            setBody("ABCDEF")
        })
        server.start()

        performDownloadAtomically(fileToSave)

        assertEquals("ABCDEF", fileToSave.readText())
        assertFalse(File(fileToSave.path + ".new").exists())
    }

    @Test
    fun failedDueToReadTimeout() = runBlocking {
        val fileToSave = tempFolder.newFile().apply {
            writeText("0123456789")
        }
        assertEquals("0123456789", fileToSave.readText())

        server.enqueue(MockResponse().apply {
            setBody("ABCDEF")
            setBodyDelay(8, TimeUnit.SECONDS)
        })
        server.start()

        assertFailsWith<IOException> {
            performDownloadAtomically(fileToSave)
        }

        assertEquals("0123456789", fileToSave.readText())
        assertFalse(File(fileToSave.path + ".new").exists())
    }

    @Test
    fun coroutineTimeout() = runBlocking {
        val fileToSave = tempFolder.newFile().apply {
            writeText("0123456789")
        }
        assertEquals("0123456789", fileToSave.readText())

        server.enqueue(MockResponse().apply {
            setBody("ABCDEF")
            setBodyDelay(5, TimeUnit.SECONDS)
        })
        server.start()

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(2000L) {
                performDownloadAtomically(fileToSave)
            }
        }
        delay(1000L) // To ensure the tmp file is deleted.

        assertEquals("0123456789", fileToSave.readText())
        assertFalse(File(fileToSave.path + ".new").exists())
    }

    private suspend fun performDownloadAtomically(fileToSave: File) {
        val request = Request.Builder()
            .url(server.url("/foo"))
            .build()
        okHttpClient.call(request) { response ->
            if (response.code != StatusCode.OK) {
                throw ResponseStatusException(response.code)
            }
            response.saveBodyToFileAtomically(fileToSave)
        }
    }
}

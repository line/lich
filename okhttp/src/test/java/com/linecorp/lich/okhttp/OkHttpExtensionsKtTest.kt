package com.linecorp.lich.okhttp

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OkHttpExtensionsKtTest {
    private lateinit var server: MockWebServer
    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        okHttpClient = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun verifyResumeWith() = runBlocking {
        val data = ByteArray(20000) { it.toByte() }

        server.enqueue(MockResponse().apply {
            body = Buffer().write(data)
            throttleBody(1000, 50, TimeUnit.MILLISECONDS)
        })
        server.start()

        val request = Request.Builder().url(server.url("/foo")).build()
        val callStateList = okHttpClient.call(request) { response ->
            assertTrue(response.isSuccessful)
            checkNotNull(response.body()).bytes()
        }.toList()

        assertEquals(callStateList.size, 20000)
    }
}

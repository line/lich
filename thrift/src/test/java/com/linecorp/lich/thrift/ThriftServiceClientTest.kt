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
package com.linecorp.lich.thrift

import com.linecorp.lich.okhttp.ResponseStatusException
import com.linecorp.lich.sample.thrift.FooException
import com.linecorp.lich.sample.thrift.FooParam
import com.linecorp.lich.sample.thrift.FooResponse
import com.linecorp.lich.sample.thrift.FooService
import com.linecorp.lich.sample.thrift.FooUnion
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.apache.thrift.TApplicationException
import org.apache.thrift.TBase
import org.apache.thrift.TSerializable
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.protocol.TMessage
import org.apache.thrift.protocol.TMessageType
import org.apache.thrift.transport.TTransportException
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.ProtocolException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ThriftServiceClientTest {

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
    fun testPing() = runBlocking {
        val result = FooService.ping_result()
        server.enqueue(MockResponse().setThriftResponse("ping", result))
        server.start()

        newClient().ping()

        val args = FooService.ping_args()
        verifyRequest(server.takeRequest(), "ping", args)
    }

    @Test
    fun testPingApplicationError() = runBlocking {
        val exception = TApplicationException(TApplicationException.INTERNAL_ERROR, "ERROR!")
        server.enqueue(MockResponse().setThriftResponse("ping", exception))
        server.start()

        assertFailsWith<TApplicationException> {
            newClient().ping()
        }.let { e ->
            assertEquals(exception.type, e.type)
            assertEquals(exception.message, e.message)
        }
    }

    @Test
    fun testPingServerError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        server.start()

        assertFailsWith<TTransportException> {
            newClient().ping()
        }.let { e ->
            val cause = assertIs<ResponseStatusException>(e.cause)
            assertEquals(500, cause.code)
        }
    }

    @Test
    fun testCallFoo() = runBlocking {
        val fooResponse = FooResponse().apply {
            setNumber(200)
            setMessage("Response")
            setComment("ResponseComment")
        }
        val result = FooService.callFoo_result().setSuccess(fooResponse)
        server.enqueue(MockResponse().setThriftResponse("callFoo", result))
        server.start()

        val fooParam = FooParam().apply {
            setNumber(100)
            setComment("RequestComment")
        }
        val actualResponse = newClient().callFoo(123, "foobar", fooParam)

        assertEquals(fooResponse, actualResponse)

        val args = FooService.callFoo_args(123, "foobar", fooParam)
        verifyRequest(server.takeRequest(), "callFoo", args)
    }

    @Test
    fun testCallFooException() = runBlocking {
        val fooException = FooException("Foo Error!")
        val result = FooService.callFoo_result().setE(fooException)
        server.enqueue(MockResponse().setThriftResponse("callFoo", result))
        server.start()

        val fooParam = FooParam().apply {
            setNumber(100)
            setComment("RequestComment")
        }
        assertFailsWith<FooException> {
            newClient().callFoo(123, "foobar", fooParam)
        }.let { e ->
            assertEquals(fooException, e)
        }
    }

    @Test
    fun testCallFooWithInvalidUnion() = runBlocking {
        server.start()

        val fooParam = FooParam().apply {
            setNumber(100)
            // Serializing a value-less Union causes TProtocolException.
            setFooUnion(FooUnion())
        }
        assertFailsWith<TTransportException> {
            newClient().callFoo(123, "foobar", fooParam)
        }.let { e ->
            assertIs<ProtocolException>(e.cause)
        }

        assertEquals(0, server.requestCount)
    }

    private fun newClient(): FooServiceClient =
        FooServiceClient(okHttpClient, server.url("/foo"))

    private fun MockResponse.setThriftResponse(
        methodName: String,
        result: TSerializable
    ): MockResponse {
        val buffer = Buffer()

        val protocol = TCompactProtocol(OkioTransport(sendSink = buffer))
        val messageType = if (result is TApplicationException) {
            TMessageType.EXCEPTION
        } else {
            TMessageType.REPLY
        }
        protocol.writeMessageBegin(TMessage(methodName, messageType, 1))
        result.write(protocol)
        protocol.writeMessageEnd()

        setBody(buffer)

        return this
    }

    private fun <T : TBase<*, *>> verifyRequest(
        request: RecordedRequest,
        methodName: String,
        expectedArgs: T
    ) {
        assertEquals("POST", request.method)
        assertEquals("/foo", request.path)

        val protocol = TCompactProtocol(OkioTransport(receiveSource = request.body))

        assertEquals(TMessage(methodName, TMessageType.CALL, 1), protocol.readMessageBegin())

        val requestArgs = expectedArgs.deepCopy().also {
            it.clear()
            it.read(protocol)
        }
        assertEquals(expectedArgs, requestArgs)

        protocol.readMessageEnd()
        assertTrue(request.body.exhausted())
    }
}

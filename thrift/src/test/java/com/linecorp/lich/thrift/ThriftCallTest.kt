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
package com.linecorp.lich.thrift

import com.linecorp.lich.sample.thrift.FooException
import com.linecorp.lich.sample.thrift.FooParam
import com.linecorp.lich.sample.thrift.FooResponse
import com.linecorp.lich.sample.thrift.FooService
import com.linecorp.lich.thrift.internal.ReceiveTransport
import com.linecorp.lich.thrift.internal.SendTransport
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
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
import org.apache.thrift.protocol.TProtocolException
import org.apache.thrift.transport.TTransportException
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ThriftCallTest {

    private lateinit var server: MockWebServer

    private lateinit var handler: ThriftCallHandler<FooService.Client>

    private lateinit var okHttpClient: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        handler = Handler(server)
        okHttpClient = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .build()
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

        callPing()

        val args = FooService.ping_args()
        verifyRequest(server.takeRequest(), "ping", args)
    }

    @Test
    fun testPingApplicationError() = runBlocking {
        val exception = TApplicationException(TApplicationException.INTERNAL_ERROR, "ERROR!")
        server.enqueue(MockResponse().setThriftResponse("ping", exception))
        server.start()

        try {
            callPing()
            fail()
        } catch (e: TApplicationException) {
            assertEquals(exception.type, e.type)
            assertEquals(exception.message, e.message)
        }
    }

    @Test
    fun testPingServerError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        server.start()

        try {
            callPing()
            fail()
        } catch (e: TTransportException) {
            assertEquals("HTTP Response code: 500", e.message)
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
        val actualResponse = callFoo(123, "foobar", fooParam)

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
        try {
            callFoo(123, "foobar", fooParam)
            fail()
        } catch (e: FooException) {
            assertEquals(fooException, e)
        }
    }

    @Test
    fun testExceptionOnSend() = runBlocking {
        server.start()

        try {
            okHttpClient.callThrift(handler,
                { throw TProtocolException("Failed to serialize your request.") },
                { recv_callFoo() }
            )
            fail()
        } catch (e: TProtocolException) {
            assertEquals("Failed to serialize your request.", e.message)
        }

        assertEquals(0, server.requestCount)
    }

    private suspend fun callPing(): Unit =
        okHttpClient.callThrift(handler,
            { send_ping() },
            { recv_ping() }
        )

    private suspend fun callFoo(id: Long, name: String, param: FooParam): FooResponse =
        okHttpClient.callThrift(handler,
            { send_callFoo(id, name, param) },
            { recv_callFoo() }
        )

    private fun MockResponse.setThriftResponse(
        methodName: String,
        result: TSerializable
    ): MockResponse {
        val responseBody = Buffer()
        val responseBodyTransport = SendTransport().apply { sink = responseBody }
        val protocol = TCompactProtocol(responseBodyTransport)

        val messageType = if (result is TApplicationException) {
            TMessageType.EXCEPTION
        } else {
            TMessageType.REPLY
        }
        protocol.writeMessageBegin(TMessage(methodName, messageType, 1))
        result.write(protocol)
        protocol.writeMessageEnd()

        responseBodyTransport.flush()
        setBody(responseBody)

        return this
    }

    private fun <T> verifyRequest(
        request: RecordedRequest,
        methodName: String,
        expectedArgs: T
    ) where T : TSerializable, T : TBase<*, *> {
        assertEquals("POST", request.method)
        assertEquals("/foo", request.path)

        val requestBodyTransport = ReceiveTransport().apply { source = request.body }
        val protocol = TCompactProtocol(requestBodyTransport)

        assertEquals(TMessage(methodName, TMessageType.CALL, 1), protocol.readMessageBegin())

        val requestArgs = expectedArgs.deepCopy().apply {
            clear()
            read(protocol)
        }
        assertEquals(expectedArgs, requestArgs)

        protocol.readMessageEnd()
        assertTrue(request.body.exhausted())
    }

    private class Handler(private val server: MockWebServer) :
        AbstractThriftCallHandler<FooService.Client>(FooService.Client.Factory()) {
        override val endpointUrl: HttpUrl
            get() = server.url("/foo")
    }
}

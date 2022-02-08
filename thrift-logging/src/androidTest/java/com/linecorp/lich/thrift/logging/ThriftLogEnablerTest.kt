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
package com.linecorp.lich.thrift.logging

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.sample.thrift.FooException
import com.linecorp.lich.sample.thrift.FooParam
import com.linecorp.lich.sample.thrift.FooResponse
import com.linecorp.lich.sample.thrift.FooService
import com.linecorp.lich.thrift.OkioTransport
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.apache.thrift.TApplicationException
import org.apache.thrift.TSerializable
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.protocol.TMessage
import org.apache.thrift.protocol.TMessageType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
class ThriftLogEnablerTest {

    private lateinit var server: MockWebServer

    private lateinit var okHttpClient: OkHttpClient

    private lateinit var thriftLogger: ThriftLogger

    private lateinit var thriftLogEnabler: ThriftLogEnabler

    @Before
    fun setUp() {
        server = MockWebServer()
        okHttpClient = OkHttpClient()
        thriftLogger = mock()
        thriftLogEnabler =
            ThriftLogEnabler(ApplicationProvider.getApplicationContext(), thriftLogger)
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

        thriftLogger.inOrder {
            verify().logSend(
                "com.linecorp.lich.sample.thrift",
                "FooService",
                "ping",
                FooService.ping_args()
            )
            verify().logReceive(
                "com.linecorp.lich.sample.thrift",
                "FooService",
                "ping",
                result
            )
        }
        verifyNoMoreInteractions(thriftLogger)
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

        thriftLogger.inOrder {
            verify().logSend(
                "com.linecorp.lich.sample.thrift",
                "FooService",
                "ping",
                FooService.ping_args()
            )
        }
        verifyNoMoreInteractions(thriftLogger)
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

        thriftLogger.inOrder {
            verify().logSend(
                "com.linecorp.lich.sample.thrift",
                "FooService",
                "callFoo",
                FooService.callFoo_args(123, "foobar", fooParam)
            )
            verify().logReceive(
                "com.linecorp.lich.sample.thrift",
                "FooService",
                "callFoo",
                result
            )
        }
        verifyNoMoreInteractions(thriftLogger)
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

        thriftLogger.inOrder {
            verify().logSend(
                "com.linecorp.lich.sample.thrift",
                "FooService",
                "callFoo",
                FooService.callFoo_args(123, "foobar", fooParam)
            )
            verify().logReceive(
                "com.linecorp.lich.sample.thrift",
                "FooService",
                "callFoo",
                result
            )
        }
        verifyNoMoreInteractions(thriftLogger)
    }

    private fun newClient(): LoggingFooServiceClient =
        LoggingFooServiceClient(okHttpClient, server.url("/foo"), thriftLogEnabler)

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
}

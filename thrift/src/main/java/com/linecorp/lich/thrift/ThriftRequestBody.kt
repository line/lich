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

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import org.apache.thrift.TServiceClient

/**
 * A [RequestBody] that represents a request of a Thrift call.
 *
 * To receive the response of the Thrift call, use [receiveThriftResponse].
 *
 * @param thriftClientFactory A [ThriftClientFactory] to create a TServiceClient for the Thrift call.
 * @param sendFunction A function that calls `send_METHOD(...)` of the TServiceClient.
 * @see receiveThriftResponse
 */
class ThriftRequestBody<T : TServiceClient>(
    private val thriftClientFactory: ThriftClientFactory<T>,
    private val sendFunction: T.() -> Unit
) : RequestBody() {

    internal val okioTransport: OkioTransport = OkioTransport()

    internal var thriftClient: T? = null

    private var sendBuffer: Buffer? = null

    private fun populateBuffer(): Buffer =
        sendBuffer ?: Buffer().also { buffer ->
            okioTransport.sendSink = buffer
            try {
                thriftClient = thriftClientFactory.newClient(okioTransport).apply(sendFunction)
            } finally {
                okioTransport.sendSink = null
                sendBuffer = buffer
            }
        }

    override fun contentType(): MediaType = mediaTypeThrift

    override fun contentLength(): Long = populateBuffer().size

    override fun writeTo(sink: BufferedSink) {
        // Since readAll() consumes all bytes from the buffer, we copy it first.
        populateBuffer().copy().readAll(sink)
    }

    private companion object {
        private val mediaTypeThrift: MediaType = "application/x-thrift".toMediaType()
    }
}

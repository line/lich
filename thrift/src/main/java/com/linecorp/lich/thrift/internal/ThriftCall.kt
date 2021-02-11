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
package com.linecorp.lich.thrift.internal

import com.linecorp.lich.okhttp.call
import com.linecorp.lich.thrift.ThriftCallHandler
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.BufferedSource
import org.apache.thrift.TServiceClient
import java.io.IOException

/**
 * A class that represents a single call for a Thrift Service over HTTP.
 */
internal class ThriftCall<T : TServiceClient, R>(
    private val okHttpClient: OkHttpClient,
    private val thriftCallHandler: ThriftCallHandler<T>,
    private val sendRequest: T.() -> Unit,
    private val receiveResponse: T.() -> R
) {
    private val sendTransport: SendTransport = SendTransport()

    private val receiveTransport: ReceiveTransport = ReceiveTransport()

    private val serviceClient: T =
        thriftCallHandler.newServiceClient(sendTransport, receiveTransport)

    suspend fun call(): R {
        val request = thriftCallHandler.newRequest(ThriftRequestBody())
        return okHttpClient.call(request, ::handleResponse)
    }

    private fun handleResponse(response: Response): R {
        thriftCallHandler.throwExceptionIfError(response)
        val source = response.body?.source() ?: throw IOException("No response body.")
        return readResponseFrom(source)
    }

    private fun readResponseFrom(source: BufferedSource): R {
        try {
            receiveTransport.source = source
            return serviceClient.receiveResponse()
        } finally {
            receiveTransport.source = null
        }
    }

    private fun writeRequestTo(sink: BufferedSink) {
        try {
            sendTransport.sink = sink
            serviceClient.sendRequest()
        } catch (e: IOException) {
            throw e
        } catch (e: Throwable) {
            // Since RequestBody.contentLength() and RequestBody.writeTo() can throw IOExceptions only,
            // other Exceptions thrown from `serviceClient.sendRequest()` should be wrapped with
            // SendRequestException.
            throw SendRequestException(e)
        } finally {
            sendTransport.sink = null
        }
    }

    private inner class ThriftRequestBody : RequestBody() {
        /**
         * The content of the request body.
         *
         * This is lazily populated when [contentLength] or [writeTo] is first called so that
         * the operation is performed on OkHttp's background threads.
         */
        private var buffer: Buffer? = null

        private fun populateBuffer(): Buffer =
            buffer ?: Buffer().also {
                writeRequestTo(it)
                buffer = it
            }

        override fun contentType(): MediaType =
            MEDIA_TYPE_THRIFT

        override fun contentLength(): Long =
            populateBuffer().size

        override fun writeTo(sink: BufferedSink) {
            // Since readAll() consumes all bytes from the source, we clone it first.
            populateBuffer().clone().readAll(sink)
        }
    }

    companion object {
        private val MEDIA_TYPE_THRIFT: MediaType = "application/x-thrift".toMediaType()
    }
}

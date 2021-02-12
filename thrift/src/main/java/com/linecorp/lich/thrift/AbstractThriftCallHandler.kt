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

import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.apache.thrift.TServiceClient
import org.apache.thrift.TServiceClientFactory
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.protocol.TProtocolFactory
import org.apache.thrift.transport.TTransport
import org.apache.thrift.transport.TTransportException

/**
 * Skeleton of [ThriftCallHandler].
 *
 * This is an example of a `ThriftCallHandler` instance.
 * ```
 * class MyThriftCallHandler<T : TServiceClient>(
 *     serviceClientFactory: TServiceClientFactory<T>,
 *     endpointPath: String
 * ) : AbstractThriftCallHandler<T>(serviceClientFactory) {
 *
 *     override val endpointUrl: HttpUrl = endpointUrlBase.resolve(endpointPath)
 *         ?: throw IllegalArgumentException("Invalid path: $endpointPath")
 *
 *     companion object {
 *         val endpointUrlBase: HttpUrl = HttpUrl.get("https://api.example.com")
 *     }
 * }
 *
 * val fooServiceCallHandler: ThriftCallHandler<FooService.Client> =
 *     MyThriftCallHandler(FooService.Client.Factory(), "/foo")
 * ```
 *
 * @see okhttp3.OkHttpClient.callThrift
 */
abstract class AbstractThriftCallHandler<T : TServiceClient>(
    private val serviceClientFactory: TServiceClientFactory<T>,
    private val sendProtocolFactory: TProtocolFactory = defaultProtocolFactory,
    private val receiveProtocolFactory: TProtocolFactory = defaultProtocolFactory
) : ThriftCallHandler<T> {
    /**
     * Returns a new instance of [T] using the specified send / receive transports.
     *
     * This implementation creates send / receive transports from [sendProtocolFactory] /
     * [receiveProtocolFactory], then returns a TServiceClient created from the protocols and
     * [serviceClientFactory].
     */
    override fun newServiceClient(sendTransport: TTransport, receiveTransport: TTransport): T {
        val sendProtocol = sendProtocolFactory.getProtocol(sendTransport)
        val receiveProtocol = receiveProtocolFactory.getProtocol(receiveTransport)
        return serviceClientFactory.getClient(receiveProtocol, sendProtocol)
    }

    /**
     * Returns a new instance of [Request] using the specified [requestBody].
     *
     * This implementation creates a HTTP POST request for the [endpointUrl].
     * You can configure the request object by overriding [onPrepareRequest].
     */
    override fun newRequest(requestBody: RequestBody): Request {
        val builder = Request.Builder().url(endpointUrl).post(requestBody)
        onPrepareRequest(builder)
        return builder.build()
    }

    /**
     * The URL for the endpoint of the Thrift remote service.
     */
    protected abstract val endpointUrl: HttpUrl

    /**
     * A function called from [newRequest].
     *
     * You can configure the HTTP request like this:
     * ```
     * override fun onPrepareRequest(builder: Request.Builder) {
     *     builder.addHeader("X-Foo", "foobar")
     * }
     * ```
     */
    protected open fun onPrepareRequest(builder: Request.Builder) {
    }

    /**
     * Checks the status code of [response] and throws an Exception if the response did not succeed.
     *
     * This implementation throws a [TTransportException] if the status code of [response] is not
     * HTTP OK (200).
     */
    override fun throwExceptionIfError(response: Response) {
        val responseCode = response.code
        if (responseCode != HTTP_STATUS_OK) {
            throw TTransportException("HTTP Response code: $responseCode")
        }
    }

    companion object {
        /**
         * HTTP status code: OK(200)
         */
        const val HTTP_STATUS_OK: Int = 200

        /**
         * The default [TProtocolFactory].
         * Currently, this is [TCompactProtocol.Factory].
         */
        val defaultProtocolFactory: TProtocolFactory = TCompactProtocol.Factory()
    }
}

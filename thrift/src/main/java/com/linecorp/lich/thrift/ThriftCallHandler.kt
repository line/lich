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

import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.apache.thrift.TServiceClient
import org.apache.thrift.transport.TTransport

/**
 * A handler responsible for creating instances of [TServiceClient] and handling HTTP requests
 * for Thrift Service calls.
 *
 * @see AbstractThriftCallHandler
 */
interface ThriftCallHandler<T : TServiceClient> {
    /**
     * Returns a new instance of [T] using the specified send / receive transports.
     *
     * @param sendTransport a [TTransport] for sending (output).
     * @param receiveTransport a [TTransport] for receiving (input).
     * @return a brand new instance of [T].
     */
    fun newServiceClient(sendTransport: TTransport, receiveTransport: TTransport): T

    /**
     * Returns a new instance of [Request] using the specified request body.
     * Usually, it is a "POST" request to the service endpoint.
     *
     * @param requestBody a [RequestBody] to be sent.
     * @return a new instance of [Request].
     */
    fun newRequest(requestBody: RequestBody): Request

    /**
     * Checks the status code of [response] and throws an Exception if the response did not succeed.
     * The Exception thrown from this function will be delivered to [okhttp3.OkHttpClient.callThrift].
     */
    fun throwExceptionIfError(response: Response)
}

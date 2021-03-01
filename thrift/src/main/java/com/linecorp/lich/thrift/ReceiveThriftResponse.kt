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

import okhttp3.Response
import org.apache.thrift.TServiceClient

/**
 * Receives the response of a Thrift call that was sent using [ThriftRequestBody].
 *
 * If any I/O error occurs, this function throws [java.io.IOException] rather than
 * [org.apache.thrift.transport.TTransportException].
 *
 * @param receiveFunction A function that calls `recv_METHOD(...)` of the TServiceClient.
 * @return The result of [receiveFunction].
 * @throws java.io.IOException If any I/O error occurred.
 * @see ThriftRequestBody
 */
fun <T : TServiceClient, R> Response.receiveThriftResponse(receiveFunction: T.() -> R): R {
    val responseBody = checkNotNull(body) { "This response has no body." }

    @Suppress("UNCHECKED_CAST")
    val thriftRequestBody = checkNotNull(request.body as? ThriftRequestBody<T>) {
        "The request body is not a ThriftRequestBody."
    }
    val thriftClient = checkNotNull(thriftRequestBody.thriftClient) {
        "The request has not been sent yet."
    }
    val transport = thriftRequestBody.okioTransport

    transport.receiveSource = responseBody.source()
    try {
        return thriftClient.receiveFunction()
    } finally {
        transport.receiveSource = null
    }
}

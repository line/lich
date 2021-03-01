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
import com.linecorp.lich.okhttp.StatusCode
import com.linecorp.lich.okhttp.call
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.thrift.TServiceClient
import org.apache.thrift.transport.TTransportException
import java.io.IOException

/**
 * An abstract template class for building client classes of Thrift services.
 *
 * An example of calling a Thrift API `FooService.callFoo(id, name, param)` is as follows:
 * ```
 * class FooServiceClient(
 *     override val okHttpClient: OkHttpClient,
 *     override val endpointUrl: HttpUrl
 * ) : AbstractThriftServiceClient<FooService.Client>() {
 *
 *     override val thriftClientFactory: ThriftClientFactory<FooService.Client> =
 *         ThriftClientFactory(FooService.Client.Factory())
 *
 *     suspend fun callFoo(id: Long, name: String, param: FooParam): FooResponse =
 *         call({ send_callFoo(id, name, param) }, { recv_callFoo() })
 * }
 * ```
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractThriftServiceClient<T : TServiceClient> {
    /**
     * The [OkHttpClient] for calling Thrift services.
     */
    protected abstract val okHttpClient: OkHttpClient

    /**
     * The endpoint URL of the Thrift service.
     */
    protected abstract val endpointUrl: HttpUrl

    /**
     * The factory of [TServiceClient] for the Thrift service.
     */
    protected abstract val thriftClientFactory: ThriftClientFactory<T>

    /**
     * Creates a new [ThriftRequestBody] with the given [sendFunction].
     *
     * @param sendFunction A function that calls `send_METHOD(...)` of the TServiceClient.
     */
    protected fun newRequestBody(sendFunction: T.() -> Unit): ThriftRequestBody<T> =
        ThriftRequestBody(thriftClientFactory, sendFunction)

    /**
     * Builds a new OkHttp [Request] for a Thrift call.
     *
     * By default, this function builds a HTTP `POST` request to [endpointUrl] with a body created
     * by [newRequestBody].
     *
     * @param sendFunction A function that calls `send_METHOD(...)` of the TServiceClient.
     */
    protected open fun newRequest(sendFunction: T.() -> Unit): Request =
        Request.Builder()
            .url(endpointUrl)
            .post(newRequestBody(sendFunction))
            .build()

    /**
     * Receives the response of a Thrift call.
     *
     * By default, this function checks the status code of the response and throws
     * [ResponseStatusException] if it is not `200 OK`. Otherwise, this function parses the response
     * body using [Response.receiveThriftResponse].
     *
     * @param receiveFunction A function that calls `recv_METHOD(...)` of the TServiceClient.
     * @return The result of [receiveFunction].
     * @throws IOException If any I/O error occurred.
     */
    protected open fun <R> Response.handleResponse(receiveFunction: T.() -> R): R {
        if (code != StatusCode.OK) {
            throw ResponseStatusException(code)
        }
        return receiveThriftResponse(receiveFunction)
    }

    /**
     * Makes a Thrift call with the given [request] and [receiveFunction].
     *
     * If any I/O error occurs, this function throws a [TTransportException].
     *
     * @param request A OkHttp [Request] for the Thrift call. Must have a [ThriftRequestBody].
     * @param receiveFunction A function that calls `recv_METHOD(...)` of the TServiceClient.
     * @return The result of [receiveFunction].
     * @throws TTransportException If any I/O error occurred.
     */
    protected suspend fun <R> doCall(request: Request, receiveFunction: T.() -> R): R =
        try {
            okHttpClient.call(request) { response ->
                response.handleResponse(receiveFunction)
            }
        } catch (e: IOException) {
            throw TTransportException(e)
        }

    /**
     * Makes a Thrift call with the given [sendFunction] and [receiveFunction].
     *
     * If any I/O error occurs, this function throws a [TTransportException].
     *
     * @param sendFunction A function that calls `send_METHOD(...)` of the TServiceClient.
     * @param receiveFunction A function that calls `recv_METHOD(...)` of the TServiceClient.
     * @return The result of [receiveFunction].
     * @throws TTransportException If any I/O error occurred.
     */
    protected suspend fun <R> call(sendFunction: T.() -> Unit, receiveFunction: T.() -> R): R =
        doCall(newRequest(sendFunction), receiveFunction)
}

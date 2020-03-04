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

import com.linecorp.lich.thrift.internal.SendRequestException
import com.linecorp.lich.thrift.internal.ThriftCall
import okhttp3.OkHttpClient
import org.apache.thrift.TException
import org.apache.thrift.TServiceClient
import org.apache.thrift.transport.TTransportException
import java.io.IOException

/**
 * Makes a call to a Thrift Service.
 *
 * The transport protocol of this function is compatible with [org.apache.thrift.transport.THttpClient].
 * Any IOExceptions will be thrown as [TTransportException]s.
 *
 * Suppose you have a service defined as below.
 * ```
 * // FooService.thrift
 * service FooService {
 *   FooResponse callFoo(1:i64 id, 2:string name, 3:FooParam param) throws (1:FooException e)
 * }
 * ```
 *
 * The code for invoking this service is as follows.
 * ```
 * class FooServiceClient(private val okHttpClient: OkHttpClient) {
 *     // See AbstractThriftCallHandler.
 *     private val handler: ThriftCallHandler<FooService.Client> = ...
 *
 *     suspend fun callFoo(id: Long, name: String, param: FooParam): FooResponse =
 *         okHttpClient.callThrift(handler,
 *             { send_callFoo(id, name, param) },
 *             { recv_callFoo() }
 *         )
 * }
 * ```
 *
 * @param T the `Client` class of the target Thrift Service.
 * @param R the return type of the calling method.
 * @param thriftCallHandler a handler that handles a Thrift call over HTTP.
 * @param sendRequest a function that sends request parameters to the remote host. This function
 * have to call `send_METHOD(...)` of the Service Client.
 * @param receiveResponse a function that receives a response from the remote host. This function
 * have to call `recv_METHOD()` of the Service Client.
 * @throws TException
 * @see AbstractThriftCallHandler
 */
suspend fun <T : TServiceClient, R> OkHttpClient.callThrift(
    thriftCallHandler: ThriftCallHandler<T>,
    sendRequest: T.() -> Unit,
    receiveResponse: T.() -> R
): R = try {
    ThriftCall(this, thriftCallHandler, sendRequest, receiveResponse).call()
} catch (e: SendRequestException) {
    throw e.source
} catch (e: IOException) {
    throw TTransportException(e)
}

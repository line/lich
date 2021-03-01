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

import org.apache.thrift.TServiceClient
import org.apache.thrift.TServiceClientFactory
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.protocol.TProtocolFactory
import org.apache.thrift.transport.TTransport

/**
 * A factory that creates new [TServiceClient] instances.
 */
fun interface ThriftClientFactory<T : TServiceClient> {
    /**
     * Creates a new [TServiceClient] instance that uses the given [transport].
     */
    fun newClient(transport: TTransport): T
}

/**
 * Creates a new instance of [ThriftClientFactory] from the given [TServiceClientFactory] and
 * [TProtocolFactory].
 *
 * @param serviceClientFactory A [TServiceClientFactory] to create a `TServiceClient`.
 * @param protocolFactory A [TProtocolFactory] to create a `TProtocol`. If not specified,
 * [TCompactProtocol.Factory] will be used.
 */
@Suppress("FunctionName")
fun <T : TServiceClient> ThriftClientFactory(
    serviceClientFactory: TServiceClientFactory<T>,
    protocolFactory: TProtocolFactory = TCompactProtocol.Factory()
): ThriftClientFactory<T> = ThriftClientFactory { transport ->
    serviceClientFactory.getClient(protocolFactory.getProtocol(transport))
}

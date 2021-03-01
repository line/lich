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

import okio.Buffer
import okio.BufferedSource
import org.apache.thrift.TBase
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.protocol.TProtocolFactory

/**
 * Utilities to convert between Thrift objects and Okio Buffers.
 */
object OkioThriftUtil {
    /**
     * Serializes a Thrift object into a [Buffer].
     *
     * @param target The object to serialize.
     * @param protocolFactory A [TProtocolFactory] used for serialization. If not specified,
     * [TCompactProtocol.Factory] will be used.
     * @return Serialized object in a [Buffer].
     */
    fun <T : TBase<*, *>> serialize(
        target: T,
        protocolFactory: TProtocolFactory = TCompactProtocol.Factory()
    ): Buffer = Buffer().also { buffer ->
        target.write(protocolFactory.getProtocol(OkioTransport(sendSink = buffer)))
    }

    /**
     * Deserializes a Thrift object from a [BufferedSource].
     *
     * @param source A [BufferedSource] to read from.
     * @param target An empty object of [T]. This should be `T()`.
     * @param protocolFactory A [TProtocolFactory] used for deserialization. If not specified,
     * [TCompactProtocol.Factory] will be used.
     * @return Deserialized object.
     */
    fun <T : TBase<*, *>> deserialize(
        source: BufferedSource,
        target: T,
        protocolFactory: TProtocolFactory = TCompactProtocol.Factory()
    ): T {
        target.read(protocolFactory.getProtocol(OkioTransport(receiveSource = source)))
        return target
    }
}

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

import okio.BufferedSource
import org.apache.thrift.transport.TTransport
import org.apache.thrift.transport.TTransportException

/**
 * A [TTransport] for deserializing a Thrift call response.
 */
internal class ReceiveTransport : TTransport() {

    var source: BufferedSource? = null

    private fun requireSource(): BufferedSource =
        source ?: throw IllegalStateException("This transport is not currently open.")

    override fun read(buf: ByteArray, off: Int, len: Int): Int {
        val bytesRead = requireSource().read(buf, off, len)
        if (bytesRead < 0) {
            throw TTransportException(TTransportException.END_OF_FILE)
        }
        return bytesRead
    }

    override fun isOpen(): Boolean {
        return source != null
    }

    override fun open() {
    }

    override fun close() {
    }

    override fun write(buf: ByteArray, off: Int, len: Int) {
        throw UnsupportedOperationException("This transport is read-only.")
    }
}

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

import okio.BufferedSink
import org.apache.thrift.transport.TTransport

/**
 * A [TTransport] for serializing a Thrift call request.
 */
internal class SendTransport : TTransport() {

    var sink: BufferedSink? = null

    private fun requireSink(): BufferedSink =
        sink ?: throw IllegalStateException("This transport is not currently open.")

    override fun write(buf: ByteArray, off: Int, len: Int) {
        requireSink().write(buf, off, len)
    }

    override fun flush() {
        requireSink().flush()
    }

    override fun isOpen(): Boolean {
        return sink != null
    }

    override fun open() {
    }

    override fun close() {
    }

    override fun read(buf: ByteArray, off: Int, len: Int): Int {
        throw UnsupportedOperationException("This transport is write-only.")
    }
}

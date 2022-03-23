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

import okio.BufferedSink
import okio.BufferedSource
import org.apache.thrift.TConfiguration
import org.apache.thrift.transport.TTransport
import java.io.EOFException

/**
 * An implementation of [TTransport] using [Okio](https://square.github.io/okio/).
 *
 * If any I/O error occurs, this transport throws [java.io.IOException] rather than
 * [org.apache.thrift.transport.TTransportException].
 *
 * @property sendSink A [BufferedSink] used for sending. This must be set when performing any write
 * operations.
 * @property receiveSource A [BufferedSource] used for receiving. This must be set when performing
 * any read operations.
 */
class OkioTransport(
    var sendSink: BufferedSink? = null,
    var receiveSource: BufferedSource? = null
) : TTransport() {

    private val checkedSendSink: BufferedSink
        get() = sendSink ?: throw IllegalStateException("sendSink is not set.")

    private val checkedReceiveSource: BufferedSource
        get() = receiveSource ?: throw IllegalStateException("receiveSource is not set.")

    override fun write(buf: ByteArray, off: Int, len: Int) {
        checkedSendSink.write(buf, off, len)
    }

    override fun flush() {
        checkedSendSink.flush()
    }

    override fun read(buf: ByteArray, off: Int, len: Int): Int =
        checkedReceiveSource.read(buf, off, len)

    override fun readAll(buf: ByteArray, off: Int, len: Int): Int {
        val source = checkedReceiveSource
        var position = off
        var remaining = len
        while (remaining != 0) {
            val read = source.read(buf, position, remaining)
            if (read < 0) throw EOFException()
            position += read
            remaining -= read
        }
        return len
    }

    override fun isOpen(): Boolean = true

    override fun open() = Unit

    override fun close() = Unit

    // To keep binary compatibility with libthrift 0.13.0 or lower,
    // we provide empty implementations for the following methods.
    override fun getConfiguration(): TConfiguration = TConfiguration.DEFAULT

    override fun checkReadBytesAvailable(numBytes: Long) = Unit

    override fun updateKnownMessageSize(size: Long) = Unit
}

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
package com.linecorp.lich.okhttp

import okio.Sink
import okio.sink
import java.io.File

/**
 * A resource that supports writing to it.
 */
interface WritableResource {
    /**
     * The byte length of this resource.
     * If the substance for this resource does not exist, this value is `0`.
     */
    val length: Long

    /**
     * Opens a new [Sink] that writes to this resource.
     * If the substance for this resource does not exist, it will be created.
     *
     * @param append If `true`, data will be written to the end of the resource rather than
     * the beginning.
     */
    fun newSink(append: Boolean): Sink

    companion object {
        /**
         * Returns a [WritableResource] that represents the given File.
         */
        fun File.asWritableResource(): WritableResource = FileWritableResource(this)

        private class FileWritableResource(private val file: File) : WritableResource {
            override val length: Long
                get() = file.length()

            override fun newSink(append: Boolean): Sink = file.sink(append)
        }
    }
}

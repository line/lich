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

import com.linecorp.lich.sample.thrift.FooParam
import com.linecorp.lich.sample.thrift.FooResponse
import com.linecorp.lich.sample.thrift.FooUnion
import org.apache.thrift.protocol.TProtocolException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OkioThriftUtilTest {

    @Test
    fun serializeAndDeserialize() {
        val fooResponse = FooResponse().apply {
            setNumber(200)
            setMessage("Response")
            setComment("ResponseComment")
        }

        val buffer = OkioThriftUtil.serialize(fooResponse)
        assertEquals(31, buffer.size)

        val deserialized = OkioThriftUtil.deserialize(buffer, FooResponse())
        assertEquals(fooResponse, deserialized)
        assertEquals(0, buffer.size)
    }

    @Test
    fun serializeInvalidUnion() {
        val fooParam = FooParam().apply {
            setNumber(100)
            // Serializing a value-less Union causes TProtocolException.
            setFooUnion(FooUnion())
        }

        assertFailsWith<TProtocolException> {
            OkioThriftUtil.serialize(fooParam)
        }
    }
}

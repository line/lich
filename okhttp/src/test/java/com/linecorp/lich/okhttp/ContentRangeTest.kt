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

import com.linecorp.lich.okhttp.ContentRange.Companion.mayGetSinglePartContentRange
import com.linecorp.lich.okhttp.ContentRange.Companion.mayGetTotalLengthOfUnsatisfiedRange
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ContentRangeTest {

    @Test
    fun testContentRange() {
        val request = Request.Builder()
            .url("http://example.com/foo")
            .setRangeHeader(10000)
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(StatusCode.PARTIAL_CONTENT)
            .message("Partial Content")
            .addHeader("Content-Range", "bytes 10000-19999/20000")
            .body("BODY".toResponseBody())
            .build()

        val contentRange = response.mayGetSinglePartContentRange()

        assertEquals("bytes=10000-", request.header("Range"))
        assertNotNull(contentRange)
        assertEquals(10000, contentRange.start)
        assertEquals(19999, contentRange.endInclusive)
        assertEquals(20000, contentRange.totalLength)
        assertEquals(true, contentRange.isLastPart)
        assertEquals("10000-19999/20000", contentRange.toString())
    }

    @Test
    fun testContentRangeUnknown() {
        val request = Request.Builder()
            .url("http://example.com/foo")
            .setRangeHeader(10000)
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(StatusCode.PARTIAL_CONTENT)
            .message("Partial Content")
            .addHeader("Content-Range", "bytes 10000-19999/*")
            .body("BODY".toResponseBody())
            .build()

        val contentRange = response.mayGetSinglePartContentRange()

        assertEquals("bytes=10000-", request.header("Range"))
        assertNotNull(contentRange)
        assertEquals(10000, contentRange.start)
        assertEquals(19999, contentRange.endInclusive)
        assertEquals(-1, contentRange.totalLength)
        assertEquals(false, contentRange.isLastPart)
        assertEquals("10000-19999/*", contentRange.toString())
    }

    @Test
    fun testContentRangeMiddle() {
        val request = Request.Builder()
            .url("http://example.com/foo")
            .setRangeHeader(10000, 14999)
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(StatusCode.PARTIAL_CONTENT)
            .message("Partial Content")
            .addHeader("Content-Range", "bytes 10000-14999/20000")
            .body("BODY".toResponseBody())
            .build()

        val contentRange = response.mayGetSinglePartContentRange()

        assertEquals("bytes=10000-14999", request.header("Range"))
        assertNotNull(contentRange)
        assertEquals(10000, contentRange.start)
        assertEquals(14999, contentRange.endInclusive)
        assertEquals(20000, contentRange.totalLength)
        assertEquals(false, contentRange.isLastPart)
        assertEquals("10000-14999/20000", contentRange.toString())
    }

    @Test
    fun testContentRangeTail() {
        val request = Request.Builder()
            .url("http://example.com/foo")
            .setSuffixRangeHeader(5000)
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(StatusCode.PARTIAL_CONTENT)
            .message("Partial Content")
            .addHeader("Content-Range", "bytes 15000-19999/20000")
            .body("BODY".toResponseBody())
            .build()

        val contentRange = response.mayGetSinglePartContentRange()

        assertEquals("bytes=-5000", request.header("Range"))
        assertNotNull(contentRange)
        assertEquals(15000, contentRange.start)
        assertEquals(19999, contentRange.endInclusive)
        assertEquals(20000, contentRange.totalLength)
        assertEquals(true, contentRange.isLastPart)
        assertEquals("15000-19999/20000", contentRange.toString())
    }

    @Test
    fun testUnsatisfiedRange() {
        val request = Request.Builder()
            .url("http://example.com/foo")
            .setRangeHeader(20000)
            .build()
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(StatusCode.RANGE_NOT_SATISFIABLE)
            .message("Range Not Satisfiable")
            .addHeader("Content-Range", "bytes */20000")
            .body("".toResponseBody())
            .build()

        val totalLength = response.mayGetTotalLengthOfUnsatisfiedRange()

        assertEquals("bytes=20000-", request.header("Range"))
        assertEquals(20000, totalLength)
    }
}

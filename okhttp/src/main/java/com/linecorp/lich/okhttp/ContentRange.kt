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

import okhttp3.Response

/**
 * A *valid* value of a `Content-Range` header field.
 *
 * Specification: [RFC 7233, section 4.2](https://tools.ietf.org/html/rfc7233#section-4.2)
 *
 * @property start the offset of the first byte in the content range.
 * @property endInclusive the offset of the last byte in the content range.
 * @property totalLength the total length of the document, or `-1` if it is unknown ('*').
 * @see Response.saveToResourceWithSupportingResumption
 */
class ContentRange(val start: Long, val endInclusive: Long, val totalLength: Long) {
    /**
     * `true` if this content range is at the end of the document.
     *
     * If the total length of the document is unknown, this is `false`.
     */
    val isLastPart: Boolean
        get() = endInclusive + 1 == totalLength

    override fun toString(): String =
        "$start-$endInclusive/${if (totalLength < 0) "*" else totalLength.toString()}"

    companion object {
        /**
         * Returns the [ContentRange] of a `206 Partial Content` response transferring a single part.
         * If the response doesn't have a valid `Content-Range` header field, returns `null`.
         *
         * Specification: [RFC 7233, section 4.1](https://tools.ietf.org/html/rfc7233#section-4.1)
         */
        fun Response.mayGetSinglePartContentRange(): ContentRange? {
            // A single-part partial content response must have a Content-Range header field.
            val headerValue = header("Content-Range") ?: return null

            // Parse the Content-Range header value.
            // cf. https://tools.ietf.org/html/rfc7233#section-4.2
            val result = validRangeRegex.matchEntire(headerValue) ?: return null
            val start = result.groupValues[1].toLongOrNull()?.takeIf { it >= 0 } ?: return null
            val end = result.groupValues[2].toLongOrNull()?.takeIf { it >= start } ?: return null
            val totalLength = when (val total = result.groupValues[3]) {
                "*" -> -1
                else -> total.toLongOrNull()?.takeIf { it > end } ?: return null
            }
            return ContentRange(start, end, totalLength)
        }

        /**
         * Returns the length of the document specified in the `Content-Range` header field of
         * a `416 Range Not Satisfiable` response.
         * If the response doesn't have an unsatisfied `Content-Range` header field, returns `null`.
         *
         * Specification: [RFC 7233, section 4.4](https://tools.ietf.org/html/rfc7233#section-4.4)
         */
        fun Response.mayGetTotalLengthOfUnsatisfiedRange(): Long? {
            // A "416 Range Not Satisfiable" response should have a Content-Range header field
            // with an unsatisfied-range value.
            val headerValue = header("Content-Range") ?: return null

            // Parse the Content-Range header value.
            // cf. https://tools.ietf.org/html/rfc7233#section-4.2
            val result = unsatisfiedRangeRegex.matchEntire(headerValue) ?: return null
            return result.groupValues[1].toLongOrNull()?.takeIf { it >= 0 }
        }

        private val validRangeRegex: Regex = Regex("""bytes (\d+)-(\d+)/(\d+|\*)""")
        private val unsatisfiedRangeRegex: Regex = Regex("""bytes \*/(\d+)""")
    }
}

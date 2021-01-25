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
import okhttp3.Request
import okhttp3.Response
import okio.Sink
import java.io.IOException

/**
 * Saves the response body to the given [WritableResource] with handling a single-part partial
 * response defined in [RFC 7233](https://tools.ietf.org/html/rfc7233).
 *
 * This is a sample code that performs a *resumable download* using Range requests defined in
 * RFC 7233.
 * ```
 * suspend fun performResumableDownload(url: HttpUrl, fileToSave: File) {
 *     val resourceToSave = fileToSave.asWritableResource()
 *     val request = Request.Builder()
 *         .url(url)
 *         .setRangeHeader(resourceToSave.length)
 *         .build()
 *     okHttpClient.callWithCounting(request) { response ->
 *         response.saveToResourceWithSupportingResumption(resourceToSave)
 *     }.collect { state ->
 *         when (state) {
 *             is Uploading -> Unit
 *             is Downloading ->
 *                 println("Downloading: ${state.bytesTransferred} bytes downloaded." +
 *                     state.progressPercentage?.let { " ($it%)" }.orEmpty())
 *             is Success ->
 *                 if (state.data)
 *                     println("The download is complete. TotalLength=${state.bytesDownloaded}")
 *                 else
 *                     println("The HTTP call was successful, but the content may have a remaining part.\n" +
 *                         "To complete the download, call this function again.")
 *             is Failure ->
 *                 println("Failure: ${state.exception}\nTo resume the download, call this function again.")
 *         }
 *     }
 * }
 * ```
 *
 * @param resourceToSave the resource to save the downloaded content.
 * @return `true` if the download of the entire content is completed. `false` if the response is
 * `206 Partial Content` and there may be remaining undownloaded content.
 * @throws InconsistentContentRangeException if the response code is `206 Partial Content` or
 * `416 Range Not Satisfiable`, and the `Content-Range` header value does not match the length of
 * [resourceToSave].
 * @throws ResponseStatusException if the response code is neither `200 OK` nor
 * `206 Partial Content` nor `416 Range Not Satisfiable`.
 * @throws IOException if any other I/O error occurs.
 */
fun Response.saveToResourceWithSupportingResumption(resourceToSave: WritableResource): Boolean {
    val sink: Sink
    val isLastPart: Boolean

    when (code()) {
        StatusCode.OK -> {
            // The response body contains the entire content.
            sink = resourceToSave.newSink(append = false)
            isLastPart = true
        }
        StatusCode.PARTIAL_CONTENT -> {
            val contentRange = mayGetSinglePartContentRange()
                ?: throw InconsistentContentRangeException("No valid Content-Range header in the `Partial Content` response.")
            if (contentRange.start != resourceToSave.length) {
                throw InconsistentContentRangeException("The start offset of the partial response does not match the length of the resource.")
            }
            // The response body contains the continuation of the resource.
            sink = resourceToSave.newSink(append = true)
            isLastPart = contentRange.isLastPart
        }
        StatusCode.RANGE_NOT_SATISFIABLE -> {
            val totalLength = mayGetTotalLengthOfUnsatisfiedRange()
                ?: throw InconsistentContentRangeException("No unsatisfied Content-Range header in the `Range Not Satisfiable` response.")
            if (totalLength != resourceToSave.length) {
                throw InconsistentContentRangeException("The content length of the `Range Not Satisfiable` response does not match the length of the resource.")
            }
            // The entire content was already downloaded to the resource.
            return true
        }
        else -> throw ResponseStatusException(code())
    }

    sink.use { checkNotNull(body()).source().readAll(it) }

    return isLastPart
}

/**
 * Sets a single-part `Range` header with the given [start] offset.
 *
 * Specification: [RFC 7233, section 3.1](https://tools.ietf.org/html/rfc7233#section-3.1)
 */
fun Request.Builder.setRangeHeader(start: Long): Request.Builder {
    require(start >= 0) { "The start position must not be less than zero." }
    return header("Range", "bytes=$start-")
}

/**
 * Sets a single-part `Range` header with the given [start] and [endInclusive] offset.
 *
 * Specification: [RFC 7233, section 3.1](https://tools.ietf.org/html/rfc7233#section-3.1)
 */
fun Request.Builder.setRangeHeader(start: Long, endInclusive: Long): Request.Builder {
    require(start >= 0) { "The start position must not be less than zero." }
    require(endInclusive >= start) { "The end position must not be less than the start position." }
    return header("Range", "bytes=$start-$endInclusive")
}

/**
 * Sets a single-part `Range` header with the given [suffixLength].
 *
 * Specification: [RFC 7233, section 3.1](https://tools.ietf.org/html/rfc7233#section-3.1)
 */
fun Request.Builder.setSuffixRangeHeader(suffixLength: Long): Request.Builder {
    require(suffixLength > 0) { "The suffix length must be greater than zero." }
    return header("Range", "bytes=-$suffixLength")
}

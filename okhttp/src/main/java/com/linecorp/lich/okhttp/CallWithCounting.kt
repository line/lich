/*
 * Copyright 2020 LINE Corporation
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
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onEach
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.ForwardingSource
import okio.Sink
import okio.Source
import okio.buffer
import java.io.IOException

/**
 * Creates a [Flow] that executes an HTTP call with counting the number of bytes transferred in its
 * request and response body. Through the [Flow], you can see the progress of the HTTP call.
 *
 * This is a sample code that sends the content of `fileToUpload` as an HTTP POST method.
 * ```
 * suspend fun performUpload(url: HttpUrl, fileToUpload: File) {
 *     val request = Request.Builder()
 *         .url(url)
 *         .post(fileToUpload.asRequestBody("application/octet-stream".toMediaType()))
 *         .build()
 *     okHttpClient.callWithCounting(request, countDownload = false) { response ->
 *         if (!response.isSuccessful) {
 *             throw ResponseStatusException(response.code)
 *         }
 *     }.collect { state ->
 *         when (state) {
 *             is Uploading ->
 *                 println("Uploading: ${state.bytesTransferred} bytes sent." +
 *                     state.progressPercentage?.let { " ($it%)" }.orEmpty())
 *             is Downloading -> Unit
 *             is Success ->
 *                 println("The upload is complete. TotalLength=${state.bytesUploaded}")
 *             is Failure ->
 *                 println("Failure: ${state.exception}")
 *         }
 *     }
 * }
 * ```
 *
 * This is a sample code that downloads the content of `url` using an HTTP GET method, and saves it
 * to `fileToSave`.
 * ```
 * suspend fun performDownload(url: HttpUrl, fileToSave: File) {
 *     val request = Request.Builder().url(url).build()
 *     okHttpClient.callWithCounting<Unit>(request) { response ->
 *         if (response.code != StatusCode.OK) {
 *             throw ResponseStatusException(response.code)
 *         }
 *         fileToSave.sink().use {
 *             checkNotNull(response.body).source().readAll(it)
 *         }
 *     }.collect { state ->
 *         when (state) {
 *             is Uploading -> Unit
 *             is Downloading ->
 *                 println("Downloading: ${state.bytesTransferred} bytes received." +
 *                     state.progressPercentage?.let { " ($it%)" }.orEmpty())
 *             is Success ->
 *                 println("The download is complete. TotalLength=${state.bytesDownloaded}")
 *             is Failure ->
 *                 println("Failure: ${state.exception}")
 *         }
 *     }
 * }
 * ```
 *
 * @param request An OkHttp [Request] to be executed.
 * @param countUpload If `false`, don't count the number of bytes in the request body.
 * @param countDownload If `false`, don't count the number of bytes in the response body.
 * @param handlePartialResponse If `true`, includes the `Content-Range` header value of a partial
 * response in [CallState.Downloading] and [CallState.Success].
 * @param throttleMillis Limits the emission of [CallState.Uploading] or [CallState.Downloading] to
 * at most once every millisecond of this value.
 * @param responseHandler A function to process an OkHttp [Response]. The response object will be
 * closed automatically after the function call. This function is called from a background thread of
 * OkHttp's thread pool.
 * @return A [Flow] that emits the progress of the HTTP call.
 * @see Response.saveToResourceWithSupportingResumption
 */
fun <T> OkHttpClient.callWithCounting(
    request: Request,
    countUpload: Boolean = true,
    countDownload: Boolean = true,
    handlePartialResponse: Boolean = true,
    throttleMillis: Long = 100L,
    responseHandler: (Response) -> T
): Flow<CallState<T>> {
    val unthrottledFlow = callbackFlow<CallState<T>> {
        val countingRequestBody =
            request.takeIf { countUpload }?.body?.let { CountingRequestBody(it, channel) }
        val call = newCall(countingRequestBody?.injectTo(request) ?: request)
        call.enqueue(
            CountingCallback(
                countingRequestBody,
                countDownload,
                handlePartialResponse,
                responseHandler,
                channel
            )
        )
        awaitClose(call::cancel)
    }.conflate()
    return if (throttleMillis > 0) {
        unthrottledFlow.onEach { if (it is TransferProgress) delay(throttleMillis) }
    } else unthrottledFlow
}

/**
 * The states that represent the progress of an HTTP call.
 *
 * @see OkHttpClient.callWithCounting
 */
sealed class CallState<out T> {
    /**
     * Common interface for [Uploading] and [Downloading].
     */
    @Deprecated(
        "Use `TransferProgress` instead.",
        ReplaceWith("TransferProgress", "com.linecorp.lich.okhttp.TransferProgress")
    )
    interface Progress {
        /**
         * The number of bytes that have been sent / received so far.
         */
        val bytesTransferred: Long

        /**
         * The number of bytes of the entire content. If it is unknown, this value is `-1`.
         */
        val bytesTotal: Long

        /**
         * Percentage of transferred bytes to the entire content. (`0..100`)
         * If [bytesTotal] is unknown, this value is `null`.
         */
        @Deprecated(
            "Use `TransferProgress.progressPercentage` instead.",
            ReplaceWith("progressPercentage", "com.linecorp.lich.okhttp.progressPercentage"),
            DeprecationLevel.HIDDEN
        )
        val progressPercentage: Int?
            get() = when {
                bytesTotal <= 0 -> null
                bytesTotal <= Long.MAX_VALUE / 100 -> (bytesTransferred * 100 / bytesTotal).toInt()
                else -> (bytesTransferred / (bytesTotal / 100)).toInt() // Avoid overflow.
            }
    }

    /**
     * An intermediate state indicating that the HTTP request body is being sent.
     *
     * This state is not emitted if the `countUpload` parameter is `false` or the `request` has no body.
     *
     * @property bytesTransferred The number of bytes that have been sent as the request body so far.
     * @property bytesTotal The length of the request body specified in the `Content-Length` header.
     */
    class Uploading(override val bytesTransferred: Long, override val bytesTotal: Long) :
        CallState<Nothing>(), TransferProgress, Progress

    /**
     * An intermediate state indicating that the HTTP response body is being received.
     *
     * This state is not emitted if the `countDownload` parameter is `false` or the response status
     * code is other than `2xx`.
     *
     * @property bytesTransferred The number of bytes that have been sent as the response body so far.
     * If the `handlePartialResponse` parameter is `true` and the response is a single-part
     * `206 Partial Content` response, this value includes the start offset of the `Content-Range` header.
     * @property bytesTotal If the `handlePartialResponse` parameter is `true` and the response is
     * a single-part `206 Partial Content` response, this value is the total length of the
     * `Content-Range` header. Otherwise, this is the length of the response body specified in the
     * `Content-Length` header.
     */
    class Downloading(override val bytesTransferred: Long, override val bytesTotal: Long) :
        CallState<Nothing>(), TransferProgress, Progress

    /**
     * A final state indicating that the HTTP call completed successfully.
     * After emitting this state, the `flow` completes normally.
     *
     * @property data The result returned by the `responseHandler`.
     * @property bytesUploaded The number of bytes finally sent as the request body,
     * or `-1` if the `countUpload` parameter is `false` or the `request` has no body.
     * @property bytesDownloaded The number of bytes finally received as the response body, unless
     * the `countDownload` parameter is `false` or the response status code is other than `2xx`.
     * If the `handlePartialResponse` parameter is `true` and the response is a single-part
     * `206 Partial Content` response, this value includes the start offset of the `Content-Range` header.
     * If the `handlePartialResponse` parameter is `true` and the response is `416 Range Not Satisfiable`,
     * this value is the total length of the `Content-Range` header.
     * If neither of them, this value is `-1`.
     */
    class Success<T>(val data: T, val bytesUploaded: Long, val bytesDownloaded: Long) :
        CallState<T>()

    /**
     * A final state indicating that the HTTP call failed with an [IOException].
     * After emitting this state, the `flow` completes normally.
     *
     * This state is also emitted when the `responseHandler` throws an [IOException].
     *
     * @property exception the [IOException] that caused the failure.
     */
    class Failure(val exception: IOException) : CallState<Nothing>()
}

/**
 * Common interface for [CallState.Uploading] and [CallState.Downloading].
 */
interface TransferProgress {
    /**
     * The number of bytes that have been sent / received so far.
     */
    val bytesTransferred: Long

    /**
     * The number of bytes of the entire content. If it is unknown, this value is `-1`.
     */
    val bytesTotal: Long
}

/**
 * Percentage of transferred bytes to the entire content. (`0..100`)
 * If [TransferProgress.bytesTotal] is unknown, this value is `null`.
 */
val TransferProgress.progressPercentage: Int?
    get() {
        val total = bytesTotal
        return when {
            total <= 0 -> null
            total <= Long.MAX_VALUE / 100 -> (bytesTransferred * 100 / total).toInt()
            else -> (bytesTransferred / (total / 100)).toInt() // Avoid overflow.
        }
    }

/**
 * A [RequestBody] that replaces the original one to count the number of bytes uploaded.
 */
private class CountingRequestBody(
    val delegate: RequestBody,
    private val channel: SendChannel<CallState.Uploading>
) : RequestBody() {

    var bytesUploaded: Long = 0

    fun injectTo(request: Request): Request =
        request.newBuilder().method(request.method, this).build()

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink, contentLength(), channel)

        val countingBufferedSink = countingSink.buffer()
        delegate.writeTo(countingBufferedSink)
        if (countingBufferedSink.isOpen) {
            countingBufferedSink.emit()
        }

        bytesUploaded = countingSink.bytesUploaded
    }

    override fun isOneShot(): Boolean = delegate.isOneShot()

    // CountingRequestBody cannot be duplex.
    override fun isDuplex(): Boolean = false
}

/**
 * A [Sink] for counting the number of bytes uploaded.
 */
private class CountingSink(
    delegate: Sink,
    private val contentLength: Long,
    private val channel: SendChannel<CallState.Uploading>
) : ForwardingSink(delegate) {

    var bytesUploaded: Long = 0

    override fun write(source: Buffer, byteCount: Long) {
        super.write(source, byteCount)
        bytesUploaded += byteCount
        emitUploadProgress()
    }

    private fun emitUploadProgress() {
        try {
            channel.offer(CallState.Uploading(bytesUploaded, contentLength))
        } catch (e: Exception) {
            // We ignore any exceptions thrown from `channel.offer()`.
            // cf. https://github.com/Kotlin/kotlinx.coroutines/issues/974
            // Cancellation will be notified as an IOException on `sink.write()`.
        }
    }
}

/**
 * A [Source] for counting the number of bytes downloaded.
 */
private class CountingSource(
    delegate: Source,
    startOffset: Long,
    private val contentLength: Long,
    private val channel: SendChannel<CallState.Downloading>
) : ForwardingSource(delegate) {

    var bytesDownloaded: Long = startOffset

    override fun read(sink: Buffer, byteCount: Long): Long =
        super.read(sink, byteCount).also { bytesRead ->
            if (bytesRead > 0) {
                bytesDownloaded += bytesRead
                emitDownloadProgress()
            }
        }

    private fun emitDownloadProgress() {
        try {
            channel.offer(CallState.Downloading(bytesDownloaded, contentLength))
        } catch (e: Exception) {
            // We ignore any exceptions thrown from `channel.offer()`.
            // cf. https://github.com/Kotlin/kotlinx.coroutines/issues/974
            // Cancellation will be notified as an IOException on `source.read()`.
        }
    }
}

/**
 * OkHttp [Callback] for [callWithCounting].
 */
private class CountingCallback<T>(
    private val countingRequestBody: CountingRequestBody?,
    private val countDownload: Boolean,
    private val handlePartialResponse: Boolean,
    private val responseHandler: (Response) -> T,
    private val channel: SendChannel<CallState<T>>
) : Callback {

    override fun onResponse(call: Call, response: Response) {
        runAndCloseChannel {
            val finalState = try {
                response.use { processResponse(it) }
            } catch (e: IOException) {
                CallState.Failure(e)
            }
            // Since the channel is CONFLATED, `offer()` will always succeed unless the channel
            // is cancelled.
            channel.offer(finalState)
        }
    }

    override fun onFailure(call: Call, e: IOException) {
        runAndCloseChannel {
            // Since the channel is CONFLATED, `offer()` will always succeed unless the channel
            // is cancelled.
            channel.offer(CallState.Failure(e))
        }
    }

    private fun processResponse(response: Response): CallState.Success<T> {
        val responseBuilderForHandler = response.newBuilder()

        val request = response.request
        val requestBody = request.body
        if (requestBody is CountingRequestBody) {
            // Restore the original request body.
            val restoredRequest = request.newBuilder()
                .method(request.method, requestBody.delegate)
                .build()
            responseBuilderForHandler.request(restoredRequest)
        }

        val countingSource = mayCreateCountingSource(response)
        if (countingSource != null) {
            val originalBody = checkNotNull(response.body)
            val countingResponseBody = countingSource.buffer()
                .asResponseBody(originalBody.contentType(), originalBody.contentLength())
            responseBuilderForHandler.body(countingResponseBody)
        }

        val data = responseHandler(responseBuilderForHandler.build())

        val bytesUploaded = countingRequestBody?.bytesUploaded ?: -1
        val bytesDownloaded = when {
            countingSource != null -> countingSource.bytesDownloaded
            countDownload && handlePartialResponse &&
                response.code == StatusCode.RANGE_NOT_SATISFIABLE ->
                response.mayGetTotalLengthOfUnsatisfiedRange() ?: -1
            else -> -1
        }
        return CallState.Success(data, bytesUploaded, bytesDownloaded)
    }

    private fun mayCreateCountingSource(response: Response): CountingSource? {
        if (!countDownload || !response.isSuccessful) return null

        val originalBody = checkNotNull(response.body)
        val originalSource = originalBody.source()
        val contentRange =
            if (handlePartialResponse && response.code == StatusCode.PARTIAL_CONTENT) {
                response.mayGetSinglePartContentRange()
            } else null
        return if (contentRange != null) {
            CountingSource(originalSource, contentRange.start, contentRange.totalLength, channel)
        } else {
            CountingSource(originalSource, 0L, originalBody.contentLength(), channel)
        }
    }

    private inline fun runAndCloseChannel(block: () -> Unit) {
        var closeCause: Throwable? = null
        try {
            block()
        } catch (e: Throwable) {
            closeCause = e
        } finally {
            channel.close(closeCause)
        }
    }
}

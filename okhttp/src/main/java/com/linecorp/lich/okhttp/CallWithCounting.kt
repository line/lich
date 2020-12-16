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
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.ForwardingSource
import okio.Okio
import okio.Sink
import okio.Source
import java.io.IOException

/**
 * Creates a [Flow] that executes an HTTP call with counting the number of bytes transferred in its
 * request and response body. Through the [Flow], you can see the progress of the HTTP call.
 *
 * This is a sample code that sends the contents of `fileToUpload` as a HTTP POST request and
 * writes the response body to `fileToDownload`.
 *
 * ```
 * val url: HttpUrl
 * val fileToUpload: File
 * val fileToDownload: File
 *
 * coroutineScope.launch {
 *     val request = Request.Builder()
 *         .url(url)
 *         .post(RequestBody.create(MediaType.get("application/octet-stream"), fileToUpload))
 *         .build()
 *     okHttpClient.callWithCounting(request) { response ->
 *         if (!response.isSuccessful) {
 *             throw IOException("HTTP Response code: ${response.code()}")
 *         }
 *         Okio.sink(fileToDownload).use { fileSink ->
 *             checkNotNull(response.body()).source().readAll(fileSink)
 *         }
 *         Unit
 *     }.collect { state ->
 *         when (state) {
 *             is Uploading ->
 *                 println("Uploading: ${state.bytesTransferred} bytes sent." +
 *                     state.progressPercentage?.let { " ($it%)" }.orEmpty())
 *             is Downloading ->
 *                 println("Downloading: ${state.bytesTransferred} bytes received." +
 *                     state.progressPercentage?.let { " ($it%)" }.orEmpty())
 *             is Success ->
 *                 println("Success: ${state.bytesUploaded} bytes sent" +
 *                     " and ${state.bytesDownloaded} bytes received.")
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
 * @param throttleMillis Limit the emission of [CallState.Uploading] or [CallState.Downloading] to
 * at most once every millisecond of this value.
 * @param responseHandler a function to process an OkHttp [Response]. The response object will be
 * closed automatically after the function call. This function is called from a background thread of
 * OkHttp's thread pool.
 * @return A [Flow] that emits the progress of the HTTP call.
 */
fun <T> OkHttpClient.callWithCounting(
    request: Request,
    countUpload: Boolean = true,
    countDownload: Boolean = true,
    throttleMillis: Long = 100L,
    responseHandler: (Response) -> T
): Flow<CallState<T>> {
    val unthrottledFlow = callbackFlow<CallState<T>> {
        val countingRequestBody =
            request.takeIf { countUpload }?.body()?.let { CountingRequestBody(it, channel) }
        val call = newCall(countingRequestBody?.injectTo(request) ?: request)
        call.enqueue(CountingCallback(countingRequestBody, countDownload, responseHandler, channel))
        awaitClose(call::cancel)
    }.conflate()
    return if (throttleMillis > 0) {
        unthrottledFlow.onEach { if (it is CallState.Progress) delay(throttleMillis) }
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
    interface Progress {
        /**
         * The number of bytes that have been sent / received as an HTTP request / response body
         * so far.
         */
        val bytesTransferred: Long

        /**
         * The number of bytes of the request / response body specified in the `Content-Length`
         * header. If the header does not exist, this value is `-1`.
         */
        val bytesTotal: Long

        /**
         * Percentage of transferred bytes to entire request / response body. (`0..100`)
         * If the `Content-Length` header does not exist, this is `null`.
         */
        val progressPercentage: Int?
            get() = when {
                bytesTotal <= 0 -> null
                bytesTotal <= Long.MAX_VALUE / 100 -> (bytesTransferred * 100 / bytesTotal).toInt()
                else -> (bytesTransferred / (bytesTotal / 100)).toInt() // Avoid overflow.
            }
    }

    /**
     * An intermediate state indicating that the HTTP request body is being sent.
     * This state is not emitted if the `countUpload` parameter is `false` or the `request` has no
     * body.
     */
    class Uploading(override val bytesTransferred: Long, override val bytesTotal: Long) :
        CallState<Nothing>(), Progress

    /**
     * An intermediate state indicating that the HTTP response body is being received.
     * This state is not emitted if the `countDownload` parameter is `false`.
     */
    class Downloading(override val bytesTransferred: Long, override val bytesTotal: Long) :
        CallState<Nothing>(), Progress

    /**
     * A final state indicating that the HTTP call completed successfully.
     * After emitting this state, the `flow` completes normally.
     *
     * @param data The result returned by the `responseHandler`.
     * @param bytesUploaded The number of bytes finally sent as the HTTP request body,
     * or `-1` if the `countUpload` parameter is `false` or the `request` has no body.
     * @param bytesDownloaded The number of bytes finally received as the HTTP response body,
     * or `-1` if the `countDownload` parameter is `false`.
     */
    class Success<T>(val data: T, val bytesUploaded: Long, val bytesDownloaded: Long) :
        CallState<T>()

    /**
     * A final state indicating that the HTTP call failed with an [IOException].
     * After emitting this state, the `flow` completes normally.
     *
     * This state is also emitted when the `responseHandler` throws an [IOException].
     *
     * @param exception the [IOException] that caused the failure.
     */
    class Failure(val exception: IOException) : CallState<Nothing>()
}

/**
 * A [RequestBody] that replaces the original one to count the number of bytes uploaded.
 */
private class CountingRequestBody(
    private val delegate: RequestBody,
    private val channel: SendChannel<CallState.Uploading>
) : RequestBody() {

    var bytesUploaded: Long = 0

    fun injectTo(request: Request): Request =
        request.newBuilder().method(request.method(), this).build()

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink, contentLength(), channel)

        val countingBufferedSink = Okio.buffer(countingSink)
        delegate.writeTo(countingBufferedSink)
        if (countingBufferedSink.isOpen) {
            countingBufferedSink.emit()
        }

        bytesUploaded = countingSink.bytesUploaded
    }
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
    private val contentLength: Long,
    private val channel: SendChannel<CallState.Downloading>
) : ForwardingSource(delegate) {

    var bytesDownloaded: Long = 0

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
    private val responseHandler: (Response) -> T,
    private val channel: SendChannel<CallState<T>>
) : Callback {

    override fun onResponse(call: Call, response: Response) {
        runAndCloseChannel {
            val finalState = try {
                processResponse(response)
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

    private fun processResponse(originalResponse: Response): CallState.Success<T> {
        val countingSource: CountingSource?
        val responseForHandler: Response
        if (countDownload) {
            // Inject `CountingSource` to the response body.
            val originalBody = checkNotNull(originalResponse.body())
            countingSource =
                CountingSource(originalBody.source(), originalBody.contentLength(), channel)
            val countingResponseBody = ResponseBody.create(
                originalBody.contentType(),
                originalBody.contentLength(),
                Okio.buffer(countingSource)
            )
            responseForHandler = originalResponse.newBuilder().body(countingResponseBody).build()
        } else {
            countingSource = null
            responseForHandler = originalResponse
        }

        val data = responseForHandler.use { responseHandler(it) }

        val bytesUploaded = countingRequestBody?.bytesUploaded ?: -1
        val bytesDownloaded = countingSource?.bytesDownloaded ?: -1
        return CallState.Success(data, bytesUploaded, bytesDownloaded)
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

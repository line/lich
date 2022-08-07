/*
 * Copyright 2022 LINE Corporation
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

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import okhttp3.Response
import okio.sink

/**
 * Saves the response body to [fileToSave] **atomically**.
 *
 * [fileToSave] is updated if and only if the entire response body has been successfully downloaded.
 * Otherwise, [fileToSave] will not be modified at all.
 *
 * This is a sample code that downloads a content of the given URL to a file.
 * ```
 * suspend fun performDownloadAtomically(url: HttpUrl, fileToSave: File): Boolean {
 *     val request = Request.Builder().url(url).build()
 *     return try {
 *         okHttpClient.call(request) { response ->
 *             if (response.code != StatusCode.OK) {
 *                 throw ResponseStatusException(response.code)
 *             }
 *             response.saveBodyToFileAtomically(fileToSave)
 *         }
 *         // At this point, `fileToSave` contains the complete response body downloaded.
 *         true
 *     } catch (e: IOException) {
 *         println("Failed to download: $e")
 *         // At this point, `fileToSave` is not modified at all.
 *         false
 *     }
 * }
 * ```
 *
 * Implementation note: This function is inspired by `android.util.AtomicFile`, but does not depend
 * on any Android API. Technically, this atomicity is implemented by writing the response body to
 * a temporary file and then renaming it. The name of this temporary file is given by appending
 * [suffixForTmpFile] to the path of [fileToSave].
 *
 * @param fileToSave the file to save the downloaded content.
 * @param suffixForTmpFile the suffix to be appended to the temporary file.
 * @throws IOException if any I/O error occurs. In this case, [fileToSave] is not modified at all.
 */
fun Response.saveBodyToFileAtomically(fileToSave: File, suffixForTmpFile: String = ".new") {
    if (suffixForTmpFile.isEmpty()) {
        throw IllegalArgumentException("suffixForTmpFile must not be empty.")
    }

    val tmpFile = File(fileToSave.path + suffixForTmpFile)
    tmpFile.parentFile?.mkdirs()
    val tmpFileOutputStream = FileOutputStream(tmpFile)
    try {
        tmpFileOutputStream.use {
            val responseBody = body ?: throw IOException("This response has no body.")
            responseBody.source().readAll(it.sink())
            it.flush()
            it.fd.sync()
        }
        if (!tmpFile.renameTo(fileToSave)) {
            throw IOException("Failed to rename the tmpFile to $fileToSave")
        }
    } catch (e: IOException) {
        tmpFile.delete()
        throw e
    }
}

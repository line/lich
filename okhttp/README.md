# Lich OkHttp

[ ![Maven Central](https://badgen.net/maven/v/maven-central/com.linecorp.lich/okhttp) ](https://search.maven.org/artifact/com.linecorp.lich/okhttp)

Coroutine-aware extensions for [OkHttp](https://square.github.io/okhttp/).

```groovy
// build.gradle
dependencies {
    implementation 'com.linecorp.lich:okhttp:x.x.x'
}
```

- [OkHttpClient.call()](src/main/java/com/linecorp/lich/okhttp/Call.kt) - A suspending function to
send an HTTP request and receive its response.
- [Response.saveBodyToFileAtomically()](src/main/java/com/linecorp/lich/okhttp/AtomicDownload.kt) -
Saves an HTTP response body to a file *atomically*.
- [OkHttpClient.callWithCounting()](src/main/java/com/linecorp/lich/okhttp/CallWithCounting.kt) -
Creates a `Flow` that executes an HTTP call with counting the number of bytes transferred in its
request and response body.
- [Response.saveToResourceWithSupportingResumption()](src/main/java/com/linecorp/lich/okhttp/ResumableDownload.kt) -
Performs a *resumable download* using the HTTP semantics defined in [RFC 9110, Section 14](https://www.rfc-editor.org/rfc/rfc9110.html#section-14).

## Simple HTTP call

This is a sample code that fetches a content of the given URL as a `String`.
```kotlin
suspend fun fetchContentAsString(url: String): String {
    val request = Request.Builder().url(url).build()
    return okHttpClient.call(request) { response ->
        if (!response.isSuccessful) {
            throw ResponseStatusException(response.code)
        }
        checkNotNull(response.body).string()
    }
}
```

This is an example that calls a JSON API and parses the response using [Gson](https://github.com/google/gson).
```kotlin
suspend fun fetchFooJson(url: String): Foo {
    val request = Request.Builder().url(url).build()
    return okHttpClient.call(request) { response ->
        if (!response.isSuccessful) {
            throw ResponseStatusException(response.code)
        }
        try {
            gson.fromJson(checkNotNull(response.body).charStream(), Foo::class.java)
        } catch (e: JsonParseException) {
            throw IOException(e)
        }
    }
}
```

NOTE: You *don't* need to use `withContext(Dispatchers.IO) { ... }` in the above code.
The `response` handler of the `call` function is always executed on OkHttp's background threads,
and the caller thread is never blocked.

## Download to file atomically

This is a sample code that downloads the content of `url` using an HTTP GET method, and saves it to
`fileToSave`. This download is performed *atomically*. That is, `fileToSave` is updated if and only
if the entire response body has been successfully downloaded.
```kotlin
suspend fun performDownloadAtomically(url: HttpUrl, fileToSave: File): Boolean {
    val request = Request.Builder().url(url).build()
    return try {
        okHttpClient.call(request) { response ->
            if (response.code != StatusCode.OK) {
                throw ResponseStatusException(response.code)
            }
            response.saveBodyToFileAtomically(fileToSave)
        }
        // At this point, `fileToSave` contains the complete response body downloaded.
        true
    } catch (e: IOException) {
        println("Failed to download: $e")
        // At this point, `fileToSave` is not modified at all.
        false
    }
}
```

## Upload with progress monitoring

This is a sample code that uploads the content of `fileToUpload` with monitoring its progress.
```kotlin
suspend fun performUploadWithProgress(url: HttpUrl, fileToUpload: File) {
    val request = Request.Builder()
        .url(url)
        .post(fileToUpload.asRequestBody("application/octet-stream".toMediaType()))
        .build()
    okHttpClient.callWithCounting(request, countDownload = false) { response ->
        if (!response.isSuccessful) {
            throw ResponseStatusException(response.code)
        }
    }.collect { state ->
        when (state) {
            is Uploading ->
                println("Uploading: ${state.bytesTransferred} bytes sent." +
                    state.progressPercentage?.let { " ($it%)" }.orEmpty())
            is Downloading -> Unit
            is Success ->
                println("The upload is complete. TotalLength=${state.bytesUploaded}")
            is Failure ->
                println("Failure: ${state.exception}")
        }
    }
}
```

## Download with progress monitoring

This is a sample code that downloads the content of `url` to `fileToSave` with monitoring its progress.
```kotlin
suspend fun performDownloadWithProgress(url: HttpUrl, fileToSave: File) {
    val request = Request.Builder().url(url).build()
    okHttpClient.callWithCounting(request) { response ->
        if (response.code != StatusCode.OK) {
            throw ResponseStatusException(response.code)
        }
        response.saveBodyToFileAtomically(fileToSave)
    }.collect { state ->
        when (state) {
            is Uploading -> Unit
            is Downloading ->
                println("Downloading: ${state.bytesTransferred} bytes received." +
                    state.progressPercentage?.let { " ($it%)" }.orEmpty())
            is Success ->
                println("The download is complete. TotalLength=${state.bytesDownloaded}")
            is Failure ->
                println("Failure: ${state.exception}")
        }
    }
}
```

## Resumable download

This is similar to the example above, but if it fails in the middle of the download, `fileToSave`
will still contain what has been downloaded up to that point. Then, when it is run again, the
download will resume from the continuation.
```kotlin
suspend fun performResumableDownloadWithProgress(url: HttpUrl, fileToSave: File) {
    val resourceToSave = fileToSave.asWritableResource()
    val request = Request.Builder()
        .url(url)
        .setRangeHeader(resourceToSave.length)
        .build()
    okHttpClient.callWithCounting(request) { response ->
        response.saveToResourceWithSupportingResumption(resourceToSave)
    }.collect { state ->
        when (state) {
            is Uploading -> Unit
            is Downloading ->
                println("Downloading: ${state.bytesTransferred} bytes downloaded." +
                    state.progressPercentage?.let { " ($it%)" }.orEmpty())
            is Success ->
                if (state.data)
                    println("The download is complete. TotalLength=${state.bytesDownloaded}")
                else
                    println("The HTTP call was successful, but the content may have a remaining part.\n" +
                        "To complete the download, call this function again.")
            is Failure ->
                println("Failure: ${state.exception}\nTo resume the download, call this function again.")
        }
    }
}
```

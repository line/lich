# Lich OkHttp

[ ![Maven Central](https://badgen.net/maven/v/maven-central/com.linecorp.lich/okhttp) ](https://search.maven.org/artifact/com.linecorp.lich/okhttp)

Coroutine-aware extensions for [OkHttp](https://square.github.io/okhttp/).

```groovy
// build.gradle
dependencies {
    implementation 'com.linecorp.lich:okhttp:x.x.x'
}
```

- [OkHttpClient.call()](src/main/java/com/linecorp/lich/okhttp/Call.kt) - A suspending
function to send an HTTP request and receive its response.
- [OkHttpClient.callWithCounting()](src/main/java/com/linecorp/lich/okhttp/CallWithCounting.kt) -
Creates a `Flow` that executes an HTTP call with counting the number of bytes transferred in its
request and response body.
- [Response.saveToResourceWithSupportingResumption()](src/main/java/com/linecorp/lich/okhttp/ResumableDownload.kt) -
Performs a *resumable download* using the HTTP semantics defined in [RFC 7233](https://tools.ietf.org/html/rfc7233).

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

## File upload

This is a sample code that sends the content of `fileToUpload` as an HTTP POST method.
```kotlin
suspend fun performUpload(url: HttpUrl, fileToUpload: File) {
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

## File download

This is a sample code that downloads the content of `url` using an HTTP GET method, and saves it to `fileToSave`.
```kotlin
suspend fun performDownload(url: HttpUrl, fileToSave: File) {
    val request = Request.Builder().url(url).build()
    okHttpClient.callWithCounting<Unit>(request) { response ->
        if (response.code != StatusCode.OK) {
            throw ResponseStatusException(response.code)
        }
        fileToSave.sink().use {
            checkNotNull(response.body).source().readAll(it)
        }
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

This is a sample code that performs a resumable download using Range requests defined in RFC 7233.
```kotlin
suspend fun performResumableDownload(url: HttpUrl, fileToSave: File) {
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

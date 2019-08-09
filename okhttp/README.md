# Lich OkHttp

A small library for [OkHttp](https://square.github.io/okhttp/).

```groovy
// build.gradle
dependencies {
    implementation 'com.linecorp.lich:okhttp:x.x.x'
}
```

- [OkHttpClient.call()](src/main/java/com/linecorp/lich/okhttp/OkHttpExtensions.kt) - A suspending
function to send an HTTP request and receive its response.

This is a sample code that fetches a content of the given URL as a `String`.
```kotlin
@Throws(IOException::class)
suspend fun fetchContentAsString(url: String): String {
    val request = Request.Builder().url(url).build()
    return okHttpClient.call(request) { response ->
        if (!response.isSuccessful) {
            throw IOException("HTTP Response code: ${response.code()}")
        }
        checkNotNull(response.body()).string()
    }
}
```

# Lich Thrift

[ ![Maven Central](https://badgen.net/maven/v/maven-central/com.linecorp.lich/thrift) ](https://search.maven.org/artifact/com.linecorp.lich/thrift)

A small library for [Apache Thrift](https://thrift.apache.org/).

```groovy
// build.gradle
dependencies {
    implementation 'com.linecorp.lich:thrift:x.x.x'
}
```

This library provides the following APIs.

- [OkHttpClient.callThrift()](src/main/java/com/linecorp/lich/thrift/ThriftExtensions.kt) - A
suspending function to call a remote Thrift service over HTTP.
- [ThriftCallHandler](src/main/java/com/linecorp/lich/thrift/ThriftCallHandler.kt) - A handler
responsible for creating instances of `TServiceClient` and handling HTTP requests for Thrift Service
calls.
- [AbstractThriftCallHandler](src/main/java/com/linecorp/lich/thrift/AbstractThriftCallHandler.kt) -
Skeleton of `ThriftCallHandler`.

This is a sample code to call [FooService](../sample_thrift/src/main/thrift/FooService.thrift):
```kotlin
class MyThriftCallHandler<T : TServiceClient>(
    serviceClientFactory: TServiceClientFactory<T>,
    endpointPath: String
) : AbstractThriftCallHandler<T>(serviceClientFactory) {

    override val endpointUrl: HttpUrl = endpointUrlBase.resolve(endpointPath)
        ?: throw IllegalArgumentException("Invalid path: $endpointPath")

    companion object {
        val endpointUrlBase: HttpUrl = HttpUrl.get("https://api.example.com")
    }
}

class FooServiceClient(private val okHttpClient: OkHttpClient) {

    private val handler: ThriftCallHandler<FooService.Client> =
        MyThriftCallHandler(FooService.Client.Factory(), "/foo")

    suspend fun ping() =
        okHttpClient.callThrift(handler,
            { send_ping() },
            { recv_ping() }
        )

    suspend fun callFoo(id: Long, name: String, param: FooParam): FooResponse =
        okHttpClient.callThrift(handler,
            { send_callFoo(id, name, param) },
            { recv_callFoo() }
        )
}
```

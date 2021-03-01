# Lich Thrift

[ ![Maven Central](https://badgen.net/maven/v/maven-central/com.linecorp.lich/thrift) ](https://search.maven.org/artifact/com.linecorp.lich/thrift)

A library for using [Apache Thrift](https://thrift.apache.org/) in combination with [OkHttp](https://square.github.io/okhttp/).

```groovy
// build.gradle
dependencies {
    implementation 'com.linecorp.lich:thrift:x.x.x'
}
```

The basic usage is just to implement a class that inherits
[AbstractThriftServiceClient](src/main/java/com/linecorp/lich/thrift/AbstractThriftServiceClient.kt).

For example, the code that calls [FooService](../sample_thrift/src/main/thrift/FooService.thrift)
can be implemented as follows:

```kotlin
class FooServiceClient(
    override val okHttpClient: OkHttpClient,
    override val endpointUrl: HttpUrl
) : AbstractThriftServiceClient<FooService.Client>() {

    override val thriftClientFactory: ThriftClientFactory<FooService.Client> =
        ThriftClientFactory(FooService.Client.Factory())

    suspend fun ping() =
        call({ send_ping() }, { recv_ping() })

    suspend fun callFoo(id: Long, name: String, param: FooParam): FooResponse =
        call({ send_callFoo(id, name, param) }, { recv_callFoo() })
}

val fooServiceClient = FooServiceClient(OkHttpClient(), "https://api.example.com/foo".toHttpUrl())
fooServiceClient.ping()
```

## Transparent logging

[thrift-logging](../thrift-logging) provides transparent logging with dynamic code generation.

```groovy
// build.gradle
dependencies {
    debugImplementation 'com.linecorp.lich:thrift-logging:x.x.x'
}
```

```kotlin
val thriftLogger = object : ThriftLogger {
    override fun logSend(namespace: String, service: String, function: String, args: TBase<*, *>) {
        Log.d("ThriftLogger", "$service.send_$function: $args")
    }

    override fun logReceive(namespace: String, service: String, function: String, result: TBase<*, *>) {
        Log.d("ThriftLogger", "$service.recv_$function: $result")
    }
}
val thriftLogEnabler = ThriftLogEnabler(context, thriftLogger)

class FooServiceClient : AbstractThriftServiceClient<FooService.Client>() {

    override val thriftClientFactory: ThriftClientFactory<FooService.Client> =
        thriftLogEnabler.enableLogging(ThriftClientFactory(FooService.Client.Factory()))

    // snip...
}
```

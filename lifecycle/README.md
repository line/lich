# Lich Lifecycle

[ ![Maven Central](https://badgen.net/maven/v/maven-central/com.linecorp.lich/lifecycle) ](https://search.maven.org/artifact/com.linecorp.lich/lifecycle)

A small library for Android Jetpack Lifecycle.

```groovy
// build.gradle
dependencies {
    implementation 'com.linecorp.lich:lifecycle:x.x.x'
}
```

## AutoResetLifecycleScope: A safer alternative to AndroidX lifecycleScope

[AutoResetLifecycleScope](src/main/java/com/linecorp/lich/lifecycle/AutoResetLifecycleScope.kt) is
a safer alternative to AndroidX [lifecycleScope](https://developer.android.com/reference/kotlin/androidx/lifecycle/package-summary#lifecyclescope).

This scope is bound to [Dispatchers.Main](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-main.html),
and will be cancelled when the `Lifecycle` is DESTROYED.
In addition, any coroutines launched from this scope are automatically cancelled when the
`Lifecycle` is STOPPED. (If `ResetPolicy.ON_STOP` is specified for the `resetPolicy` parameter.)

This "auto-reset" feature is useful for collecting `Flow`s safely.
It is known that collecting `Flow`s with `lifecycleScope.launchWhenStarted` can
[lead to resource leaks](https://link.medium.com/OR5ePKTGthb).
This is because coroutines launched by `lifecycleScope.launchWhenStarted` will not be cancelled
even if the `Lifecycle` is stopped.
On the other hand, coroutines launched from [AutoResetLifecycleScope](src/main/java/com/linecorp/lich/lifecycle/AutoResetLifecycleScope.kt)
are automatically cancelled when the `Lifecycle` is stopped. This avoids resource leaks.

The following code is an example of using [AutoResetLifecycleScope](src/main/java/com/linecorp/lich/lifecycle/AutoResetLifecycleScope.kt)
to collect a `Flow` safely.

```kotlin
class FooActivity : AppCompatActivity() {

    // Any coroutines launched from this scope are automatically cancelled when FooActivity is STOPPED.
    private val autoResetLifecycleScope: CoroutineScope = AutoResetLifecycleScope(this)

    // A repository that provides some data as a Flow.
    private val fooRepository = FooRepository()

    override fun onStart() {
        super.onStart()

        // It is SAFE to collect a flow with AutoResetLifecycleScope.
        // The coroutine launched here will be automatically cancelled just before `FooActivity.onStop()`.
        autoResetLifecycleScope.launch {
            fooRepository.dataFlow().collect { value ->
                textView.text = value
            }
        }
    }
}
```

[LifecycleScopeDemoActivity](../sample_app/src/main/java/com/linecorp/lich/sample/lifecyclescope/LifecycleScopeDemoActivity.kt)
also demonstrates the pitfalls of collecting flows with [lifecycleScope](https://developer.android.com/reference/kotlin/androidx/lifecycle/package-summary#lifecyclescope).

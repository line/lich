# Lich Lifecycle

[ ![Download](https://api.bintray.com/packages/line/lich/lifecycle/images/download.svg) ](https://bintray.com/line/lich/lifecycle/_latestVersion)

A small library for Android Jetpack Lifecycle.

```groovy
// build.gradle
dependencies {
    implementation 'com.linecorp.lich:lifecycle:x.x.x'
}
```

## AutoResetLifecycleScope

[AutoResetLifecycleScope](src/main/java/com/linecorp/lich/lifecycle/AutoResetLifecycleScope.kt)
is a lifecycle-aware
[CoroutineScope](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/).

`AutoResetLifecycleScope` will be canceled when its `Lifecycle` is destroyed.
`AutoResetLifecycleScope` will also "reset" jobs launched from it when the Lifecycle events
specified by `resetPolicy` have occurred.

`AutoResetLifecycleScope` is bound to
[Dispatchers.Main](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-main.html).
So, jobs launched from the scope will be executed on the main thread by default.

`AutoResetLifecycleScope` is similar to
[Android Architecture Components' lifecycleScope](https://developer.android.com/reference/kotlin/androidx/lifecycle/package-summary.html#lifecyclescope),
but it is canceled only when its `Lifecycle` is destroyed.

Example of use:
```kotlin
class FooActivity : FragmentActivity() {

    private val coroutineScope: CoroutineScope = AutoResetLifecycleScope(this)

    fun loadDataThenDraw() {
        // The launched job will be automatically cancelled ON_STOP and ON_DESTROY.
        coroutineScope.launch {
            try {
                val fooData = fooServiceClient.fetchFooData()
                drawData(fooData)
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to fetch data.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @MainThread
    private fun drawData(fooData: FooData) {
        // snip...
    }
}
```
See also
[SimpleCoroutineActivity](../sample_app/src/main/java/com/linecorp/lich/sample/simplecoroutine/SimpleCoroutineActivity.kt).

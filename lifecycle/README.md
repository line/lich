# Lich Lifecycle

[ ![Maven Central](https://badgen.net/maven/v/maven-central/com.linecorp.lich/lifecycle) ](https://search.maven.org/artifact/com.linecorp.lich/lifecycle)

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
`AutoResetLifecycleScope` will also "reset" coroutines launched from it when the Lifecycle events
specified by `resetPolicy` have occurred.

`AutoResetLifecycleScope` is bound to
[Dispatchers.Main](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-main.html).
So, coroutines launched from the scope will be executed on the main thread by default.

Example of use:
```kotlin
class FooActivity : AppCompatActivity() {

    private val coroutineScope: CoroutineScope = AutoResetLifecycleScope(this)

    fun loadDataThenDraw() {
        // The launched coroutines will be automatically cancelled ON_STOP and ON_DESTROY.
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

### About difference with Jetpack's lifecycleScope

AndroidX Lifecycle 2.2.0+ provides `lifecycleScope`. Please read
[this document](https://developer.android.com/topic/libraries/architecture/coroutines#lifecyclescope)
first for details. `lifecycleScope` has useful functions like `launchWhenStarted`,
but these functions must be used with care.

[LifecycleScopeDemo1Fragment](../sample_app/src/main/java/com/linecorp/lich/sample/lifecyclescope/LifecycleScopeDemo1Fragment.kt)
is an example. Here are some things to keep in mind when using `lifecycleScope`:

- Avoid using `Fragment.lifecycleScope.launch*`. Coroutines launched from `Fragment.lifecycleScope` stay alive even after `onDestroyView()`. So, you need to be careful about View recreation.
- `Fragment.viewLifecycleOwner.lifecycleScope.launchWhenStarted` is somewhat safe, but its coroutines stay alive even after `onStop()` and restart execution after the next `onStart()`. So, you shouldn't use it for tasks launched on `onStart()`.

Thus, `lifecycleScope` has some pitfalls. So, you should consider using alternatives.

The best way is to use coroutines only in ViewModels. All asynchronous tasks are launched
in ViewModels, and Fragment/Activity only observes the result via LiveData.
See [SampleViewModel](../sample_app/src/main/java/com/linecorp/lich/sample/mvvm/SampleViewModel.kt)
for a working example.

Another alternative is using `AutoResetLifecycleScope`. Any coroutines launched from it
are just cancelled when `onStop()` is called. So it is relatively safe to use.

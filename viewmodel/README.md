# Lich ViewModel

[ ![Download](https://api.bintray.com/packages/line/lich/viewmodel/images/download.svg) ](https://bintray.com/line/lich/viewmodel/_latestVersion)

Lightweight framework for managing ViewModels in the same way as [Lich Component](../component).

This library is very similar to
[Android Architecture Components's ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel),
but it has the following advantages:

- Declare and use ViewModels with simple code.
- Use mock instances in unit tests easily.

## Set up

First, add the following entry to your `build.gradle` file.
```groovy
dependencies {
    implementation 'com.linecorp.lich:viewmodel:x.x.x'
}
```

Then, make your `Application` class implement
[BridgeViewModelProviderOwner](src/main/java/com/linecorp/lich/viewmodel/provider/BridgeViewModelProviderOwner.kt)
like this:
```xml
<!-- AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.app">

    <application android:name=".MyApplication">
        <!-- snip... -->
    </application>

</manifest>
```
```kotlin
package com.example.app

class MyApplication : Application(), BridgeViewModelProviderOwner {

    override val bridgeViewModelProvider: BridgeViewModelProvider = BridgeViewModelProvider()

    // snip...
}
```

### for testing

The `viewmodel-test` module provides [AndroidX Test](https://developer.android.com/training/testing/set-up-project)
support. (For Robolectric, see also [this document](http://robolectric.org/androidx_test/).)
And, it also provides helper functions to work with [Mockito-Kotlin](https://github.com/nhaarman/mockito-kotlin).
To use these features, please add the following dependencies:

```groovy
dependencies {
    testImplementation 'com.linecorp.lich:viewmodel-test:x.x.x'
    testImplementation 'androidx.test:runner:x.x.x'
    testImplementation 'androidx.test.ext:junit:x.x.x'
    testImplementation 'org.mockito:mockito-inline:x.x.x'
    testImplementation 'com.nhaarman.mockitokotlin2:mockito-kotlin:x.x.x'
    testImplementation 'org.robolectric:robolectric:x.x'

    androidTestImplementation 'com.linecorp.lich:viewmodel-test:x.x.x'
    androidTestImplementation 'androidx.test:runner:x.x.x'
    androidTestImplementation 'androidx.test.ext:junit:x.x.x'
    androidTestImplementation 'org.mockito:mockito-android:x.x.x'
    androidTestImplementation 'com.nhaarman.mockitokotlin2:mockito-kotlin:x.x.x'
}
```

## How to use

In this library, your ViewModel classes should extend
[AbstractViewModel](src/main/java/com/linecorp/lich/viewmodel/AbstractViewModel.kt) and have a
companion object inheriting
[ViewModelFactory](src/main/java/com/linecorp/lich/viewmodel/ViewModelFactory.kt).

This is a sample code:
```kotlin
class FooViewModel(private val context: Context) : AbstractViewModel() {

    // snip...

    companion object : ViewModelFactory<FooViewModel>() {
        override fun createViewModel(context: Context): FooViewModel =
            FooViewModel(context)
    }
}
```

You can obtain an instance of the ViewModel using
[ComponentActivity.viewModel](src/main/java/com/linecorp/lich/viewmodel/ViewModelLazy.kt)
or
[Fragment.viewModel](src/main/java/com/linecorp/lich/viewmodel/ViewModelLazy.kt) like this:

```kotlin
class FooActivity : AppCompatActivity() {

    // An instance of FooViewModel associated with FooActivity.
    private val fooViewModel by viewModel(FooViewModel)

    // snip...
}
```

```kotlin
class FooFragment : Fragment() {

    // An instance of FooViewModel associated with FooFragment.
    private val fooViewModel by viewModel(FooViewModel)

    // snip...
}
```

You can also use
[Fragment.activityViewModel](src/main/java/com/linecorp/lich/viewmodel/ViewModelLazy.kt)
to obtain a ViewModel associated with the Activity hosting the current Fragment.
This ViewModel can be used to share data between Fragments and their host Activity.

```kotlin
class FooFragment : Fragment() {

    // A shared instance of FooViewModel associated with the Activity hosting this FooFragment.
    private val fooActivityViewModel by activityViewModel(FooViewModel)

    // snip...
}
```

## Coroutines support

[AbstractViewModel](src/main/java/com/linecorp/lich/viewmodel/AbstractViewModel.kt) is implementing
[CoroutineScope](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/)
interface. This scope is bound to
[Dispatchers.Main](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-main.html),
and will be cancelled just before `AbstractViewModel.onCleared()` is called.

```kotlin
class FooViewModel(private val fooRepository: FooRepository) : AbstractViewModel() {

    val fooText: MutableLiveData<String> = MutableLiveData("")

    fun loadFooText() {
        // The launched job will be automatically cancelled when this ViewModel is destroyed.
        launch {
            val foo = fooRepository.loadFoo()
            fooText.value = foo.text
        }
    }

    // snip...
}
```

This feature is equivalent to
[Android Architecture Components' ViewModel.viewModelScope](https://developer.android.com/reference/kotlin/androidx/lifecycle/package-summary.html#viewmodelscope)
except that it is implemented as an extension property.

## Example

[SampleViewModel](../sample_app/src/main/java/com/linecorp/lich/sample/mvvm/SampleViewModel.kt)

## Testing

You can mock ViewModels using
[mockViewModel](../viewmodel-test/src/main/java/com/linecorp/lich/viewmodel/test/MockitoViewModelMocks.kt)
function.

```kotlin
@RunWith(AndroidJUnit4::class)
class FooActivityTest {

    @After
    fun tearDown() {
        // You can omit this in Robolectric tests, because Robolectric recreates
        // `applicationContext` for each test.
        clearAllMockViewModels()
    }

    @Test
    fun testViewBinding() {
        val mockFooText: MutableLiveData<String> = MutableLiveData("Mocked.")
        // Set mock ViewModel factory for `FooViewModel`.
        val mockViewModelHandle = mockViewModel(FooViewModel) {
            on { fooText } doReturn mockFooText
        }

        ActivityScenario.launch(FooActivity::class.java).use { scenario ->

            scenario.onActivity {
                assertTrue(mockViewModelHandle.isCreated)
            }

            onView(withId(R.id.foo_text)).check(matches(withText("Mocked.")))

            onView(withId(R.id.load_foo_button)).perform(click())

            scenario.onActivity {
                verify(mockViewModelHandle.mock, times(1)).loadFooText()
            }
        }
    }
}
```
See also
[MvvmSampleActivityTest](../sample_app/src/test/java/com/linecorp/lich/sample/mvvm/MvvmSampleActivityTest.kt).

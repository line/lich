# Lich ViewModel

[ ![Maven Central](https://badgen.net/maven/v/maven-central/com.linecorp.lich/viewmodel) ](https://search.maven.org/artifact/com.linecorp.lich/viewmodel)

Lightweight framework for managing ViewModels in the same way as [Lich Component](../component).

This library is very similar to
[AndroidX ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel),
but it has the following advantages:

- The code for initializing a ViewModel class can be written in the ViewModel itself.
- It is easy to write unit tests for Activity / Fragment with mocking ViewModels.

## Set up

Add the following entries to your `build.gradle` file.

```groovy
dependencies {
    implementation 'com.linecorp.lich:viewmodel:x.x.x'

    testImplementation 'com.linecorp.lich:viewmodel-test-mockk:x.x.x'
    testImplementation 'androidx.test:runner:x.x.x'
    testImplementation 'androidx.test.ext:junit:x.x.x'
    testImplementation 'io.mockk:mockk:x.x.x'
    testImplementation 'org.robolectric:robolectric:x.x'

    androidTestImplementation 'com.linecorp.lich:viewmodel-test-mockk:x.x.x'
    androidTestImplementation 'androidx.test:runner:x.x.x'
    androidTestImplementation 'androidx.test.ext:junit:x.x.x'
    androidTestImplementation 'io.mockk:mockk-android:x.x.x'
}
```

The above code uses [MockK](https://mockk.io/) as a mocking library.
If you prefer [Mockito-Kotlin](https://github.com/mockito/mockito-kotlin) over MockK, you can specify the following instead.

```groovy
dependencies {
    implementation 'com.linecorp.lich:viewmodel:x.x.x'

    testImplementation 'com.linecorp.lich:viewmodel-test-mockitokotlin:x.x.x'
    testImplementation 'androidx.test:runner:x.x.x'
    testImplementation 'androidx.test.ext:junit:x.x.x'
    testImplementation 'org.mockito:mockito-inline:x.x.x'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:x.x.x'
    testImplementation 'org.robolectric:robolectric:x.x'

    androidTestImplementation 'com.linecorp.lich:viewmodel-test-mockitokotlin:x.x.x'
    androidTestImplementation 'androidx.test:runner:x.x.x'
    androidTestImplementation 'androidx.test.ext:junit:x.x.x'
    androidTestImplementation 'org.mockito:mockito-android:x.x.x'
    androidTestImplementation 'org.mockito.kotlin:mockito-kotlin:x.x.x'
}
```

## How to use

In this library, your ViewModel classes need to extend
[AbstractViewModel](src/main/java/com/linecorp/lich/viewmodel/AbstractViewModel.kt) and have a
companion object inheriting
[ViewModelFactory](src/main/java/com/linecorp/lich/viewmodel/ViewModelFactory.kt).

This is a sample code:
```kotlin
class FooViewModel(context: Context, savedStateHandle: SavedStateHandle) : AbstractViewModel() {

    // snip...

    companion object : ViewModelFactory<FooViewModel>() {
        override fun createViewModel(context: Context, savedStateHandle: SavedStateHandle): FooViewModel =
            FooViewModel(context, savedStateHandle)
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

If you're using [AndroidX Navigation library](https://developer.android.com/guide/navigation),
you can use [Fragment.navGraphViewModel](src/main/java/com/linecorp/lich/viewmodel/ViewModelLazy.kt)
to obtain a ViewModel scoped to the entry point's navigation back stack.

```kotlin
class FooFragment : Fragment() {

    // A shared instance of FooViewModel scoped to the `foo_nav_graph` navigation graph.
    private val fooNavGraphViewModel by navGraphViewModel(FooViewModel, R.id.foo_nav_graph)

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
class BarViewModel(private val barRepository: BarRepository) : AbstractViewModel() {

    val barText: MutableLiveData<String> = MutableLiveData("")

    fun loadBarText() {
        // The launched job will be automatically cancelled when this ViewModel is destroyed.
        launch {
            val barData = barRepository.loadBarData()
            barText.value = barData.text
        }
    }

    // snip...
}
```

This feature is almost equivalent to
[Android Architecture Components' viewModelScope](https://developer.android.com/topic/libraries/architecture/coroutines#viewmodelscope),
but you can simply write `launch { ... }` instead of `viewModelScope.launch { ... }`.

## Testing

In unit tests, you can use `mockViewModel(factory) { ... }` to mock ViewModels.
The following code is an example of [mockViewModel](../viewmodel-test-mockk/src/main/java/com/linecorp/lich/viewmodel/test/mockk/Mocking.kt)
to mock the above `BarViewModel` class using [MockK](https://mockk.io/):

```kotlin
@RunWith(AndroidJUnit4::class)
class BarActivityTest {

    @After
    fun tearDown() {
        // You can omit this in Robolectric tests.
        // All ViewModel mocks are automatically cleared for every Robolectric test.
        clearAllMockViewModels()
    }

    @Test
    fun testViewBinding() {
        val mockBarText = MutableLiveData("Mocked.")
        // Set mock ViewModel for `BarViewModel`.
        val mockViewModelHandle = mockViewModel(BarViewModel) {
            every { barText } returns mockBarText
        }

        ActivityScenario.launch(BarActivity::class.java).use { scenario ->

            scenario.onActivity {
                assertTrue(mockViewModelHandle.isCreated)
            }

            onView(withId(R.id.bar_text)).check(matches(withText("Mocked.")))

            onView(withId(R.id.load_bar_button)).perform(click())

            scenario.onActivity {
                verify(exactly = 1) { mockViewModelHandle.mock.loadBarText() }
            }
        }
    }
}
```

See also
[MvvmSampleActivityTest](../sample_app/src/test/java/com/linecorp/lich/sample/mvvm/MvvmSampleActivityTest.kt)
in the sample_app module.

The [mockViewModel](../viewmodel-test-mockitokotlin/src/main/java/com/linecorp/lich/viewmodel/test/mockitokotlin/Mocking.kt)
function is also available for [Mockito-Kotlin](https://github.com/mockito/mockito-kotlin).
Here is an example using Mockito-Kotlin.

```kotlin
@RunWith(AndroidJUnit4::class)
class BarActivityTest {

    @After
    fun tearDown() {
        // You can omit this in Robolectric tests.
        // All ViewModel mocks are automatically cleared for every Robolectric test.
        clearAllMockViewModels()
    }

    @Test
    fun testViewBinding() {
        val mockBarText = MutableLiveData("Mocked.")
        // Set mock ViewModel for `BarViewModel`.
        val mockViewModelHandle = mockViewModel(BarViewModel) {
            on { barText } doReturn mockBarText
        }

        ActivityScenario.launch(BarActivity::class.java).use { scenario ->

            scenario.onActivity {
                assertTrue(mockViewModelHandle.isCreated)
            }

            onView(withId(R.id.bar_text)).check(matches(withText("Mocked.")))

            onView(withId(R.id.load_bar_button)).perform(click())

            scenario.onActivity {
                verify(mockViewModelHandle.mock, times(1)).loadBarText()
            }
        }
    }
}
```

## Working with Lich SavedState library

This library can be used in conjunction with [Lich SavedState](../savedstate) library.
The delegated properties and auto-generated `ViewModelArgs` classes provided by the Lich SavedState
library can be used in Lich ViewModel library as well.

```kotlin
@GenerateArgs
class FooViewModel(savedStateHandle: SavedStateHandle) : AbstractViewModel() {

    @Argument
    private val userName: String by savedStateHandle.required()

    @Argument(isOptional = true)
    private val tags: Array<String> by savedStateHandle.initial(arrayOf("normal"))

    @Argument
    private var attachment: Parcelable? by savedStateHandle

    @Argument
    private val message: MutableLiveData<CharSequence> by savedStateHandle.liveData()

    // snip...

    companion object : ViewModelFactory<FooViewModel>() {
        override fun createViewModel(context: Context, savedStateHandle: SavedStateHandle): FooViewModel =
            FooViewModel(savedStateHandle)
    }
}
```

```kotlin
class FooFragment : Fragment() {

    private val fooViewModel by viewModel(FooViewModel)

    // snip...
}

val fooFragment = FooFragment().also {
    // This `FooViewModelArgs` is used to initialize the `savedStateHandle` of `FooFragment.fooViewModel`.
    it.setViewModelArgs(FooViewModelArgs(userName = "John", attachment = null, message = "Hello."))
}
```

The generated `ViewModelArgs` class can be used in the argument of the `viewModel` extension as follows.

```kotlin
class FooFragment : Fragment() {

    private val fooViewModel by viewModel(FooViewModel) {
        // This `FooViewModelArgs` is used to initialize the `savedStateHandle` of `fooViewModel`.
        FooViewModelArgs(userName = "John", attachment = null, message = "Hello.").toBundle()
    }

    // snip...
}
```

## Example

[SampleViewModel](../sample_app/src/main/java/com/linecorp/lich/sample/mvvm/SampleViewModel.kt)
in the sample_app module.

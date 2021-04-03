# Lich ViewModel

[ ![Maven Central](https://badgen.net/maven/v/maven-central/com.linecorp.lich/viewmodel) ](https://search.maven.org/artifact/com.linecorp.lich/viewmodel)

Lightweight framework for managing ViewModels in the same way as [Lich Component](../component).

This library is very similar to
[Android Architecture Components's ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel),
but it has the following advantages:

- Declare and use ViewModels with simple code.
- Provide type-safe access to [saved instance state](https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate).
- Easy to mock, easy to test.

## Set up

First, add the following entries to your `build.gradle` file.

```groovy
apply plugin: 'kotlin-kapt'

dependencies {
    implementation 'com.linecorp.lich:viewmodel:x.x.x'
    kapt 'com.linecorp.lich:viewmodel-compiler:x.x.x'
}
```

For unit-testing, the `viewmodel-test` module provides [AndroidX Test](https://developer.android.com/training/testing/set-up-project)
support. (For Robolectric, see also [this document](http://robolectric.org/androidx_test/).)
In addition, helper functions to work with [MockK](https://mockk.io/) or
[Mockito-Kotlin](https://github.com/nhaarman/mockito-kotlin) are also available.

If you are using [MockK](https://mockk.io/), add the following dependencies:

```groovy
dependencies {
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

If you are using [Mockito-Kotlin](https://github.com/nhaarman/mockito-kotlin), add the following
dependencies instead:

```groovy
dependencies {
    testImplementation 'com.linecorp.lich:viewmodel-test-mockitokotlin:x.x.x'
    testImplementation 'androidx.test:runner:x.x.x'
    testImplementation 'androidx.test.ext:junit:x.x.x'
    testImplementation 'org.mockito:mockito-inline:x.x.x'
    testImplementation 'com.nhaarman.mockitokotlin2:mockito-kotlin:x.x.x'
    testImplementation 'org.robolectric:robolectric:x.x'

    androidTestImplementation 'com.linecorp.lich:viewmodel-test-mockitokotlin:x.x.x'
    androidTestImplementation 'androidx.test:runner:x.x.x'
    androidTestImplementation 'androidx.test.ext:junit:x.x.x'
    androidTestImplementation 'org.mockito:mockito-android:x.x.x'
    androidTestImplementation 'com.nhaarman.mockitokotlin2:mockito-kotlin:x.x.x'
}
```

## How to use

In this library, your ViewModel classes need to extend
[AbstractViewModel](src/main/java/com/linecorp/lich/viewmodel/AbstractViewModel.kt) and have a
companion object inheriting
[ViewModelFactory](src/main/java/com/linecorp/lich/viewmodel/ViewModelFactory.kt).

This is a sample code:
```kotlin
class FooViewModel(private val context: Context, savedState: SavedState) : AbstractViewModel() {

    // snip...

    companion object : ViewModelFactory<FooViewModel>() {
        override fun createViewModel(context: Context, savedState: SavedState): FooViewModel =
            FooViewModel(context, savedState)
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

## Access to Saved Instance States

The [SavedState](src/main/java/com/linecorp/lich/viewmodel/SavedState.kt) object passed down to
`ViewModelFactory.createViewModel(context, savedState)` provides access to
[saved instance state](https://developer.android.com/topic/libraries/architecture/saving-states).
The values set to this `savedState` are retained even after system-initiated process death.

You can access the values of `SavedState` via delegated properties like this:

```kotlin
class ParamsViewModel(private val context: Context, savedState: SavedState) : AbstractViewModel() {

    // A delegated property that accesses the SavedState value associated with "param1".
    private var param1: String? by savedState

    // A delegated property that accesses the SavedState value associated with "param2".
    // Its initial value is "foo" unless specified by the argument of `viewModel(factory)`. (See below)
    private var param2: String by savedState.initial("foo")

    // A delegated property that accesses the SavedState value associated with "param3".
    // Its initial value must be specified by the argument of `viewModel(factory)`. (See below)
    private var param3: String by savedState.required()

    // A delegated property that accesses the SavedState value associated with "param4".
    // You can access the String value via the `MutableLiveData`.
    private val param4: MutableLiveData<String> by savedState.liveData()

    // A delegated property that accesses the SavedState value associated with "param5".
    // Its initial value is "foo" unless specified by the argument of `viewModel(factory)`. (See below)
    // You can access the String value via the `MutableLiveData`.
    private val param5: MutableLiveData<String> by savedState.liveData("foo")

    // snip...
}
```

## Specify initial values of Saved Instance States

The initial values of the [SavedState](src/main/java/com/linecorp/lich/viewmodel/SavedState.kt) can
be specified by the `arguments` parameter of the functions such as
[Fragment.viewModel(factory)](src/main/java/com/linecorp/lich/viewmodel/ViewModelLazy.kt).

This library provides type-safe access to the `arguments` via code generation.
To generate an *Args* class for a ViewModel, specify
[@GenerateArgs](src/main/java/com/linecorp/lich/viewmodel/GenerateArgs.kt) and
[@Argument](src/main/java/com/linecorp/lich/viewmodel/Argument.kt) annotations like this:

```kotlin
@GenerateArgs
class FooViewModel(private val context: Context, savedState: SavedState) : AbstractViewModel() {

    @Argument
    private val userName: String by savedState.required()

    @Argument(isOptional = true)
    private var amount: Int by savedState.initial(0)

    @Argument(isOptional = true)
    private val message: MutableLiveData<String> by savedState.liveData()

    // snip...
}
```

Then, the following class is generated.

```kotlin
public class FooViewModelArgs(
    public val userName: String,
    public val amount: Int? = null,
    public val message: String? = null
) : ViewModelArgs {
    public override fun toBundle(): Bundle = Bundle().also {
        it.putString("userName", this.userName)
        if (this.amount != null) it.putInt("amount", this.amount)
        if (this.message != null) it.putString("message", this.message)
    }
}
```

You can use the *Args* object as the `arguments` parameter of the `viewModel` functions like this:

```kotlin
class FooFragment : Fragment() {

    private val fooViewModel by viewModel(FooViewModel) {
        FooViewModelArgs(userName = "foo", amount = 100, message = "Hello.").toBundle()
    }

    // snip...
}
```

Or, you can use [Intent.putViewModelArgs](src/main/java/com/linecorp/lich/viewmodel/ViewModelArgs.kt)
/ [Fragment.setViewModelArgs](src/main/java/com/linecorp/lich/viewmodel/ViewModelArgs.kt) to set
the *Args* object to the Activity / Fragment that hosts the ViewModel.

```kotlin
class FooFragment : Fragment() {

    private val fooViewModel by viewModel(FooViewModel)

    // snip...
}

val fooFragment = FooFragment().also {
    // This `FooViewModelArgs` is used to initialize the `SavedState` of `FooFragment.fooViewModel`.
    it.setViewModelArgs(FooViewModelArgs(userName = "foo", amount = 100, message = "Hello."))
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

## Example

[SampleViewModel](../sample_app/src/main/java/com/linecorp/lich/sample/mvvm/SampleViewModel.kt)
in the sample_app module.

## Testing

The `viewmodel-test` module provides useful APIs for testing.

You can use [createSavedStateForTesting](../viewmodel-test/src/main/java/com/linecorp/lich/viewmodel/test/SavedStates.kt)
for unit tests of ViewModels.

```kotlin
@RunWith(AndroidJUnit4::class)
class FooViewModelTest {

    @Test
    fun testFoo() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val savedState = createSavedStateForTesting(
            FooViewModelArgs(userName = "testUser")
        )
        val fooViewModel = FooViewModel(context, savedState)

        // snip...
    }
}
```

On the other hand, you can use `mockViewModel(factory) { ... }` to mock ViewModels.
Here is an example of [mockViewModel](../viewmodel-test-mockk/src/main/java/com/linecorp/lich/viewmodel/test/mockk/Mocking.kt)
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
function is also available for [Mockito-Kotlin](https://github.com/nhaarman/mockito-kotlin).
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

# Lich SavedState

[ ![Maven Central](https://badgen.net/maven/v/maven-central/com.linecorp.lich/savedstate) ](https://search.maven.org/artifact/com.linecorp.lich/savedstate)

A library that provides type-safe access to [saved instance state](https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate).

This library provides the following features.

- Provides delegated properties for type-safe access to [ViewModel's SavedStateHandle](https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate).
- Generates [ViewModelArgs](src/main/java/com/linecorp/lich/savedstate/ViewModelArgs.kt) classes to initialize the `SavedStateHandle` in a type-safe manner.

## Set up

This library officially supports [KSP](https://github.com/google/ksp).
Please add the following entries to your `settings.gradle` and `build.gradle` files.

```groovy
// settings.gradle
pluginManagement {
    plugins {
        id 'com.google.devtools.ksp' version '1.5.31-1.0.0'
    }
}
```

```groovy
// build.gradle
plugins {
    id 'com.google.devtools.ksp'
}

dependencies {
    implementation 'com.linecorp.lich:savedstate:x.x.x'
    ksp 'com.linecorp.lich:savedstate-compiler:x.x.x'
}
```

Alternatively, you can use [kapt](https://kotlinlang.org/docs/kapt.html) instead of KSP.
In that case, change `build.gradle` as follows.

```groovy
// build.gradle
plugins {
    id 'org.jetbrains.kotlin.kapt'
}

dependencies {
    implementation 'com.linecorp.lich:savedstate:x.x.x'
    kapt 'com.linecorp.lich:savedstate-compiler:x.x.x'
}
```

## Delegated properties for SavedStateHandle

This library provides delegated properties for [SavedStateHandle](https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate).

You can access the values of `SavedStateHandle` via delegated properties as follows.

```kotlin
class SampleViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    // A delegated property that accesses the value associated with "param1".
    // This is equivalent to the code below.
    // ```
    // private var param1: String?
    //     get() = savedStateHandle["param1"]
    //     set(value) { savedStateHandle["param1"] = value }
    // ```
    private var param1: String? by savedStateHandle

    // A delegated property that accesses the value associated with "param2".
    // The value is initialized with "abc" unless specified by a `ViewModelArgs`.
    // This is equivalent to the code below.
    // ```
    // init {
    //     if ("param2" !in savedStateHandle) {
    //         savedStateHandle["param2"] = "abc"
    //     }
    // }
    //
    // private var param2: String
    //     get() = savedStateHandle["param2"]!!
    //     set(value) { savedStateHandle["param2"] = value }
    // ```
    private var param2: String by savedStateHandle.initial("abc")

    // A delegated property that accesses the value associated with "param3".
    // The value must be initialized by a `ViewModelArgs`, otherwise IllegalStateException is thrown.
    // This is equivalent to the code below.
    // ```
    // init {
    //     check("param3" in savedStateHandle) { "param3 is not specified in the arguments." }
    // }
    //
    // private var param3: String
    //     get() = savedStateHandle["param3"]!!
    //     set(value) { savedStateHandle["param3"] = value }
    // ```
    private var param3: String by savedStateHandle.required()

    // A delegated property of a `MutableLiveData` that accesses the value associated with "param4".
    // This is equivalent to the code below.
    // ```
    // private val param4: MutableLiveData<String> = savedStateHandle.getLiveData("param4")
    // ```
    private val param4: MutableLiveData<String> by savedStateHandle.liveData()

    // A delegated property of a `MutableLiveData` that accesses the value associated with "param5".
    // The value is initialized with "abc" unless specified by a `ViewModelArgs`.
    // This is equivalent to the code below.
    // ```
    // private val param5: MutableLiveData<String> = savedStateHandle.getLiveData("param5", "abc")
    // ```
    private val param5: MutableLiveData<String> by savedStateHandle.liveDataWithInitial("abc")

    // snip...
}
```

## Initialize SavedStateHandle with auto-generated ViewModelArgs classes

This library can generate `ViewModelArgs` classes to initialize `SavedStateHandle`.

To generate a `ViewModelArgs` class, specify
[@GenerateArgs](src/main/java/com/linecorp/lich/savedstate/GenerateArgs.kt) and
[@Argument](src/main/java/com/linecorp/lich/savedstate/Argument.kt) annotations to a ViewModel class as follows.

```kotlin
@GenerateArgs
class FooViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    @Argument
    private val userName: String by savedStateHandle.required()

    @Argument(isOptional = true)
    private val tags: Array<String> by savedStateHandle.initial(arrayOf("normal"))

    @Argument
    private var attachment: Parcelable? by savedStateHandle

    @Argument
    private val message: MutableLiveData<CharSequence> by savedStateHandle.liveData()

    // snip...
}
```

Then, the following class will be generated.

```kotlin
public class FooViewModelArgs(
    public val userName: String,
    public val tags: Array<String>? = null,
    public val attachment: Parcelable?,
    public val message: CharSequence
) : ViewModelArgs {
    public override fun toBundle(): Bundle = Bundle().also {
        it.putString("userName", this.userName)
        if (this.tags != null) it.putSerializable("tags", this.tags)
        it.putParcelable("attachment", this.attachment)
        it.putCharSequence("message", this.message)
    }
}
```

You can use [Intent.putViewModelArgs](src/main/java/com/linecorp/lich/savedstate/ViewModelArgs.kt)
/ [Fragment.setViewModelArgs](src/main/java/com/linecorp/lich/savedstate/ViewModelArgs.kt) to set
a `ViewModelArgs` object to the Activity / Fragment that hosts the ViewModel.

```kotlin
class FooFragment : Fragment() {

    private val fooViewModel: FooViewModel by viewModels()

    // snip...
}

val fooFragment = FooFragment().also {
    // This `FooViewModelArgs` is used to initialize the `savedStateHandle` of `FooFragment.fooViewModel`.
    it.setViewModelArgs(FooViewModelArgs(userName = "John", attachment = null, message = "Hello."))
}
```

Alternatively, you can use the generated `ViewModelArgs` class in the argument of the `viewModels`
extension function, as shown below.

```kotlin
class FooFragment : Fragment() {

    private val fooViewModel: FooViewModel by viewModels {
        SavedStateViewModelFactory(
            requireActivity().application,
            this,
            FooViewModelArgs(userName = "John", attachment = null, message = "Hello.").toBundle()
        )
    }

    // snip...
}
```

## Testing

You can use [createSavedStateHandleForTesting](src/main/java/com/linecorp/lich/savedstate/SavedStateTesting.kt)
to initialize `SavedStateHandle` for unit tests of ViewModels.

```kotlin
@RunWith(AndroidJUnit4::class)
class FooViewModelTest {

    @Test
    fun testFooViewModel() {
        val savedStateHandle = createSavedStateHandleForTesting(
            FooViewModelArgs(userName = "John", attachment = null, message = "Hello.")
        )
        val fooViewModel = FooViewModel(savedStateHandle)

        // snip...
    }
}
```

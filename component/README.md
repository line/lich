# Lich Component

[ ![Maven Central](https://badgen.net/maven/v/maven-central/com.linecorp.lich/component) ](https://search.maven.org/artifact/com.linecorp.lich/component)

Lightweight framework for managing singleton components on Android apps.

This is **NOT** a DI (Dependency Injection) framework.
That is, this library uses no configuration files, no annotations (except for AutoService) and no DSL.
Instead, you can write dependencies programmatically.
(Technically speaking, this library is a variant of *Service Locator*.)

This library offers the following benefits:

- Easy to learn
  - You only need to know about Kotlin's companion object, extension functions and delegated properties.
- Super fast
  - Runtime performance is almost equivalent to Dagger2.
- Extremely low footprint
  - Only ~3kB in classes after R8 optimization.
- Zero configuration
  - No global settings or initialization code are needed.
- Ensures completeness without building
  - In most cases, you can check the completeness of dependencies without building the project.
- Works well in Dynamic Feature Modules
  - Resolves dependencies across modules without reflection overhead.
- Easy to write lazy acquisition
  - Lazy acquisition can be simply written as a delegated property of Kotlin.
- Easy to mock in unit tests
  - You can mock any components freely with simple code.
- Extensive debugging features
  - Provides various debug logs and the ability to override dependencies at runtime.

## Set up

First, add the following entries to your `build.gradle` file.

```groovy
dependencies {
    implementation 'com.linecorp.lich:component:x.x.x'

    // Optional: Enables diagnostic features for debug builds.
    debugRuntimeOnly 'com.linecorp.lich:component-debug:x.x.x'
}
```

For unit-testing, the `component-test` module provides [AndroidX Test](https://developer.android.com/training/testing/set-up-project)
support. (For Robolectric, see also [this document](http://robolectric.org/androidx_test/).)
In addition, helper functions to work with [MockK](https://mockk.io/) or
[Mockito-Kotlin](https://github.com/mockito/mockito-kotlin) are also available.

If you are using [MockK](https://mockk.io/), add the following dependencies:

```groovy
dependencies {
    testImplementation 'com.linecorp.lich:component-test-mockk:x.x.x'
    testImplementation 'androidx.test:runner:x.x.x'
    testImplementation 'androidx.test.ext:junit:x.x.x'
    testImplementation 'io.mockk:mockk:x.x.x'
    testImplementation 'org.robolectric:robolectric:x.x'

    androidTestImplementation 'com.linecorp.lich:component-test-mockk:x.x.x'
    androidTestImplementation 'androidx.test:runner:x.x.x'
    androidTestImplementation 'androidx.test.ext:junit:x.x.x'
    androidTestImplementation 'io.mockk:mockk-android:x.x.x'
}
```

If you are using [Mockito-Kotlin](https://github.com/mockito/mockito-kotlin), add the following
dependencies instead:

```groovy
dependencies {
    testImplementation 'com.linecorp.lich:component-test-mockitokotlin:x.x.x'
    testImplementation 'androidx.test:runner:x.x.x'
    testImplementation 'androidx.test.ext:junit:x.x.x'
    testImplementation 'org.mockito:mockito-inline:x.x.x'
    testImplementation 'org.mockito.kotlin:mockito-kotlin:x.x.x'
    testImplementation 'org.robolectric:robolectric:x.x'

    androidTestImplementation 'com.linecorp.lich:component-test-mockitokotlin:x.x.x'
    androidTestImplementation 'androidx.test:runner:x.x.x'
    androidTestImplementation 'androidx.test.ext:junit:x.x.x'
    androidTestImplementation 'org.mockito:mockito-android:x.x.x'
    androidTestImplementation 'org.mockito.kotlin:mockito-kotlin:x.x.x'
}
```

## How to use

You can declare a class as a "component" by implementing its companion object that inherits
[ComponentFactory](src/main/java/com/linecorp/lich/component/ComponentFactory.kt).

```kotlin
class FooComponent {

    // snip...

    companion object : ComponentFactory<FooComponent>() {
        override fun createComponent(context: Context): FooComponent =
            FooComponent()
    }
}
```

The singleton instance of each component can be obtained via
[Context.getComponent](src/main/java/com/linecorp/lich/component/Components.kt).
Therefore, the singleton instance of `FooComponent` can be obtained by this code:

```kotlin
val fooComponent = context.getComponent(FooComponent)
```

You can also obtain components *lazily* using
[Context.component](src/main/java/com/linecorp/lich/component/ComponentLazy.kt) like this:

```kotlin
val fooComponent by context.component(FooComponent)
```

Lazy initialization is useful in Activity.

```kotlin
class FooActivity : Activity() {

    private val fooComponent by component(FooComponent)

    // snip...
}
```

For Fragment, you can use
[Fragment.component](src/main/java/com/linecorp/lich/component/ComponentLazy.kt) as well.

```kotlin
class FooFragment : Fragment() {

    private val fooComponent by component(FooComponent)

    // snip...
}
```

## Example

This is a sample code to declare a
[Room](https://developer.android.com/topic/libraries/architecture/room) database component:

```kotlin
@Database(entities = [Foo::class], version = 1)
abstract class FooDatabase : RoomDatabase() {

    abstract val fooDao: FooDao

    companion object : ComponentFactory<FooDatabase>() {
        override fun createComponent(context: Context): FooDatabase =
            Room.databaseBuilder(context, FooDatabase::class.java, "foo_db").build()
    }
}
```

Another example to declare a component that depends on `FooDatabase` is like this:

```kotlin
// To guarantee singleton, constructors of components should be private or visible only for testing.
class FooRepository @VisibleForTesting internal constructor(
    private val fooDao: FooDao
) {

    suspend fun findFoo(key: String): Foo {
        return fooDao.find(key)
    }

    suspend fun updateFoo(foo: Foo) {
        fooDao.update(foo)
    }

    companion object : ComponentFactory<FooRepository>() {
        override fun createComponent(context: Context): FooRepository {
            // Obtain the instance of FooDatabase.
            val fooDatabase = context.getComponent(FooDatabase)
            return FooRepository(fooDatabase.fooDao)
        }
    }
}
```

To use components from a regular (non-singleton) class, give
[Context](https://developer.android.com/reference/android/content/Context) to the constructor.

```kotlin
class SaveFooUseCase(context: Context) {

    // Obtains FooRepository lazily.
    private val fooRepository by context.component(FooRepository)

    suspend fun getFooFromStorage(key: String): Foo {
        return fooRepository.findFoo(key)
    }

    suspend fun storeFooIntoStorage(foo: Foo) {
        fooRepository.updateFoo(foo)
    }
}
```

## Multi-module support

This library provides two methods for resolving dependencies in multi-module projects.

We assume that there are two modules "base" and "foo" such that "foo" depends on "base".
If you want to call some function of "foo" from "base", define a Facade interface like this:

```kotlin
// "base" module
package module.base.facades

/**
 * The Facade of the "foo" module.
 * https://en.wikipedia.org/wiki/Facade_pattern
 *
 * This component defines the API of the "foo" module.
 */
interface FooModuleFacade {

    fun launchFooActivity()

    companion object : ComponentFactory<FooModuleFacade>() {
        override fun createComponent(context: Context): FooModuleFacade =
            TODO()
    }
}
```

```kotlin
// Call some feature of the "foo" module.
val fooModuleFacade = context.getComponent(FooModuleFacade)
fooModuleFacade.launchFooActivity()
```

Then, use one of the following two methods to create the implementation of `FooModuleFacade`.

### delegateCreation()

[ComponentFactory.delegateCreation](src/main/java/com/linecorp/lich/component/ComponentFactory.kt)
delegates the creation of a component to a
[DelegatedComponentFactory](src/main/java/com/linecorp/lich/component/DelegatedComponentFactory.kt)
specified by name.

Implement a subclass of `DelegatedComponentFactory` in the "foo" module. Then, specify the class
name to `delegateCreation()` like this:

```kotlin
// "base" module
package module.base.facades

interface FooModuleFacade {

    fun launchFooActivity()

    companion object : ComponentFactory<FooModuleFacade>() {
        override fun createComponent(context: Context): FooModuleFacade =
            delegateCreation(context, "module.foo.FooModuleFacadeFactory")
    }
}
```

```kotlin
// "foo" module
package module.foo

// The class inheriting DelegatedComponentFactory must have a public empty constructor.
class FooModuleFacadeFactory: DelegatedComponentFactory<FooModuleFacade>() {
    override fun createComponent(context: Context): FooModuleFacade =
        FooModuleFacadeImpl(context)
}

internal class FooModuleFacadeImpl(private val context: Context) : FooModuleFacade {

    override fun launchFooActivity() {
        // snip...
    }
}
```

See [FooFeatureFacade](../sample_app/src/main/java/com/linecorp/lich/sample/feature/foo/FooFeatureFacade.kt)
and [FooFeatureFacadeFactory](../sample_feature/src/main/java/com/linecorp/lich/sample/feature/foo/FooFeatureFacadeFactory.kt)
for the actual code.

### delegateToServiceLoader()

[ComponentFactory.delegateToServiceLoader](src/main/java/com/linecorp/lich/component/ComponentFactory.kt)
delegates the creation of a component to [ServiceLoader](https://developer.android.com/reference/java/util/ServiceLoader).

We recommend using `delegateToServiceLoader()` with the
[AutoService](https://github.com/google/auto/tree/master/service) library.
So, please add the following entries to `build.gradle` of the "foo" module first.

```groovy
apply plugin: 'kotlin-kapt'

dependencies {
    compileOnly 'com.google.auto.service:auto-service-annotations:x.x'
    kapt 'com.google.auto.service:auto-service:x.x'
}
```

Then, implement `FooModuleFacadeImpl` class in the "foo" module. The class must have a public empty
constructor and be annotated as `@AutoService(FooModuleFacade::class)`.
If the class requires the Application Context, make the class implement the
[ServiceLoaderComponent](src/main/java/com/linecorp/lich/component/ServiceLoaderComponent.kt)
interface, and override the `init(context)` function.

Finally, call `delegateToServiceLoader(context)` from `FooModuleFacade` like this:

```kotlin
// "base" module
package module.base.facades

interface FooModuleFacade {

    fun launchFooActivity()

    companion object : ComponentFactory<FooModuleFacade>() {
        override fun createComponent(context: Context): FooModuleFacade =
            delegateToServiceLoader(context)
    }
}
```

```kotlin
// "foo" module
package module.foo

// Declare this class as an implementation of `FooModuleFacade`.
@AutoService(FooModuleFacade::class)
class FooModuleFacadeImpl : FooModuleFacade, ServiceLoaderComponent {

    private lateinit var context: Context

    // The class instantiated by `delegateToServiceLoader` must have a public empty constructor.
    // If the class requires a `context`, implement the `ServiceLoaderComponent` interface and
    // override the `init(context)` function.
    override fun init(context: Context) {
        this.context = context
    }

    override fun launchFooActivity() {
        // snip...
    }
}
```

You can have different implementation classes with the same `@AutoService(FooModuleFacade::class)`
annotation. In such a case, the class with the largest `ServiceLoaderComponent.loadPriority` value
is selected as the actual implementation class of `FooModuleFacade`. This is useful when you want to
switch features depending on the project configuration.

If you are using R8 (included in Android Gradle Plugin 3.5.0+) with code shrinking and optimizations
enabled, the R8 optimization gets rid of reflection entirely in the final byte code. For details,
please refer [this article](https://medium.com/androiddevelopers/patterns-for-accessing-code-from-dynamic-feature-modules-7e5dca6f9123).

See [BarFeatureFacade](../sample_app/src/main/java/com/linecorp/lich/sample/feature/bar/BarFeatureFacade.kt)
and [BarFeatureFacadeImpl](../sample_feature/src/main/java/com/linecorp/lich/sample/feature/bar/BarFeatureFacadeImpl.kt)
for the actual code.

## Testing

If the `component-test` module is in the runtime classpath, every component is tied to an
`applicationContext`. And, a different instance of component is created for each `applicationContext`.
This is useful for Robolectric tests, because Robolectric recreates `applicationContext` for each test.
It means all components are automatically reset for every Robolectric test.

The `component-test` module also provides APIs for tests.
For example, you can use [setMockComponent](../component-test/src/main/java/com/linecorp/lich/component/test/ComponentMocks.kt)
to mock components like this:

```kotlin
setMockComponent(FooComponent, createMockFoo())
```

If you are using [MockK](https://mockk.io/),
[mockComponent](../component-test-mockk/src/main/java/com/linecorp/lich/component/test/mockk/Mocking.kt)
is also a useful function. Here is an example for testing the above `SaveFooUseCase` class.

```kotlin
@RunWith(AndroidJUnit4::class)
class SaveFooUseCaseTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testGetFooFromStorage() {
        val expected = Foo()
        val mockFooRepository = mockComponent(FooRepository) {
            coEvery { findFoo(any()) } returns expected
        }
        val saveFooUseCase = SaveFooUseCase(context)

        val actual = runBlocking { saveFooUseCase.getFooFromStorage("key") }

        assertEquals(expected, actual)
        coVerify { mockFooRepository.findFoo(eq("key")) }
    }
}
```

See also
[CounterUseCaseTest](../sample_app/src/test/java/com/linecorp/lich/sample/mvvm/CounterUseCaseTest.kt).

The [mockComponent](../component-test-mockitokotlin/src/main/java/com/linecorp/lich/component/test/mockitokotlin/Mocking.kt)
function is also available for [Mockito-Kotlin](https://github.com/mockito/mockito-kotlin).
Here is an example using Mockito-Kotlin.

```kotlin
@RunWith(AndroidJUnit4::class)
class SaveFooUseCaseTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testGetFooFromStorage() {
        val expected = Foo()
        val mockFooRepository = mockComponent(FooRepository) {
            onBlocking { findFoo(any()) } doReturn expected
        }
        val saveFooUseCase = SaveFooUseCase(context)

        val actual = runBlocking { saveFooUseCase.getFooFromStorage("key") }

        assertEquals(expected, actual)
        verifyBlocking(mockFooRepository) { findFoo(eq("key")) }
    }
}
```

## Declare 3rd-party classes as components

If you want to declare a 3rd-party class as a "component", implement a top-level object instead of
a companion object.

For example, you should share a single [OkHttpClient](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/)
instance across the app because each `OkHttpClient` instance holds its own connection pool and thread pools.
The application-wide singleton of `OkHttpClient` can be declared as follows:

```kotlin
object GlobalOkHttpClient : ComponentFactory<OkHttpClient>() {
    override fun createComponent(context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
        // Apply custom settings.
        builder.addInterceptor(LoggingInterceptor())
        return builder.build()
    }
}
```

Then, you can get the singleton as follows:

```kotlin
val okHttpClient: OkHttpClient = context.getComponent(GlobalOkHttpClient)
```

See also [the sample code](../sample_app/src/main/java/com/linecorp/lich/sample/GlobalOkHttpClient.kt).

## Resolve circular dependencies

For example, the following code has a circular dependency between `ComponentX` and `ComponentY`.

```kotlin
class ComponentX(context: Context) {

    private val componentY = context.getComponent(ComponentY)

    companion object : ComponentFactory<ComponentX>() {
        override fun createComponent(context: Context): ComponentX =
            ComponentX(context)
    }
}

class ComponentY(context: Context) {

    private val componentX = context.getComponent(ComponentX)

    companion object : ComponentFactory<ComponentY>() {
        override fun createComponent(context: Context): ComponentY =
            ComponentY(context)
    }
}
```

If there is the `component-debug` module in the runtime classpath, it detects the circular dependency
and throws an exception like this:

```text
java.lang.IllegalStateException: Detected circular dependency!: [com.example.ComponentX$Companion@44028ab5, com.example.ComponentY$Companion@60562d2d, com.example.ComponentX$Companion@44028ab5]
```

In such a case, you can use *lazy acquisition* to resolve the issue.

```kotlin
class ComponentX(context: Context) {

    private val componentY by context.component(ComponentY)

    companion object : ComponentFactory<ComponentX>() {
        override fun createComponent(context: Context): ComponentX =
            ComponentX(context)
    }
}

class ComponentY(context: Context) {

    private val componentX by context.component(ComponentX)

    companion object : ComponentFactory<ComponentY>() {
        override fun createComponent(context: Context): ComponentY =
            ComponentY(context)
    }
}
```

## Debugging features

The `component-debug` module provides some useful features for debugging.

```groovy
dependencies {
    implementation 'com.linecorp.lich:component:x.x.x'
    debugImplementation 'com.linecorp.lich:component-debug:x.x.x'
}
```

If there is the `component-debug` module in the runtime classpath, this library outputs some
diagnostic logs like this:

```text
I/DebugComponentProvider: Created com.example.app.FooComponent@8488aeb in 20 ms.
```

As mentioned above, it also checks circular dependencies of components.

The `component-debug` module also provides an API for debugging components.
For example, you can use
[DebugComponentManager](../component-debug/src/main/java/com/linecorp/lich/component/debug/DebugComponentManager.kt)
to modify components directly.

```kotlin
interface FooComponent {

    fun doSomething()

    companion object : ComponentFactory<FooComponent>() {
        override fun createComponent(context: Context): FooComponent =
            // snip...
    }
}
```

```kotlin
fun initFooComponentForDebug(context: Context) {
    val baseFoo = context.getComponent(FooComponent)
    if (baseFoo is FooComponentForDebug) {
        throw IllegalStateException("FooComponentForDebug is already set.")
    }
    context.debugComponentManager.setComponent(FooComponent, FooComponentForDebug(baseFoo))
}

private class FooComponentForDebug(private val baseFoo: FooComponent) : FooComponent {

    override fun doSomething() {
        showDebugInformation()
        baseFoo.doSomething()
    }

    private fun showDebugInformation() {
        // ...
    }
}
```

Note that modifications to
[DebugComponentManager](../component-debug/src/main/java/com/linecorp/lich/component/debug/DebugComponentManager.kt)
don't affect already acquired components.
So, the above `initFooComponentForDebug(context)` should be called prior to any acquisition of
`FooComponent`.

## FAQ

### Why doesn't this library support creation or mocking of non-singleton objects?

You can use *default arguments* instead.

```kotlin
class FooController(
    context: Context,
    private val barHelper: BarHelper = BarHelper(context)
) {
    private val bazComponent by context.component(BazComponent)

    // ...
}

class BarHelper(private val context: Context) {
    // ...
}
```

For the above code, you can mock `barHelper` and `bazComponent` with the following code:

```kotlin
@RunWith(AndroidJUnit4::class)
class FooControllerTest {

    private lateinit var context: Context

    private lateinit var mockBarHelper: BarHelper

    private lateinit var mockBazComponent: BazComponent

    private lateinit var fooController: FooController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        mockBarHelper = mock()
        mockBazComponent = mockComponent(BazComponent)

        fooController = FooController(context, mockBarHelper)
    }

    @Test
    fun testFoo() {
        // ...
    }
}
```

### Can we use "scoped" objects?

Use ViewModels instead. For most Android apps, ViewModels meet the requirements for scoped objects.

[Lich ViewModel](../viewmodel) allows you to use ViewModels in a similar way to this library.
It also provides sophisticated access to
[saved instance state](https://developer.android.com/topic/libraries/architecture/saving-states).

### Why is `getComponent(factory)` implemented as an extension of `Context`? In other words, why is a Context required to get components?

There are two reasons.

The first is to follow the standard Android architecture guidelines.

> [https://developer.android.com/reference/android/app/Application](https://developer.android.com/reference/android/app/Application)
>
> There is normally no need to subclass Application. In most situations, static singletons can
> provide the same functionality in a more modular way. If your singleton needs a global context
> (for example to register broadcast receivers), include Context.getApplicationContext() as a
> Context argument when invoking your singleton's getInstance() method.

Singleton's `getInstance()` methods usually take a `Context` as an argument.
So, components of this library are also acquired with a `Context`.

The second reason is to force the correct use of Contexts.

Generally, there are two types of Android Context: UI and non-UI. You should use these Contexts properly.
(cf. [Mastering Android context](https://www.freecodecamp.org/news/mastering-android-context-7055c8478a22/))

Suppose `getComponent(factory)` was implemented as a top-level function.
In that case, you can easily access the Application Context (it is a non-UI Context) from **anywhere** like this:

```kotlin
class LeakContextComponent(val context: Context) {

    companion object : ComponentFactory<LeakContextComponent>() {
        override fun createComponent(context: Context): LeakContextComponent =
            LeakContextComponent(context)
    }
}

val context = getComponent(LeakContextComponent).context
```

As a result, it might cause mistakes that the Application Context is wrongly used where UI Contexts should be used.
To avoid such mistakes, it is better not to allow getting components without a `Context`, we think.

### But I'm really frustrated that a Context is always required to get components.

Okay. Then, implement the `GlobalContext` object as follows:

```kotlin
object GlobalContext {
    private lateinit var application: Application

    fun setup(application: Application) {
        this.application = application
    }

    fun <T : Any> getComponent(factory: ComponentFactory<T>): T =
        application.getComponent(factory)

    fun <T : Any> component(factory: ComponentFactory<T>): Lazy<T> =
        componentLazy(factory) { application }
}

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalContext.setup(this)
    }
}
```

Now, you can get components like this:

```kotlin
val eagerFooComponent: FooComponent = GlobalContext.getComponent(FooComponent)

val lazyFooComponent: FooComponent by GlobalContext.component(FooComponent)
```

Of course, as mentioned above, please be careful not to expose the `Context` outside components.

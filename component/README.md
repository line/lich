# Lich Component

[ ![Download](https://api.bintray.com/packages/line/lich/component/images/download.svg) ](https://bintray.com/line/lich/component/_latestVersion)

Lightweight framework for managing singleton components on Android apps.

This is **NOT** a DI (Dependency Injection) framework.
That is, this framework uses no configuration files, no annotations and no DSL.
Instead, you can write dependencies programmatically.

By using this framework, you can gain the following benefits:

- Declare and use singleton instances with simple code.
- Use mock instances in unit tests easily.
- Divide dependencies in a multi-module project.

## Set up

First, add the following entries to your `build.gradle` file.

```groovy
dependencies {
    implementation 'com.linecorp.lich:component:x.x.x'

    // Optional: Enables debugging features for debug builds.
    debugImplementation 'com.linecorp.lich:component-debug:x.x.x'
}
```

For unit-testing, the `component-test` module provides [AndroidX Test](https://developer.android.com/training/testing/set-up-project)
support. (For Robolectric, see also [this document](http://robolectric.org/androidx_test/).)
And, it also provides helper functions to work with [Mockito-Kotlin](https://github.com/nhaarman/mockito-kotlin).
To use these features, add the following dependencies:

```groovy
dependencies {
    testImplementation 'com.linecorp.lich:component-test:x.x.x'
    testImplementation 'androidx.test:runner:x.x.x'
    testImplementation 'androidx.test.ext:junit:x.x.x'
    testImplementation 'org.mockito:mockito-inline:x.x.x'
    testImplementation 'com.nhaarman.mockitokotlin2:mockito-kotlin:x.x.x'
    testImplementation 'org.robolectric:robolectric:x.x'

    androidTestImplementation 'com.linecorp.lich:component-test:x.x.x'
    androidTestImplementation 'androidx.test:runner:x.x.x'
    androidTestImplementation 'androidx.test.ext:junit:x.x.x'
    androidTestImplementation 'org.mockito:mockito-android:x.x.x'
    androidTestImplementation 'com.nhaarman.mockitokotlin2:mockito-kotlin:x.x.x'
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

## Testing

If the `component-test` module is in the runtime classpath, every component is tied to an
`applicationContext`. And, a different instance of component is created for each `applicationContext`.
This is useful for Robolectric tests, because Robolectric recreates `applicationContext` for each test.
It means all components are automatically reset for every Robolectric test.

The `component-test` module also provides APIs to mock components for tests.
Here is an example of
[mockComponent](../component-test/src/main/java/com/linecorp/lich/component/test/MockitoComponentMocks.kt)
function for testing the above `SaveFooUseCase`.

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
See also
[CounterUseCaseTest](../sample_app/src/test/java/com/linecorp/lich/sample/mvvm/CounterUseCaseTest.kt).

## Multi-module support

This library provides two methods for splitting dependencies in multi-module projects.

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

`ServiceLoader` instantiates a class specified in the `META-INF/services/<binary name of the component class>`
Java resource file. Then, `delegateToServiceLoader()` calls its `init(context)` function if it implements
[ServiceLoaderComponent](src/main/java/com/linecorp/lich/component/ServiceLoaderComponent.kt) interface.

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

// The class instantiated by ServiceLoader must have a public empty constructor.
class FooModuleFacadeImpl : FooModuleFacade, ServiceLoaderComponent {

    private lateinit var context: Context

    override fun init(context: Context) {
        this.context = context
    }

    override fun launchFooActivity() {
        // snip...
    }
}
```

```text
# META-INF/services/module.base.facades.FooModuleFacade
module.foo.FooModuleFacadeImpl
```

You can place ServiceLoader resource files with different implementation classes in multiple
modules. In such a case, the class with the largest `ServiceLoaderComponent.loadPriority` value is
selected as the actual implementation class of the component. This is useful when you want to
switch features depending on the project configuration.

If you are using R8 (included in Android Gradle Plugin 3.5.0+) with code shrinking and optimizations
enabled, the R8 optimization gets rid of reflection entirely in the final byte code. For details,
please refer [this article](https://medium.com/androiddevelopers/patterns-for-accessing-code-from-dynamic-feature-modules-7e5dca6f9123).

See [BarFeatureFacade](../sample_app/src/main/java/com/linecorp/lich/sample/feature/bar/BarFeatureFacade.kt),
[BarFeatureFacadeImpl](../sample_feature/src/main/java/com/linecorp/lich/sample/feature/bar/BarFeatureFacadeImpl.kt)
and [the resource file](../sample_feature/src/main/resources/META-INF/services/com.linecorp.lich.sample.feature.bar.BarFeatureFacade)
for the actual code.

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

In addition, you can use
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

**Q.** Why doesn't this library support creation or mocking of non-singleton objects?

**A.** You can use *default arguments* instead.

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

    private lateinit var barHelper: BarHelper

    private lateinit var bazComponent: BazComponent

    private lateinit var fooController: FooController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        barHelper = mock()
        bazComponent = mockComponent(BazComponent)

        fooController = FooController(context, barHelper)
    }

    @Test
    fun testFoo() {
        // ...
    }
}
```

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

Then, make your `Application` class implement
[ComponentProviderOwner](src/main/java/com/linecorp/lich/component/provider/ComponentProviderOwner.kt)
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

class MyApplication : Application(), ComponentProviderOwner {

    override val componentProvider: ComponentProvider = ComponentProvider()

    // snip...
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

You can mock components using
[mockComponent](../component-test/src/main/java/com/linecorp/lich/component/test/MockitoComponentMocks.kt)
function. Here is an example for testing the above `SaveFooUseCase`.

```kotlin
@RunWith(AndroidJUnit4::class)
class SaveFooUseCaseTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // You can omit this in Robolectric tests, because Robolectric recreates
        // `applicationContext` for each test.
        clearAllComponents()
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

You can *delegate* the creation of a component to a
[DelegatedComponentFactory](src/main/java/com/linecorp/lich/component/DelegatedComponentFactory.kt)
specified by name. It can be used to divide dependencies in a multi-module project.

Assumes that there are two modules "base" and "foo" such that "foo" depends on "base".
If you want to call some function of "foo" from "base", implement like this:

```kotlin
// "base" module
package sample.feature.foo

/**
 * The Facade of the "foo" feature.
 * https://en.wikipedia.org/wiki/Facade_pattern
 *
 * This component defines the API of the "foo" feature.
 */
interface FooFeatureFacade {
    /**
     * Launches `FooFeatureActivity`.
     */
    fun launchFooFeatureActivity()

    companion object : ComponentFactory<FooFeatureFacade>() {
        override fun createComponent(context: Context): FooFeatureFacade =
            delegateCreation(context, "sample.feature.foo.FooFeatureFacadeFactory")
    }
}
```

```kotlin
// "foo" module
package sample.feature.foo

class FooFeatureFacadeFactory : DelegatedComponentFactory<FooFeatureFacade>() {
    override fun createComponent(context: Context): FooFeatureFacade =
        FooFeatureFacadeImpl(context)
}

/**
 * The implementation of `FooFeatureFacade`.
 */
internal class FooFeatureFacadeImpl(private val context: Context) : FooFeatureFacade {

    override fun launchFooFeatureActivity() {
        // snip...
    }
}
```

See [FooFeatureFacade](../sample_app/src/main/java/com/linecorp/lich/sample/feature/foo/FooFeatureFacade.kt)
and [FooFeatureFacadeFactory](../sample_feature/src/main/java/com/linecorp/lich/sample/feature/foo/FooFeatureFacadeFactory.kt)
for the actual code.

## Debugging features

The `component-debug` module provides some useful features for debugging.

```groovy
debugImplementation 'com.linecorp.lich:component-debug:x.x.x'
```

If there is the `component-debug` module in the runtime classpath, this library outputs some
diagnostic logs like this:

```text
I/DebugComponentProvider: Created com.example.app.FooComponent@8488aeb in 20 ms.
```

In addition, you can use
[DebugComponentProvider](../component-debug/src/main/java/com/linecorp/lich/component/debug/DebugComponentProvider.kt)
to modify components in the provider.

```kotlin
interface FooComponent {

    fun doSomething()

    companion object : ComponentFactory<FooComponent>() {
        override fun createComponent(context: Context): FooComponent =
            TODO("Create FooComponentImpl.")
    }
}
```

```kotlin
fun initFooComponentForDebug(context: Context) {
    val baseFoo = context.getComponent(FooComponent)
    if (baseFoo is FooComponentForDebug) {
        throw IllegalStateException("FooComponentForDebug is already set.")
    }
    context.debugComponentProvider.setComponent(FooComponent, FooComponentForDebug(baseFoo))
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

Note that the modification to
[DebugComponentProvider](../component-debug/src/main/java/com/linecorp/lich/component/debug/DebugComponentProvider.kt)
don't affect already acquired components.
So, the above `initFooComponentForDebug(context)` should be called prior to any acquisition of
`FooComponent`.

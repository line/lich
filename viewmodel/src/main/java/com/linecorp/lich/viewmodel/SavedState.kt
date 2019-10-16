/*
 * Copyright 2019 LINE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linecorp.lich.viewmodel

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import kotlin.reflect.KProperty

/**
 * A handle to saved state passed down to [AbstractViewModel].
 *
 * This is a key-value map that will let you write and retrieve objects to and from the saved state.
 * These values will persist after the process is killed by the system and remain available via the
 * same object.
 *
 * You can specify initial values of [SavedState] via the `arguments` parameter of the functions
 * such as [Fragment.viewModel]. In addition, there are several property delegates for easy access
 * to the values. Here is an example using these features:
 *
 * ```
 * val fooViewModel by fragment.viewModel(FooViewModel) {
 *     Bundle().apply {
 *         putString("userId", "abc123")
 *         putInt("balance", 999)
 *     }
 * }
 *
 * class FooViewModel(savedState: SavedState) : AbstractViewModel() {
 *     // This property is set to "abc123".
 *     val userId: String by savedState.required()
 *
 *     // The initial value of this MutableLiveData is 999.
 *     val balance: MutableLiveData<Int> by savedState.liveData()
 *
 *     // The initial value of this property is 0.
 *     var amount: Int by savedState.initial(0)
 *
 *     companion object : ViewModelFactory<FooViewModel>() {
 *         override fun createViewModel(context: Context, savedState: SavedState): FooViewModel =
 *             FooViewModel(savedState)
 *     }
 * }
 * ```
 */
@MainThread
class SavedState(private val savedStateHandle: SavedStateHandle) {
    /**
     * Creates a handle with the empty state.
     *
     * Typically, this constructor is used for unit tests.
     */
    constructor() : this(SavedStateHandle())

    /**
     * Creates a handle with the given initial arguments.
     *
     * Typically, this constructor is used for unit tests.
     */
    constructor(initialState: Map<String, Any?>) : this(SavedStateHandle(initialState))

    /**
     * Returns a view of the keys contained in this [SavedState].
     */
    val keys: Set<String>
        get() = savedStateHandle.keys()

    /**
     * Returns `true` if there is value associated with the given [key].
     */
    operator fun contains(key: String): Boolean =
        savedStateHandle.contains(key)

    /**
     * Returns the value associated with the given [key], or `null` if such a key is not present in
     * this [SavedState].
     */
    operator fun <T> get(key: String): T? =
        savedStateHandle.get(key)

    /**
     * Associates the given [value] with the [key].
     *
     * The value must have a type that could be stored in [android.os.Bundle].
     *
     * @param T any type that can be accepted by [android.os.Bundle].
     */
    operator fun <T> set(key: String, value: T) {
        savedStateHandle.set(key, value)
    }

    /**
     * Returns a [MutableLiveData] that accesses the value associated with the given [key].
     *
     * @param T any type that can be accepted by [android.os.Bundle].
     */
    fun <T> getLiveData(key: String): MutableLiveData<T> =
        savedStateHandle.getLiveData(key)

    /**
     * Returns a [MutableLiveData] that accesses the value associated with the given [key].
     *
     * If no value exists for the given [key], a [MutableLiveData] is created with the [initialValue].
     *
     * @param T any type that can be accepted by [android.os.Bundle].
     */
    fun <T> getLiveData(key: String, initialValue: T): MutableLiveData<T> =
        savedStateHandle.getLiveData(key, initialValue)

    // To keep the invariant of InitializedSavedState, we don't provide remove() function.
}

// Property delegates for SavedState.

/**
 * A property delegate to access the value associated with the name of the property.
 *
 * ```
 * class FooViewModel(savedState: SavedState) : AbstractViewModel() {
 *
 *     // A delegated property that accesses the value associated with "fooParam".
 *     // Its initial value can be specified by `arguments` of the functions such as `Fragment.viewModel`.
 *     var fooParam: String? by savedState
 * }
 * ```
 */
@Suppress("NOTHING_TO_INLINE")
@MainThread
inline operator fun <T> SavedState.getValue(thisRef: Any?, property: KProperty<*>): T? =
    this[property.name]

/**
 * A property delegate to access the value associated with the name of the property.
 * See [SavedState.getValue] for details.
 */
@Suppress("NOTHING_TO_INLINE")
@MainThread
inline operator fun <T> SavedState.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this[property.name] = value
}

/**
 * Provides a property delegate initialized with the given [value] unless specified by `arguments`
 * of the functions such as `Fragment.viewModel`.
 *
 * ```
 * class FooViewModel(savedState: SavedState) : AbstractViewModel() {
 *
 *     // A delegated property that accesses the value associated with "fooParam".
 *     // The value is initialized with "abc" unless specified by `arguments`.
 *     var fooParam: String by savedState.initial("abc")
 * }
 * ```
 */
@MainThread
fun <T> SavedState.initial(value: T): InitializingSavedStateDelegate<T> =
    InitializingSavedStateDelegate(this, value)

/**
 * Provides a property delegate that confirms the value is specified by `arguments` of the functions
 * such as `Fragment.viewModel`.
 * If it is not specified by `arguments`, throws IllegalStateException.
 *
 * ```
 * class FooViewModel(savedState: SavedState) : AbstractViewModel() {
 *
 *     // A delegated property that accesses the value associated with "fooParam".
 *     // The value must be initialized by `arguments`, otherwise IllegalStateException is thrown.
 *     var fooParam: String by savedState.required()
 * }
 * ```
 */
@MainThread
fun <T> SavedState.required(): RequiringSavedStateDelegate<T> =
    RequiringSavedStateDelegate(this)

/**
 * Provides a property delegate of a [MutableLiveData] that accesses the value associated with
 * the name of the property.
 *
 * ```
 * class FooViewModel(savedState: SavedState) : AbstractViewModel() {
 *
 *     // A delegated property of a `MutableLiveData` that accesses the value associated with "fooParam".
 *     // Its initial value can be specified by `arguments` of the functions such as `Fragment.viewModel`.
 *     val fooParam: MutableLiveData<String> by savedState.liveData()
 * }
 * ```
 */
@MainThread
fun <T> SavedState.liveData(): SavedStateLiveDataDelegate<T> =
    SavedStateLiveDataDelegate(this)

/**
 * Provides a property delegate of a [MutableLiveData] that accesses the value associated with
 * the name of the property.
 *
 * Its value is initialized with [initialValue] unless specified by `arguments` of the functions
 * such as `Fragment.viewModel`.
 *
 * ```
 * class FooViewModel(savedState: SavedState) : AbstractViewModel() {
 *
 *     // A delegated property of a `MutableLiveData` that accesses the value associated with "fooParam".
 *     // The value is initialized with "abc" unless specified by `arguments`.
 *     val fooParam: MutableLiveData<String> by savedState.liveData("abc")
 * }
 * ```
 */
@MainThread
fun <T> SavedState.liveData(initialValue: T): SavedStateLiveDataWithInitialDelegate<T> =
    SavedStateLiveDataWithInitialDelegate(this, initialValue)

// Implementation details of the property delegates.

/**
 * A delegate provider for [SavedState.initial].
 */
class InitializingSavedStateDelegate<T>(private val savedState: SavedState, private val value: T) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): InitializedSavedState<T> = initialize(property.name)

    @PublishedApi
    internal fun initialize(key: String): InitializedSavedState<T> {
        if (key !in savedState) {
            savedState[key] = value
        }
        return InitializedSavedState(savedState)
    }
}

/**
 * A delegate provider for [SavedState.required].
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class RequiringSavedStateDelegate<T>(val savedState: SavedState) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): InitializedSavedState<T> = checkExistence(property.name)

    @PublishedApi
    internal fun checkExistence(key: String): InitializedSavedState<T> {
        check(key in savedState) { "$key is not specified in the arguments." }
        return InitializedSavedState(savedState)
    }
}

/**
 * A property delegate that is guaranteed to have a value for the name.
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class InitializedSavedState<T>(val savedState: SavedState) {
    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST", "RemoveExplicitTypeArguments")
    @MainThread
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        savedState.get<T>(property.name) as T

    @Suppress("NOTHING_TO_INLINE")
    @MainThread
    inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        savedState[property.name] = value
    }
}

/**
 * A delegate provider for [SavedState.liveData] without `initialValue`.
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class SavedStateLiveDataDelegate<T>(val savedState: SavedState) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): SavedStateLiveData<T> = getSavedStateLiveData(property.name)

    @PublishedApi
    internal fun getSavedStateLiveData(key: String): SavedStateLiveData<T> =
        SavedStateLiveData(savedState.getLiveData(key))
}

/**
 * A delegate provider for [SavedState.liveData] with `initialValue`.
 */
class SavedStateLiveDataWithInitialDelegate<T>(
    private val savedState: SavedState,
    private val initialValue: T
) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): SavedStateLiveData<T> = getSavedStateLiveData(property.name)

    @PublishedApi
    internal fun getSavedStateLiveData(key: String): SavedStateLiveData<T> =
        SavedStateLiveData(savedState.getLiveData(key, initialValue))
}

/**
 * A property delegate that was provided by [SavedStateLiveDataDelegate] or
 * [SavedStateLiveDataWithInitialDelegate].
 */
@Suppress("EXPERIMENTAL_FEATURE_WARNING")
inline class SavedStateLiveData<T>(val liveData: MutableLiveData<T>) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): MutableLiveData<T> =
        liveData
}

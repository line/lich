/*
 * Copyright 2021 LINE Corporation
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
package com.linecorp.lich.savedstate

import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import kotlin.reflect.KProperty

/**
 * A property delegate for [SavedStateHandle] to access the value associated with the name of this
 * property.
 *
 * ```
 * class FooViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
 *
 *     // A delegated property that accesses the value associated with "fooParam".
 *     // This is equivalent to the code below.
 *     // ```
 *     // var fooParam: String?
 *     //     get() = savedStateHandle["fooParam"]
 *     //     set(value) { savedStateHandle["fooParam"] = value }
 *     // ```
 *     var fooParam: String? by savedStateHandle
 * }
 * ```
 */
@Suppress("NOTHING_TO_INLINE")
@MainThread
inline operator fun <T> SavedStateHandle.getValue(thisRef: Any?, property: KProperty<*>): T? =
    this[property.name]

/**
 * A property delegate for [SavedStateHandle] to access the value associated with the name of this
 * property. See [SavedStateHandle.getValue] for details.
 */
@Suppress("NOTHING_TO_INLINE")
@MainThread
inline operator fun <T> SavedStateHandle.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this[property.name] = value
}

/**
 * Provides a property delegate for [SavedStateHandle] to access the value associated with
 * the name of this property. Unless the value is specified in the `Args` of the ViewModel,
 * it will be initialized with the given [value].
 *
 * ```
 * class FooViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
 *
 *     // A delegated property that accesses the value associated with "fooParam".
 *     // The value is initialized with "abc" unless specified by the `Args` of the ViewModel.
 *     // This is equivalent to the code below.
 *     // ```
 *     // init {
 *     //     if ("fooParam" !in savedStateHandle) {
 *     //         savedStateHandle["fooParam"] = "abc"
 *     //     }
 *     // }
 *     //
 *     // var fooParam: String
 *     //     get() = savedStateHandle["fooParam"]!!
 *     //     set(value) { savedStateHandle["fooParam"] = value }
 *     // ```
 *     var fooParam: String by savedStateHandle.initial("abc")
 * }
 * ```
 */
@MainThread
fun <T> SavedStateHandle.initial(value: T): InitializingSavedStateDelegate<T> =
    InitializingSavedStateDelegate(this, value)

/**
 * Provides a property delegate for [SavedStateHandle] to access the value associated with
 * the name of this property. The value must be specified in the `Args` of the ViewModel,
 * otherwise IllegalStateException will be thrown.
 *
 * ```
 * class FooViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
 *
 *     // A delegated property that accesses the value associated with "fooParam".
 *     // The value must be initialized by the `Args` of the ViewModel,
 *     // otherwise IllegalStateException is thrown.
 *     // This is equivalent to the code below.
 *     // ```
 *     // init {
 *     //     check("fooParam" in savedStateHandle) { "fooParam is not specified in the arguments." }
 *     // }
 *     //
 *     // var fooParam: String
 *     //     get() = savedStateHandle["fooParam"]!!
 *     //     set(value) { savedStateHandle["fooParam"] = value }
 *     // ```
 *     var fooParam: String by savedStateHandle.required()
 * }
 * ```
 */
@MainThread
fun <T> SavedStateHandle.required(): RequiringSavedStateDelegate<T> =
    RequiringSavedStateDelegate(this)

/**
 * Provides a property delegate for [SavedStateHandle] to get the [MutableLiveData] associated with
 * the name of this property.
 *
 * ```
 * class FooViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
 *
 *     // A delegated property of a `MutableLiveData` that accesses the value associated with "fooParam".
 *     // This is equivalent to the code below.
 *     // ```
 *     // val fooParam: MutableLiveData<String> = savedStateHandle.getLiveData("fooParam")
 *     // ```
 *     val fooParam: MutableLiveData<String> by savedStateHandle.liveData()
 * }
 * ```
 */
@MainThread
fun <T> SavedStateHandle.liveData(): SavedStateLiveDataDelegate<T> =
    SavedStateLiveDataDelegate(this)

/**
 * Provides a property delegate for [SavedStateHandle] to get the [MutableLiveData] associated with
 * the name of this property. Unless the value is specified in the `Args` of the ViewModel,
 * it will be initialized with the given [initialValue].
 *
 * ```
 * class FooViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
 *
 *     // A delegated property of a `MutableLiveData` that accesses the value associated with "fooParam".
 *     // The value is initialized with "abc" unless specified by the `Args` of the ViewModel.
 *     // This is equivalent to the code below.
 *     // ```
 *     // val fooParam: MutableLiveData<String> = savedStateHandle.getLiveData("fooParam", "abc")
 *     // ```
 *     val fooParam: MutableLiveData<String> by savedStateHandle.liveDataWithInitial("abc")
 * }
 * ```
 */
@MainThread
fun <T> SavedStateHandle.liveDataWithInitial(
    initialValue: T
): SavedStateLiveDataWithInitialDelegate<T> =
    SavedStateLiveDataWithInitialDelegate(this, initialValue)

/**
 * A delegate provider for [SavedStateHandle.initial].
 */
class InitializingSavedStateDelegate<T>(
    private val savedStateHandle: SavedStateHandle,
    private val value: T
) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): InitializedSavedState<T> = initialize(property.name)

    @PublishedApi
    internal fun initialize(key: String): InitializedSavedState<T> {
        if (key !in savedStateHandle) {
            savedStateHandle[key] = value
        }
        return InitializedSavedState(savedStateHandle, key)
    }
}

/**
 * A delegate provider for [SavedStateHandle.required].
 */
class RequiringSavedStateDelegate<T>(private val savedStateHandle: SavedStateHandle) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): InitializedSavedState<T> = checkExistence(property.name)

    @PublishedApi
    internal fun checkExistence(key: String): InitializedSavedState<T> {
        check(key in savedStateHandle) { "$key is not specified in the arguments." }
        return InitializedSavedState(savedStateHandle, key)
    }
}

/**
 * A property delegate that is guaranteed to have a value for the key.
 */
class InitializedSavedState<T>(
    private val savedStateHandle: SavedStateHandle,
    private val key: String
) {
    @Suppress("NOTHING_TO_INLINE")
    @MainThread
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    @Suppress("NOTHING_TO_INLINE")
    @MainThread
    inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        set(value)
    }

    @PublishedApi
    internal fun get(): T {
        @Suppress("UNCHECKED_CAST")
        return savedStateHandle.get<T>(key) as T
    }

    @PublishedApi
    internal fun set(value: T) {
        savedStateHandle[key] = value
    }
}

/**
 * A delegate provider for [SavedStateHandle.liveData].
 */
class SavedStateLiveDataDelegate<T>(private val savedStateHandle: SavedStateHandle) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): SavedStateLiveData<T> = getSavedStateLiveData(property.name)

    @PublishedApi
    internal fun getSavedStateLiveData(key: String): SavedStateLiveData<T> =
        SavedStateLiveData(savedStateHandle.getLiveData(key))
}

/**
 * A delegate provider for [SavedStateHandle.liveDataWithInitial].
 */
class SavedStateLiveDataWithInitialDelegate<T>(
    private val savedStateHandle: SavedStateHandle,
    private val initialValue: T
) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun provideDelegate(
        thisRef: Any?,
        property: KProperty<*>
    ): SavedStateLiveData<T> = getSavedStateLiveData(property.name)

    @PublishedApi
    internal fun getSavedStateLiveData(key: String): SavedStateLiveData<T> =
        SavedStateLiveData(savedStateHandle.getLiveData(key, initialValue))
}

/**
 * A property delegate that was provided by [SavedStateLiveDataDelegate] or
 * [SavedStateLiveDataWithInitialDelegate].
 */
class SavedStateLiveData<T>(@PublishedApi internal val liveData: MutableLiveData<T>) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): MutableLiveData<T> =
        liveData
}

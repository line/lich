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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.linecorp.lich.savedstate.getValue
import com.linecorp.lich.savedstate.initial
import com.linecorp.lich.savedstate.liveData
import com.linecorp.lich.savedstate.liveDataWithInitial
import com.linecorp.lich.savedstate.required
import com.linecorp.lich.savedstate.setValue
import kotlin.reflect.KProperty

/**
 * A handle to saved state passed down to [AbstractViewModel].
 */
@Deprecated(
    "Use `SavedStateHandle` directly.",
    ReplaceWith("SavedStateHandle", "androidx.lifecycle.SavedStateHandle")
)
@MainThread
class SavedState(val savedStateHandle: SavedStateHandle) {
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
    inline operator fun <T> getValue(thisRef: Any?, property: KProperty<*>): T? =
        savedStateHandle.getValue(thisRef, property)

    /**
     * A property delegate to access the value associated with the name of the property.
     * See [getValue] for details.
     */
    @Suppress("NOTHING_TO_INLINE")
    @MainThread
    inline operator fun <T> setValue(thisRef: Any?, property: KProperty<*>, value: T) =
        savedStateHandle.setValue(thisRef, property, value)

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
    fun <T> initial(value: T): InitializingSavedStateDelegate<T> =
        savedStateHandle.initial(value)

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
    fun <T> required(): RequiringSavedStateDelegate<T> =
        savedStateHandle.required()

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
    fun <T> liveData(): SavedStateLiveDataDelegate<T> =
        savedStateHandle.liveData()

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
    @Deprecated(
        "Renamed to `liveDataWithInitial`.",
        ReplaceWith("this.liveDataWithInitial(initialValue)")
    )
    @MainThread
    fun <T> liveData(initialValue: T): SavedStateLiveDataWithInitialDelegate<T> =
        savedStateHandle.liveDataWithInitial(initialValue)

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
     *     val fooParam: MutableLiveData<String> by savedState.liveDataWithInitial("abc")
     * }
     * ```
     */
    @MainThread
    fun <T> liveDataWithInitial(initialValue: T): SavedStateLiveDataWithInitialDelegate<T> =
        savedStateHandle.liveDataWithInitial(initialValue)
}

@Deprecated(
    "Moved to `com.linecorp.lich.savedstate` package.",
    ReplaceWith(
        "InitializingSavedStateDelegate<T>",
        "com.linecorp.lich.savedstate.InitializingSavedStateDelegate"
    )
)
typealias InitializingSavedStateDelegate<T> = com.linecorp.lich.savedstate.InitializingSavedStateDelegate<T>

@Deprecated(
    "Moved to `com.linecorp.lich.savedstate` package.",
    ReplaceWith(
        "RequiringSavedStateDelegate<T>",
        "com.linecorp.lich.savedstate.RequiringSavedStateDelegate"
    )
)
typealias RequiringSavedStateDelegate<T> = com.linecorp.lich.savedstate.RequiringSavedStateDelegate<T>

@Deprecated(
    "Moved to `com.linecorp.lich.savedstate` package.",
    ReplaceWith(
        "SavedStateLiveDataDelegate<T>",
        "com.linecorp.lich.savedstate.SavedStateLiveDataDelegate"
    )
)
typealias SavedStateLiveDataDelegate<T> = com.linecorp.lich.savedstate.SavedStateLiveDataDelegate<T>

@Deprecated(
    "Moved to `com.linecorp.lich.savedstate` package.",
    ReplaceWith(
        "SavedStateLiveDataWithInitialDelegate<T>",
        "com.linecorp.lich.savedstate.SavedStateLiveDataWithInitialDelegate"
    )
)
typealias SavedStateLiveDataWithInitialDelegate<T> = com.linecorp.lich.savedstate.SavedStateLiveDataWithInitialDelegate<T>

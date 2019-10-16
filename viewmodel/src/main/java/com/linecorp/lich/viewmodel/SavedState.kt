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

/**
 * A handle to saved state passed down to [AbstractViewModel].
 *
 * This is a key-value map that will let you write and retrieve objects to and from the saved state.
 * These values will persist after the process is killed by the system and remain available via the
 * same object.
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
}

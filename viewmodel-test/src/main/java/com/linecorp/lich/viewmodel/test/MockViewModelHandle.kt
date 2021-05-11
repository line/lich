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
package com.linecorp.lich.viewmodel.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStoreOwner
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory

/**
 * Utility class to handle mocked ViewModel instances.
 *
 * @param mockProducer a function to create a new mock instance.
 */
class MockViewModelHandle<T : AbstractViewModel>(
    private val mockProducer: (SavedStateHandle) -> T
) {

    private var _mock: T? = null

    private var _viewModelStoreOwner: ViewModelStoreOwner? = null

    private var _savedStateHandle: SavedStateHandle? = null

    /**
     * True if at least one mock ViewModel instance was created for this handle.
     */
    val isCreated: Boolean
        get() = _mock != null

    /**
     * The mock ViewModel instance if created, otherwise throws [IllegalStateException].
     *
     * If multiple mock instances were created before, this is the last created one.
     */
    val mock: T
        get() = _mock ?: throwNotCreatedException()

    /**
     * The [ViewModelStoreOwner] for which [mock] was created.
     */
    val viewModelStoreOwner: ViewModelStoreOwner
        get() = _viewModelStoreOwner ?: throwNotCreatedException()

    /**
     * The [SavedStateHandle] for which [mock] was created.
     */
    val savedStateHandle: SavedStateHandle
        get() = _savedStateHandle ?: throwNotCreatedException()

    private fun throwNotCreatedException(): Nothing =
        throw IllegalStateException("Mock ViewModel is not created yet.")

    private fun createMock(
        viewModelStoreOwner: ViewModelStoreOwner,
        savedStateHandle: SavedStateHandle
    ): T {
        val mock = mockProducer(savedStateHandle)
        _mock = mock
        _viewModelStoreOwner = viewModelStoreOwner
        _savedStateHandle = savedStateHandle
        return mock
    }

    /**
     * Sets this handle as a mock ViewModel factory for [factory].
     *
     * @see setMockViewModel
     */
    @JvmOverloads
    fun setAsMockViewModelFor(
        factory: ViewModelFactory<T>,
        mockViewModelManager: MockViewModelManager = getMockViewModelManager()
    ): MockViewModelHandle<T> {
        mockViewModelManager.setMockViewModel(factory, this::createMock)
        return this
    }
}

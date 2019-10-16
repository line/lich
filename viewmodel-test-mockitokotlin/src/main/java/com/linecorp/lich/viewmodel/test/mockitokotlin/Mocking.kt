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
package com.linecorp.lich.viewmodel.test.mockitokotlin

import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import com.linecorp.lich.viewmodel.test.MockViewModelHandle
import com.linecorp.lich.viewmodel.test.createRealViewModel
import com.nhaarman.mockitokotlin2.KStubbing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy

/**
 * Sets mock ViewModel factory for [factory]. The mock factory supports immediate stubbing.
 *
 * After a mock ViewModel instance is actually created, you can obtain the mock instance via
 * [MockViewModelHandle].
 *
 * @return the handle to access created mock instances.
 */
inline fun <reified T : AbstractViewModel> mockViewModel(
    factory: ViewModelFactory<T>,
    crossinline stubbing: KStubbing<T>.(T) -> Unit = {}
): MockViewModelHandle<T> =
    MockViewModelHandle {
        mock(stubbing = stubbing)
    }.setAsMockViewModelFor(factory)

/**
 * Sets spy ViewModel factory for [factory]. The spy factory supports immediate stubbing.
 *
 * After a spy ViewModel instance is actually created, you can obtain the spy instance via
 * [MockViewModelHandle].
 *
 * @return the handle to access created spy instances.
 */
inline fun <reified T : AbstractViewModel> spyViewModel(
    factory: ViewModelFactory<T>,
    crossinline stubbing: KStubbing<T>.(T) -> Unit = {}
): MockViewModelHandle<T> =
    MockViewModelHandle { savedState ->
        spy(value = createRealViewModel(factory, savedState), stubbing = stubbing)
    }.setAsMockViewModelFor(factory)

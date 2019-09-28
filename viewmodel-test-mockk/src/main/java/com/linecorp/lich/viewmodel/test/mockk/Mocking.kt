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
package com.linecorp.lich.viewmodel.test.mockk

import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import com.linecorp.lich.viewmodel.test.MockViewModelHandle
import com.linecorp.lich.viewmodel.test.createRealViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.MainScope

/**
 * Sets mock ViewModel factory for [factory]. The mock factory supports immediate stubbing.
 *
 * After a mock ViewModel instance is actually created, you can obtain the mock instance via
 * [MockViewModelHandle].
 *
 * @param factory a ViewModel to be mocked.
 * @param relaxed allows creation with no specific behaviour.
 * @param block block to execute after mock is created with mock as a receiver.
 * @return the handle to access created mock instances.
 */
inline fun <reified T : AbstractViewModel> mockViewModel(
    factory: ViewModelFactory<T>,
    relaxed: Boolean = false,
    crossinline block: T.() -> Unit = {}
): MockViewModelHandle<T> =
    MockViewModelHandle {
        // To mock `AbstractViewModel.clear()`, relaxUnitFun is always true.
        mockk<T>(relaxed = relaxed, relaxUnitFun = true) {
            // We need to mock `AbstractViewModel.coroutineContext` explicitly because MockK cannot
            // mock `AbstractViewModel.clear()` prior to Android P.
            val mainScope = MainScope()
            every { coroutineContext } returns mainScope.coroutineContext

            block()
        }
    }.setAsMockViewModelFor(factory)

/**
 * Sets spy ViewModel factory for [factory]. The spy factory supports immediate stubbing.
 *
 * After a spy ViewModel instance is actually created, you can obtain the spy instance via
 * [MockViewModelHandle].
 *
 * @param factory a ViewModel to be spied.
 * @param recordPrivateCalls allows this spy to record any private calls, enabling a verification.
 * @param block block to execute after a spy is created with the spy as a receiver.
 * @return the handle to access created spy instances.
 */
inline fun <reified T : AbstractViewModel> spyViewModel(
    factory: ViewModelFactory<T>,
    recordPrivateCalls: Boolean = false,
    crossinline block: T.() -> Unit = {}
): MockViewModelHandle<T> =
    MockViewModelHandle {
        spyk(
            objToCopy = createRealViewModel(factory),
            recordPrivateCalls = recordPrivateCalls,
            block = block
        )
    }.setAsMockViewModelFor(factory)

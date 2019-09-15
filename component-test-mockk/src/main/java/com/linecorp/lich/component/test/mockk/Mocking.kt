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
package com.linecorp.lich.component.test.mockk

import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.test.getRealComponent
import com.linecorp.lich.component.test.setMockComponent
import io.mockk.mockk
import io.mockk.spyk

/**
 * Creates a mock component, then sets the mock for [factory].
 *
 * @param factory a Component to be mocked.
 * @param relaxed allows creation with no specific behaviour.
 * @param relaxUnitFun allows creation with no specific behaviour for Unit function.
 * @param block block to execute after mock is created with mock as a receiver.
 * @return the created mock component.
 */
inline fun <reified T : Any> mockComponent(
    factory: ComponentFactory<T>,
    relaxed: Boolean = false,
    relaxUnitFun: Boolean = false,
    block: T.() -> Unit = {}
): T = mockk(relaxed = relaxed, relaxUnitFun = relaxUnitFun, block = block)
    .also { setMockComponent(factory, it) }

/**
 * Creates a spy of the real component, then sets the spy for [factory].
 *
 * @param factory a Component to be spied.
 * @param recordPrivateCalls allows this spy to record any private calls, enabling a verification.
 * @param block block to execute after a spy is created with the spy as a receiver.
 * @return the created spy component.
 */
inline fun <reified T : Any> spyComponent(
    factory: ComponentFactory<T>,
    recordPrivateCalls: Boolean = false,
    block: T.() -> Unit = {}
): T = spyk(
    objToCopy = getRealComponent(factory),
    recordPrivateCalls = recordPrivateCalls,
    block = block
).also { setMockComponent(factory, it) }

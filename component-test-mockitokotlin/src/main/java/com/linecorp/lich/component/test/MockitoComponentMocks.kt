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
package com.linecorp.lich.component.test

import com.linecorp.lich.component.ComponentFactory
import com.nhaarman.mockitokotlin2.KStubbing

@Deprecated(
    "Moved to the `com.linecorp.lich.component.test.mockitokotlin` package.",
    ReplaceWith("com.linecorp.lich.component.test.mockitokotlin.mockComponent(factory, stubbing)")
)
inline fun <reified T : Any> mockComponent(
    factory: ComponentFactory<T>,
    stubbing: KStubbing<T>.(T) -> Unit = {}
): T = com.linecorp.lich.component.test.mockitokotlin.mockComponent(factory, stubbing)

@Deprecated(
    "Moved to the `com.linecorp.lich.component.test.mockitokotlin` package.",
    ReplaceWith("com.linecorp.lich.component.test.mockitokotlin.spyComponent(factory, stubbing)")
)
inline fun <reified T : Any> spyComponent(
    factory: ComponentFactory<T>,
    stubbing: KStubbing<T>.(T) -> Unit = {}
): T = com.linecorp.lich.component.test.mockitokotlin.spyComponent(factory, stubbing)

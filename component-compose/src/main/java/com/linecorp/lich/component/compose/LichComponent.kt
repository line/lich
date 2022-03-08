/*
 * Copyright 2022 LINE Corporation
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
package com.linecorp.lich.component.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.getComponent

/**
 * Gets a singleton instance of component created from [factory].
 * If such component is not created yet, it will be created immediately.
 *
 * This is a sample code:
 * ```
 * @Composable
 * fun FooScreen(fooComponent: FooComponent = lichComponent(FooComponent)) {
 *     // Use fooComponent here.
 * }
 * ```
 *
 * @param factory A [ComponentFactory] to create the component.
 */
@Composable
fun <T : Any> lichComponent(factory: ComponentFactory<T>): T =
    LocalContext.current.getComponent(factory)

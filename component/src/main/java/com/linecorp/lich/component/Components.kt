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
@file:JvmName("Components")

package com.linecorp.lich.component

import android.content.Context
import com.linecorp.lich.component.internal.componentProvider

/**
 * Gets a singleton instance of component created from [factory].
 * If such component is not created yet, it will be created immediately.
 *
 * This function is thread-safe. It is guaranteed that the same instance will always be returned
 * even if called from multiple threads.
 *
 * If you want to get components lazily, use [Context.component] instead.
 *
 * @see Context.component
 * @see ComponentFactory
 */
@JvmName("get")
fun <T : Any> Context.getComponent(factory: ComponentFactory<T>): T =
    componentProvider.getComponent(this, factory)

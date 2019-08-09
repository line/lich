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
package com.linecorp.lich.component.provider

import android.content.Context
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.provider.internal.ComponentCreator
import com.linecorp.lich.component.provider.internal.componentProviderFactory

/**
 * A class that provides a singleton for each [ComponentFactory].
 *
 * @see ComponentProviderOwner
 */
interface ComponentProvider {
    /**
     * Gets a singleton instance of component created from [factory].
     * If such component is not created yet, it will be created immediately.
     *
     * This method is intended for internal use of the component framework.
     * Instead, you should use such as `Context.getComponent(factory)`.
     */
    fun <T : Any> getComponent(context: Context, factory: ComponentFactory<T>): T
}

/**
 * Creates a new `ComponentProvider`.
 *
 * By default, this function returns a simple and fast implementation of ComponentProvider.
 * But, if there is the `component-debug` module in the runtime classpath, this function returns
 * a `DebugComponentProvider` that outputs diagnostic logs and supports several operations to
 * modify components in the provider.
 */
@Suppress("FunctionName") // factory function
fun ComponentProvider(): ComponentProvider =
    componentProviderFactory.newComponentProvider(ComponentCreatorImpl)

private object ComponentCreatorImpl : ComponentCreator {
    override fun <T : Any> create(context: Context, factory: ComponentFactory<T>): T =
        factory.create(context.applicationContext)
}

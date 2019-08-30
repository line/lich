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
package com.linecorp.lich.component.internal

import android.content.Context
import com.linecorp.lich.component.ComponentFactory
import java.util.ServiceLoader

interface ComponentProvider {

    val loadPriority: Int

    fun init(accessor: ComponentAccessor)

    fun <T : Any> getComponent(context: Context, factory: ComponentFactory<T>): T

    fun getManager(applicationContext: Context): Any
}

interface ComponentAccessor {

    fun <T : Any> createComponent(factory: ComponentFactory<T>, applicationContext: Context): T

    fun getComponent(factory: ComponentFactory<*>): Any?

    fun setComponent(factory: ComponentFactory<*>, component: Any?)

    fun compareAndSetComponent(factory: ComponentFactory<*>, expect: Any?, update: Any?): Boolean
}

internal val componentProvider: ComponentProvider =
    Sequence {
        ServiceLoader.load(
            ComponentProvider::class.java,
            ComponentProvider::class.java.classLoader
        ).iterator()
    }.maxBy { it.loadPriority }?.apply { init(ComponentFactory.accessor) }
        ?: throw Error("Failed to load ComponentProvider.")

fun getComponentManager(applicationContext: Context): Any =
    componentProvider.getManager(applicationContext)

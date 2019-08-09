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
package com.linecorp.lich.component

import android.content.Context

/**
 * A class to declare a component factory referenced from [ComponentFactory.delegateCreation].
 *
 * The implementation of this class should be declared as a **class** rather than a (companion) object.
 * And, since it is instantiated by `Class.newInstance()`, it should have a public constructor with
 * no argument.
 *
 * @param T the type of the component that will be created by this factory.
 * @see ComponentFactory.delegateCreation
 */
abstract class DelegatedComponentFactory<T : Any> {
    /**
     * Creates a component for this factory.
     *
     * @param context the application context.
     */
    protected abstract fun createComponent(context: Context): T

    internal fun create(context: Context): T = createComponent(context)
}

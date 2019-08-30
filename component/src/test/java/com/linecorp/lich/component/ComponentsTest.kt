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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class ComponentsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun sameInstance() {
        val componentX1 = context.getComponent(ComponentX)
        assertEquals("X", componentX1.name)
        assertSame(context, componentX1.context)

        val componentX2 = context.getComponent(ComponentX)
        assertSame(componentX1, componentX2)
    }

    @Test
    fun dependency() {
        val componentY = context.getComponent(ComponentY)
        assertEquals("Y", componentY.name)
        assertEquals("Z", componentY.componentZ.name)

        val componentZ = context.getComponent(ComponentZ)
        assertSame(componentY.componentZ, componentZ)
        assertSame(componentY, componentZ.componentY)
    }

    @Test
    fun exceptionOnCreate() {
        val ex1 = assertFailsWith(NumberFormatException::class) {
            context.getComponent(ErrorComponent)
        }

        val ex2 = assertFailsWith(NumberFormatException::class) {
            context.getComponent(ErrorComponent)
        }

        assertNotSame(ex1, ex2)
    }

    @Test
    fun delegateCreation() {
        val component = context.getComponent(DelegationByNameComponent)
        assertEquals("I am DelegationByNameComponent.", component.greeting())
    }

    @Test
    fun delegateToServiceLoader1() {
        val component = context.getComponent(DelegateToServiceLoaderComponent1)
        assertEquals("I am DelegateToServiceLoaderComponent1Impl.", component.greeting())
    }

    @Test
    fun delegateToServiceLoader2() {
        val component = context.getComponent(DelegateToServiceLoaderComponent2)
        assertEquals("otherComponent.name is foo.", component.askToOther())
    }
}

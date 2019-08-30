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
package com.linecorp.lich.component.debug

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.component.getComponent
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class DebugComponentManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        context.debugComponentManager.clearComponent(FooComponent)
    }

    @Test
    fun setComponent() {
        val foo1 = context.getComponent(FooComponent)
        assertEquals("I am FooComponent.", foo1.sayHello())

        context.debugComponentManager.setComponent(FooComponent, DebugFooComponent())

        val foo2 = context.getComponent(FooComponent)
        assertEquals("I am DebugFooComponent.", foo2.sayHello())
    }

    @Test
    fun clearComponent() {
        val foo1 = context.getComponent(FooComponent)
        assertEquals("I am FooComponent.", foo1.sayHello())

        context.debugComponentManager.clearComponent(FooComponent)

        val foo2 = context.getComponent(FooComponent)
        assertEquals("I am FooComponent.", foo2.sayHello())

        assertNotSame(foo1, foo2)
    }

    @Test
    fun getComponentIfAlreadyCreated() {
        assertNull(context.debugComponentManager.getComponentIfAlreadyCreated(FooComponent))

        val foo1 = context.getComponent(FooComponent)
        assertEquals("I am FooComponent.", foo1.sayHello())

        val foo2 = context.debugComponentManager.getComponentIfAlreadyCreated(FooComponent)

        assertSame(foo1, foo2)

        context.debugComponentManager.clearComponent(FooComponent)

        assertNull(context.debugComponentManager.getComponentIfAlreadyCreated(FooComponent))
    }

    private class DebugFooComponent : FooComponent {
        override fun sayHello(): String = "I am DebugFooComponent."
    }
}

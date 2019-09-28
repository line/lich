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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.component.getComponent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class ComponentMocksTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun setMockComponent() {
        val foo1 = context.getComponent(FooComponent)
        assertEquals("FooComponent", foo1.message)

        val mockFoo = FooComponent("MockFoo")
        setMockComponent(FooComponent, mockFoo)

        val foo2 = context.getComponent(FooComponent)
        assertEquals("MockFoo", foo2.message)

        assertSame(mockFoo, foo2)
        assertNotSame(foo1, foo2)
    }

    @Test
    fun clearMockComponent() {
        val foo1 = context.getComponent(FooComponent)
        assertEquals("FooComponent", foo1.message)

        val mockFoo = FooComponent("MockFoo")
        setMockComponent(FooComponent, mockFoo)

        val foo2 = context.getComponent(FooComponent)
        assertEquals("MockFoo", foo2.message)

        clearMockComponent(FooComponent)

        val foo3 = context.getComponent(FooComponent)
        assertEquals("FooComponent", foo3.message)

        assertSame(foo1, foo3)
    }

    @Test
    fun getRealComponent() {
        val foo1 = context.getComponent(FooComponent)
        assertEquals("FooComponent", foo1.message)

        val mockFoo = FooComponent("MockFoo")
        setMockComponent(FooComponent, mockFoo)

        val foo2 = context.getComponent(FooComponent)
        assertEquals("MockFoo", foo2.message)

        val foo3 = getRealComponent(FooComponent)
        assertEquals("FooComponent", foo3.message)

        assertSame(foo1, foo3)
    }

    @Test
    fun clearAll() {
        val foo1 = context.getComponent(FooComponent)
        assertEquals("FooComponent", foo1.message)

        val mockFoo = FooComponent("MockFoo")
        setMockComponent(FooComponent, mockFoo)

        val foo2 = context.getComponent(FooComponent)
        assertEquals("MockFoo", foo2.message)

        clearAllComponents()

        val foo3 = context.getComponent(FooComponent)
        assertEquals("FooComponent", foo3.message)

        assertNotSame(foo1, foo3)
        assertNotSame(foo2, foo3)
    }
}

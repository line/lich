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
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.component.getComponent
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.ref.WeakReference
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class MockComponentManagerTest {

    @Test
    fun multipleMockComponentManagers() {
        val context1 = MockContext()
        val manager1 = getMockComponentManager(context1)

        val foo1 = context1.getComponent(FooComponent)
        assertSame(foo1, manager1.getComponentIfAlreadyCreated(FooComponent))

        val context2 = MockContext()
        val manager2 = getMockComponentManager(context2)

        val foo2 = context2.getComponent(FooComponent)
        assertSame(foo2, manager2.getComponentIfAlreadyCreated(FooComponent))

        assertNotSame(foo1, foo2)

        System.gc()

        assertSame(foo1, context1.getComponent(FooComponent))
        assertSame(foo2, context2.getComponent(FooComponent))

        manager1.clearAllComponents()

        assertNotSame(foo1, context1.getComponent(FooComponent))
        assertSame(foo2, context2.getComponent(FooComponent))
    }

    @Test
    fun gcForMockComponentManager() {
        val fooComponentRef = getFooComponentForNewContext()

        val anotherContext = MockContext()
        anotherContext.getComponent(FooComponent)

        System.gc()

        assertNull(fooComponentRef.get())
    }

    private fun getFooComponentForNewContext(): WeakReference<FooComponent> {
        val mockContext = MockContext()

        val fooComponentRef = WeakReference(mockContext.getComponent(FooComponent))

        System.gc()

        val fooComponent2 = mockContext.getComponent(FooComponent)
        assertSame(fooComponentRef.get(), fooComponent2)

        return fooComponentRef
    }

    class MockContext : ContextWrapper(ApplicationProvider.getApplicationContext()) {
        override fun getApplicationContext(): Context = this
    }
}

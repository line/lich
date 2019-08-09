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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@Config(application = TestApplication::class)
class CircularDependencyTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun detectCircularDependency() {
        val exception = assertFailsWith(IllegalStateException::class) {
            context.getComponent(ComponentX)
        }
        assertTrue { exception.message?.startsWith("Detected circular dependency") ?: false }

        assertFailsWith(IllegalStateException::class) {
            context.getComponent(ComponentY)
        }
        assertFailsWith(IllegalStateException::class) {
            context.getComponent(ComponentZ)
        }
    }

    @Test
    fun lazyCircularDependency() {

        val componentXLazy = context.getComponent(ComponentXLazy)

        assertSame(componentXLazy, componentXLazy.componentYLazy.componentZLazy.componentXLazy)

        assertSame(context.getComponent(ComponentYLazy), componentXLazy.componentYLazy)

        assertSame(
            context.getComponent(ComponentZLazy),
            componentXLazy.componentYLazy.componentZLazy
        )
    }
}

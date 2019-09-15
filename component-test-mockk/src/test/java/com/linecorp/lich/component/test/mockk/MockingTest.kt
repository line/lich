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
package com.linecorp.lich.component.test.mockk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.component.getComponent
import com.linecorp.lich.component.test.clearMockComponent
import io.mockk.every
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class MockingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun simpleMock() {
        val mockX = mockComponent(ComponentX) {
            every { value } returns "MockedX"
            every { getMessage() } returns "Hello mock!"
        }

        val componentX = context.getComponent(ComponentX)

        assertSame(mockX, componentX)
        assertEquals("MockedX", componentX.value)
        assertEquals("Hello mock!", componentX.getMessage())
    }

    @Test
    fun injectedMock() {
        mockComponent(ComponentX) {
            every { getMessage() } returns "Mocked."
        }

        val componentY = context.getComponent(ComponentY)

        assertEquals("From X: Mocked.", componentY.askToX())
    }

    @Test
    fun spying() {
        val spyX = spyComponent(ComponentX)

        val componentY = context.getComponent(ComponentY)

        assertEquals("From X: Hello.", componentY.askToX())
        assertEquals("From X: Hello.", componentY.askToX())

        verify(exactly = 2) { spyX.getMessage() }
    }

    @Test
    fun clearMock() {
        val mockX = mockComponent(ComponentX)

        val componentX1 = context.getComponent(ComponentX)

        assertSame(mockX, componentX1)

        clearMockComponent(ComponentX)
        val componentX2 = context.getComponent(ComponentX)

        assertNotSame(mockX, componentX2)
        assertEquals("X", componentX2.value)
    }
}

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
package com.linecorp.lich.viewmodel.test.mockitokotlin

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.viewmodel.test.clearAllMockViewModels
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.only
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MockingTest {

    @After
    fun tearDown() {
        clearAllMockViewModels()
    }

    @Test
    fun mockViewModel() {
        val mockHandle = mockViewModel(FooViewModel) {
            on { greeting() } doReturn "Hello mock!"
            on { countItem() } doReturn 42
        }
        assertFalse(mockHandle.isCreated)

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(mockHandle.isCreated)
                assertSame(mockHandle.viewModelStoreOwner, activity)
                assertSame(mockHandle.mock, activity.fooViewModel)
                assertEquals(setOf("itemCount"), mockHandle.savedState.keys)
                assertEquals(10, mockHandle.savedState["itemCount"])

                verify(mockHandle.mock, only()).greeting()
                verify(mockHandle.mock, never()).countItem()

                assertEquals("Hello mock!", activity.fooViewModel.greeting())
                assertEquals(42, activity.fooViewModel.countItem())
            }
        }
    }

    @Test
    fun spyViewModel() {
        val mockHandle = spyViewModel(FooViewModel) {
            on { countItem() } doReturn 100
        }
        assertFalse(mockHandle.isCreated)

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(mockHandle.isCreated)
                assertSame(mockHandle.viewModelStoreOwner, activity)
                assertSame(mockHandle.mock, activity.fooViewModel)
                assertEquals(10, mockHandle.savedState["itemCount"])

                verify(mockHandle.mock, only()).greeting()
                verify(mockHandle.mock, never()).countItem()

                assertEquals("Hello, I'm Foo.", activity.fooViewModel.greeting())
                assertEquals(100, activity.fooViewModel.countItem())
            }
        }
    }
}

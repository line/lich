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
package com.linecorp.lich.viewmodel.test.mockk

import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.linecorp.lich.viewmodel.test.clearAllMockViewModels
import io.mockk.every
import io.mockk.verify
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
            every { greeting() } returns "Hello mock!"
            every { countItem() } returns 42
        }
        assertFalse(mockHandle.isCreated)

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(mockHandle.isCreated)
                assertSame(mockHandle.viewModelStoreOwner, activity)
                assertSame(mockHandle.mock, activity.fooViewModel)
                assertEquals(setOf("itemCount"), mockHandle.savedStateHandle.keys())
                assertEquals(10, mockHandle.savedStateHandle["itemCount"])

                verify(exactly = 1) { mockHandle.mock.greeting() }
                verify(exactly = 0) { mockHandle.mock.countItem() }

                assertEquals("Hello mock!", activity.fooViewModel.greeting())
                assertEquals(42, activity.fooViewModel.countItem())
            }
        }
    }

    // Mocking of final classes is only supported for Android P or later.
    // https://mockk.io/ANDROID.html
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun mockViewModelFinal() {
        val mockHandle = mockViewModel(BarViewModel) {
            every { greeting() } returns "Hello final mock!"
            every { countItem() } returns 420
        }
        assertFalse(mockHandle.isCreated)

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(mockHandle.isCreated)
                assertSame(mockHandle.viewModelStoreOwner, activity)
                assertSame(mockHandle.mock, activity.barViewModel)
                assertEquals(setOf("itemCount"), mockHandle.savedStateHandle.keys())
                assertEquals(20, mockHandle.savedStateHandle["itemCount"])

                verify(exactly = 1) { mockHandle.mock.greeting() }
                verify(exactly = 0) { mockHandle.mock.countItem() }

                assertEquals("Hello final mock!", activity.barViewModel.greeting())
                assertEquals(420, activity.barViewModel.countItem())
            }
        }
    }

    @Test
    fun spyViewModel() {
        val mockHandle = spyViewModel(FooViewModel) {
            every { countItem() } returns 100
        }
        assertFalse(mockHandle.isCreated)

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(mockHandle.isCreated)
                assertSame(mockHandle.viewModelStoreOwner, activity)
                assertSame(mockHandle.mock, activity.fooViewModel)
                assertEquals(10, mockHandle.savedStateHandle["itemCount"])

                verify(exactly = 1) { mockHandle.mock.greeting() }
                verify(exactly = 0) { mockHandle.mock.countItem() }

                assertEquals("Hello, I'm Foo.", activity.fooViewModel.greeting())
                assertEquals(100, activity.fooViewModel.countItem())
            }
        }
    }

    // Mocking of final classes is only supported for Android P or later.
    // https://mockk.io/ANDROID.html
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun spyViewModelFinal() {
        val mockHandle = spyViewModel(BarViewModel) {
            every { countItem() } returns 200
        }
        assertFalse(mockHandle.isCreated)

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(mockHandle.isCreated)
                assertSame(mockHandle.viewModelStoreOwner, activity)
                assertSame(mockHandle.mock, activity.barViewModel)
                assertEquals(20, mockHandle.savedStateHandle["itemCount"])

                verify(exactly = 1) { mockHandle.mock.greeting() }
                verify(exactly = 0) { mockHandle.mock.countItem() }

                assertEquals("Hello, I'm Bar.", activity.barViewModel.greeting())
                assertEquals(200, activity.barViewModel.countItem())
            }
        }
    }
}

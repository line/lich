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
package com.linecorp.lich.viewmodel.test

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.internal.util.MockUtil
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ViewModelMocksTest {

    @After
    fun tearDown() {
        clearAllMockViewModels()
    }

    @Test
    fun simpleMocking() {
        val mockHandleX = mockViewModel(ViewModelX) {
            on { greeting() } doReturn "Hello mock!"
        }
        val mockHandleY = mockViewModel(ViewModelY) {
            on { askName() } doReturn "MockY"
        }

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(mockHandleX.isCreated)
                assertTrue(mockHandleY.isCreated)

                assertNotSame(activity.viewModelX, activity.testFragment.viewModelX)
                assertSame(activity.viewModelX, activity.testFragment.activityViewModelX)

                assertSame(mockHandleY.mock, activity.viewModelY)

                assertSame(mockHandleY.viewModelStoreOwner, activity)

                assertEquals("Hello mock!", activity.viewModelX.greeting())
                assertEquals("Hello mock!", activity.testFragment.viewModelX.greeting())
                assertEquals("MockY", activity.viewModelY.askName())
            }
        }
    }

    @Test
    fun mockingForEachViewModelStoreOwner() {
        setMockViewModel(ViewModelX) { viewModelStoreOwner ->
            when (viewModelStoreOwner) {
                is TestActivity ->
                    mock {
                        on { greeting() } doReturn "I am TestActivity's ViewModel."
                    }
                is TestFragment ->
                    mock {
                        on { greeting() } doReturn "I am TestFragment's ViewModel."
                    }
                else -> createRealViewModel(ViewModelX)
            }
        }

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotSame(activity.viewModelX, activity.testFragment.viewModelX)
                assertSame(activity.viewModelX, activity.testFragment.activityViewModelX)

                assertEquals("I am TestActivity's ViewModel.", activity.viewModelX.greeting())
                assertEquals(
                    "I am TestFragment's ViewModel.",
                    activity.testFragment.viewModelX.greeting()
                )
                assertEquals(
                    "I am TestActivity's ViewModel.",
                    activity.testFragment.activityViewModelX.greeting()
                )
            }
        }
    }

    @Test
    fun spying() {
        val spyHandleY = spyViewModel(ViewModelY)

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(spyHandleY.isCreated)

                assertEquals("My name is Y.", activity.viewModelY.askName())

                verify(spyHandleY.mock) {
                    // viewModelY.askName() was also called from TestActivity.onStart().
                    2 * { askName() }
                }
            }
        }
    }

    @Test
    fun clearMock() {
        mockViewModel(ViewModelX)
        val mockHandleY = mockViewModel(ViewModelY)

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(MockUtil.isMock(activity.viewModelX))
                assertTrue(MockUtil.isMock(activity.testFragment.viewModelX))
                assertTrue(MockUtil.isMock(activity.testFragment.activityViewModelX))

                assertSame(mockHandleY.mock, activity.viewModelY)
            }
        }

        clearMockViewModel(ViewModelX)

        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(MockUtil.isMock(activity.viewModelX))
                assertFalse(MockUtil.isMock(activity.testFragment.viewModelX))
                assertFalse(MockUtil.isMock(activity.testFragment.activityViewModelX))

                assertSame(mockHandleY.mock, activity.viewModelY)
            }
        }
    }
}

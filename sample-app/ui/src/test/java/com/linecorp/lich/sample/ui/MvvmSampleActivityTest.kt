/*
 * Copyright 2021 LINE Corporation
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
package com.linecorp.lich.sample.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.viewmodel.test.MockViewModelHandle
import com.linecorp.lich.viewmodel.test.mockitokotlin.mockViewModel
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

// Workaround for https://github.com/robolectric/robolectric/issues/6593
@Config(instrumentedPackages = ["androidx.loader.content"])
@RunWith(AndroidJUnit4::class)
class MvvmSampleActivityTest {

    private lateinit var context: Context

    private lateinit var mockViewModelHandle: MockViewModelHandle<SampleViewModel>

    private lateinit var mockCounterText: MutableLiveData<String>

    private lateinit var mockIsOperationEnabled: MutableLiveData<Boolean>

    private lateinit var mockIsLoading: MutableLiveData<Boolean>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockCounterText = MutableLiveData("5")
        mockIsOperationEnabled = MutableLiveData(true)
        mockIsLoading = MutableLiveData(false)
        mockViewModelHandle = mockViewModel(SampleViewModel) {
            on { counterText } doReturn mockCounterText
            on { isOperationEnabled } doReturn mockIsOperationEnabled
            on { isLoading } doReturn mockIsLoading
        }
    }

    @Test
    fun testCounter() {
        val intent = MvvmSampleActivity.newIntent(context, "counter")
        ActivityScenario.launch<MvvmSampleActivity>(intent).use { scenario ->

            scenario.onActivity {
                assertTrue(mockViewModelHandle.isCreated)
                assertEquals("counter", mockViewModelHandle.savedStateHandle["counterName"])
                assertSame(mockCounterText, mockViewModelHandle.mock.counterText)
            }

            onView(withId(R.id.counter_value)).check(matches(withText("5")))

            scenario.onActivity {
                mockCounterText.value = "42"
            }

            onView(withId(R.id.counter_value)).check(matches(withText("42")))
        }
    }

    @Test
    fun testCountUpButton() {
        val intent = MvvmSampleActivity.newIntent(context, "counter")
        ActivityScenario.launch<MvvmSampleActivity>(intent).use { scenario ->

            scenario.onActivity {
                assertTrue(mockViewModelHandle.isCreated)
                verify(mockViewModelHandle.mock, never()).countUp()
            }

            onView(withId(R.id.count_up_btn)).check(matches(isEnabled()))

            onView(withId(R.id.count_up_btn)).perform(click())

            scenario.onActivity {
                verify(mockViewModelHandle.mock, times(1)).countUp()
            }
        }
    }
}

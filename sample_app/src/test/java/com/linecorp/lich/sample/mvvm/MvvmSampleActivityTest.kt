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
package com.linecorp.lich.sample.mvvm

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
import com.linecorp.lich.sample.R
import com.linecorp.lich.viewmodel.test.MockViewModelHandle
import com.linecorp.lich.viewmodel.test.mockk.mockViewModel
import io.mockk.every
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

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
            every { counterText } returns mockCounterText
            every { isOperationEnabled } returns mockIsOperationEnabled
            every { isLoading } returns mockIsLoading
        }
    }

    @Test
    fun testCounter() {
        val intent = MvvmSampleActivity.newIntent(context, "counter")
        ActivityScenario.launch<MvvmSampleActivity>(intent).use { scenario ->

            scenario.onActivity {
                assertTrue(mockViewModelHandle.isCreated)
                assertEquals<String?>("counter", mockViewModelHandle.savedState["counterName"])
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
                verify(exactly = 0) { mockViewModelHandle.mock.countUp() }
            }

            onView(withId(R.id.count_up_btn)).check(matches(isEnabled()))

            onView(withId(R.id.count_up_btn)).perform(click())

            scenario.onActivity {
                verify(exactly = 1) { mockViewModelHandle.mock.countUp() }
            }
        }
    }
}

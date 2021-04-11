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
package com.linecorp.lich.savedstate

import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class SavedStatesTest {

    @Test
    fun testViewModels() {
        lateinit var activityViewModel: TestViewModel
        lateinit var fragmentViewModel: TestViewModel

        val intent = TestActivity.newIntent(ApplicationProvider.getApplicationContext())

        ActivityScenario.launch<TestActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.testFragment

                activityViewModel = ViewModelProvider(activity).get(TestViewModel::class.java)
                fragmentViewModel = ViewModelProvider(fragment).get(TestViewModel::class.java)
                assertNotSame(activityViewModel, fragmentViewModel)

                assertSame(activityViewModel, activity.testViewModel)
                assertSame(fragmentViewModel, fragment.testViewModel)
                assertSame(activityViewModel, fragment.testActivityViewModel)

                assertEquals(Uri.parse("http://example.com"), activityViewModel.param1)
                assertEquals("example", activityViewModel.param2)
                assertEquals(42, activityViewModel.param3)
                assertEquals(null, activityViewModel.param4.value)
                assertEquals("NONE", activityViewModel.param5.value)

                assertEquals(Uri.parse("http://example.jp"), fragmentViewModel.param1)
                assertEquals("TestFragment", fragmentViewModel.param2)
                assertEquals(200, fragmentViewModel.param3)
                assertEquals(Uri.EMPTY, fragmentViewModel.param4.value)
                assertEquals("TestLiveData", fragmentViewModel.param5.value)

                activityViewModel.param1 = null
                activityViewModel.param2 = "updated"
                activityViewModel.param3 = 420
                activityViewModel.param4.value = Uri.parse("http://example.com/foo")
                activityViewModel.param5.value = "NEW"
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                // Even after recreation, the values of SavedStateHandle must be kept.
                val testViewModel = activity.testViewModel
                assertEquals(null, testViewModel.param1)
                assertEquals("updated", testViewModel.param2)
                assertEquals(420, testViewModel.param3)
                assertEquals(Uri.parse("http://example.com/foo"), testViewModel.param4.value)
                assertEquals("NEW", testViewModel.param5.value)
            }
        }
    }

    @Test
    fun createForTestingFromMap() {
        val savedStateHandle = createSavedStateHandleForTesting(
            "param1" to Uri.parse("http://example.jp"),
            "param2" to "TestFragment",
            "param3" to 200,
            "param4" to Uri.EMPTY,
            "param5" to "TestLiveData"
        )

        val testViewModel = TestViewModel(savedStateHandle)
        assertEquals(Uri.parse("http://example.jp"), testViewModel.param1)
        assertEquals("TestFragment", testViewModel.param2)
        assertEquals(200, testViewModel.param3)
        assertEquals(Uri.EMPTY, testViewModel.param4.value)
        assertEquals("TestLiveData", testViewModel.param5.value)
    }

    @Test
    fun createForTestingFromArgs() {
        val savedStateHandle = createSavedStateHandleForTesting(
            TestViewModelArgs(
                param1 = Uri.parse("http://example.jp"),
                param2 = "TestFragment",
                param3 = 200,
                param4 = Uri.EMPTY,
                param5 = "TestLiveData"
            )
        )

        val testViewModel = TestViewModel(savedStateHandle)
        assertEquals(Uri.parse("http://example.jp"), testViewModel.param1)
        assertEquals("TestFragment", testViewModel.param2)
        assertEquals(200, testViewModel.param3)
        assertEquals(Uri.EMPTY, testViewModel.param4.value)
        assertEquals("TestLiveData", testViewModel.param5.value)
    }
}

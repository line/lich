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
package com.linecorp.lich.viewmodel

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ViewModelTest {

    @Test
    fun startActivityAndFragment() {
        lateinit var activityViewModel: TestViewModel
        lateinit var fragmentViewModel: TestViewModel

        val context: Context = ApplicationProvider.getApplicationContext()
        val intent = TestActivity.newIntent(context)
        ActivityScenario.launch<TestActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                println("Activity started.")

                val fragment = activity.testFragment

                activityViewModel = activity.getViewModel(TestViewModel)
                fragmentViewModel = fragment.getViewModel(TestViewModel)
                assertNotSame(activityViewModel, fragmentViewModel)
                assertSame(activityViewModel, fragment.getActivityViewModel(TestViewModel))

                assertSame(activityViewModel, activity.testViewModel)
                assertSame(fragmentViewModel, fragment.testViewModel)
                assertSame(activityViewModel, fragment.testActivityViewModel)

                assertSame(context, activityViewModel.context)
                assertSame(context, fragmentViewModel.context)

                assertFalse(activityViewModel.isCleared)
                assertFalse(fragmentViewModel.isCleared)

                assertEquals("xyz", activityViewModel.param1)
                assertEquals(42, activityViewModel.param2)
                assertEquals(null, activityViewModel.param3.value)
                assertEquals("abc", fragmentViewModel.param1)
                assertEquals(100, fragmentViewModel.param2)
                assertEquals("fooBar", fragmentViewModel.param3.value)
            }

            println("Recreating Activity...")
            scenario.recreate()

            scenario.onActivity { activity ->
                println("Activity recreated.")

                // Even after recreation, the instances of ViewModels must be kept.
                assertSame(activityViewModel, activity.testViewModel)
                assertSame(fragmentViewModel, activity.testFragment.testViewModel)
                assertSame(activityViewModel, activity.testFragment.testActivityViewModel)

                assertFalse(activityViewModel.isCleared)
                assertFalse(fragmentViewModel.isCleared)
            }

            println("Destroying Activity...")
            scenario.moveToState(Lifecycle.State.DESTROYED)

            println("Activity destroyed.")

            assertTrue(activityViewModel.isCleared)
            assertTrue(fragmentViewModel.isCleared)
        }

        ActivityScenario.launch<TestActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                println("Another Activity started.")

                assertNotSame(activityViewModel, activity.testViewModel)
                assertNotSame(fragmentViewModel, activity.testFragment.testViewModel)
                assertSame(activity.testViewModel, activity.testFragment.testActivityViewModel)

                assertFalse(activity.testViewModel.isCleared)
                assertFalse(activity.testFragment.testViewModel.isCleared)
            }
        }
    }
}

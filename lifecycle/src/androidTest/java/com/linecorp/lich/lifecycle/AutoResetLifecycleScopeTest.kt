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
package com.linecorp.lich.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Job
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AutoResetLifecycleScopeTest {

    @Test
    fun testLifecycleScope() {
        ActivityScenario.launch(TestActivity::class.java).use { scenario ->
            lateinit var job: Job

            scenario.onActivity { activity ->
                println("Activity started.")

                job = activity.job
                assertTrue(job.isActive)
            }

            scenario.moveToState(Lifecycle.State.CREATED)

            scenario.onActivity {
                println("Activity stopped.")

                assertTrue(job.isCancelled)
            }

            scenario.moveToState(Lifecycle.State.RESUMED)

            scenario.onActivity { activity ->
                println("Activity resumed again.")

                assertNotSame(job, activity.job)
                job = activity.job
                assertTrue(job.isActive)
            }

            scenario.moveToState(Lifecycle.State.DESTROYED)

            println("Activity destroyed.")

            assertTrue(job.isCancelled)
        }
    }
}

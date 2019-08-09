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
package com.linecorp.lich.component

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
@Config(application = TestApplication::class)
class ConcurrencyTest {

    private lateinit var context: Context

    private lateinit var executorService: ExecutorService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        executorService = Executors.newFixedThreadPool(CONCURRENCY)
    }

    @After
    fun tearDown() {
        executorService.shutdown()
    }

    @Test
    fun concurrentGet() {
        getComponentInParallel(ComponentX)
    }

    @Test
    fun concurrentGetSlow() {
        getComponentInParallel(SlowComponent)
    }

    private fun <T : Any> getComponentInParallel(componentFactory: ComponentFactory<T>) {
        val startBarrier = CyclicBarrier(CONCURRENCY)
        val tasks = List(CONCURRENCY) { index ->
            Callable {
                println("Task: $index")
                startBarrier.await()
                context.getComponent(componentFactory)
            }
        }

        val components = executorService.invokeAll(tasks).map { it.get() }

        val c = context.getComponent(componentFactory)
        components.forEach {
            assertSame(c, it)
        }
    }

    companion object {
        const val CONCURRENCY: Int = 8
    }
}

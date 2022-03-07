/*
 * Copyright 2022 LINE Corporation
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
package com.linecorp.lich.viewmodel.compose

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.savedstate.initial
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LichViewModelTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun differentViewModelStoreOwner() {
        var firstViewModel: SimpleViewModel? = null
        var secondViewModel: SimpleViewModel? = null
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "One") {
                composable("One") {
                    // firstViewModel is owned by the current NavBackStackEntry.
                    firstViewModel = lichViewModel(SimpleViewModel)
                }
            }
            // secondViewModel is owned by TestActivity.
            secondViewModel = lichViewModel(SimpleViewModel)
        }
        composeTestRule.waitForIdle()

        assertNotNull(firstViewModel) {
            assertEquals("", it.param)
            assertFalse(it.isCleared)
        }
        assertNotNull(secondViewModel) {
            assertEquals("", it.param)
            assertFalse(it.isCleared)
        }
        assertNotSame(firstViewModel, secondViewModel)
    }

    @Test
    fun navigateWithParamThenPopBackStack() {
        var firstViewModel: SimpleViewModel? = null
        var secondViewModel: SimpleViewModel? = null
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "One") {
                composable("One") {
                    firstViewModel = lichViewModel(SimpleViewModel)
                    NavigateButton("Two") { navController.navigate("Two/foo") }
                }
                composable("Two/{param}") {
                    secondViewModel = lichViewModel(SimpleViewModel)
                    NavigateButton("One") { navController.popBackStack() }
                }
            }
        }
        composeTestRule.waitForIdle()

        assertNotNull(firstViewModel) {
            assertEquals("", it.param)
            assertFalse(it.isCleared)
        }
        assertNull(secondViewModel)

        composeTestRule.onNodeWithText("Navigate to Two").performClick()
        composeTestRule.waitForIdle()

        assertNotNull(firstViewModel) {
            assertFalse(it.isCleared)
        }
        assertNotNull(secondViewModel) {
            assertEquals("foo", it.param)
            assertFalse(it.isCleared)
        }
        assertNotSame(firstViewModel, secondViewModel)

        composeTestRule.onNodeWithText("Navigate to One").performClick()
        composeTestRule.waitForIdle()

        assertNotNull(firstViewModel) {
            assertFalse(it.isCleared)
        }
        assertNotNull(secondViewModel) {
            assertTrue(it.isCleared)
        }
    }

    @Test
    fun sameParentViewModelAcrossRoutes() {
        var firstViewModel: SimpleViewModel? = null
        var secondViewModel: SimpleViewModel? = null
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(navController, startDestination = "Main") {
                navigation(startDestination = "One", route = "Main") {
                    composable("One") {
                        firstViewModel = lichViewModel(
                            SimpleViewModel,
                            navController.getBackStackEntry("Main")
                        )
                        NavigateButton("Two") { navController.navigate("Two/foo") }
                    }
                    composable("Two/{param}") {
                        secondViewModel = lichViewModel(
                            SimpleViewModel,
                            navController.getBackStackEntry("Main")
                        )
                        NavigateButton("One") { navController.popBackStack() }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        assertNotNull(firstViewModel) {
            assertEquals("", it.param)
            assertFalse(it.isCleared)
        }
        assertNull(secondViewModel)

        composeTestRule.onNodeWithText("Navigate to Two").performClick()
        composeTestRule.waitForIdle()

        assertNotNull(firstViewModel)
        assertNotNull(secondViewModel)
        assertSame(firstViewModel, secondViewModel)

        composeTestRule.onNodeWithText("Navigate to One").performClick()
        composeTestRule.waitForIdle()

        assertNotNull(firstViewModel) {
            assertFalse(it.isCleared)
        }
        assertSame(firstViewModel, secondViewModel)
    }

    @Suppress("TestFunctionName")
    @Composable
    private fun NavigateButton(text: String, onClick: () -> Unit = {}) {
        Button(onClick = onClick) {
            Text(text = "Navigate to $text")
        }
    }

    class TestActivity : ComponentActivity()

    class SimpleViewModel(savedStateHandle: SavedStateHandle) : AbstractViewModel() {

        val param: String by savedStateHandle.initial("")

        var isCleared: Boolean = false

        override fun onCleared() {
            isCleared = true
        }

        companion object : ViewModelFactory<SimpleViewModel>() {
            override fun createViewModel(
                context: Context,
                savedStateHandle: SavedStateHandle
            ): SimpleViewModel = SimpleViewModel(savedStateHandle)
        }
    }
}

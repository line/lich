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
package com.linecorp.lich.component.compose

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.getComponent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class LichComponentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun differentViewModelStoreOwner() {
        var simpleComponent: SimpleComponent? = null
        composeTestRule.setContent {
            simpleComponent = lichComponent(SimpleComponent)
        }
        composeTestRule.waitForIdle()

        assertNotNull(simpleComponent) {
            val context: Context = ApplicationProvider.getApplicationContext()
            assertSame(context, it.context)
            assertSame(context.getComponent(SimpleComponent), it)
        }
    }

    class TestActivity : ComponentActivity()

    class SimpleComponent(val context: Context) {
        companion object : ComponentFactory<SimpleComponent>() {
            override fun createComponent(context: Context): SimpleComponent =
                SimpleComponent(context)
        }
    }
}

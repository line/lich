package com.linecorp.lich.sample.feature.bar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.linecorp.lich.component.getComponent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MockBarFeatureFacadeTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun getMessage() {
        val barFeatureFacade = context.getComponent(BarFeatureFacade)
        assertEquals("I am MockBarFeatureFacade.", barFeatureFacade.getMessage())
    }
}

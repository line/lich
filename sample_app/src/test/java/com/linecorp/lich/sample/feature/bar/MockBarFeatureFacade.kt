package com.linecorp.lich.sample.feature.bar

import com.google.auto.service.AutoService

/**
 * A mock implementation of [BarFeatureFacade].
 * This is used if no other implementation of BarFeatureFacade exists in the runtime classpath.
 */
@AutoService(BarFeatureFacade::class)
class MockBarFeatureFacade : BarFeatureFacade {

    // Since this class doesn't implement ServiceLoaderComponent,
    // its loadPriority is considered Int.MIN_VALUE.

    override fun launchBarFeatureActivity() = Unit

    override fun getMessage(): String = "I am MockBarFeatureFacade."
}

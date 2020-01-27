package com.linecorp.lich.static_analysis

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import com.linecorp.lich.static_analysis.detectors.OptionalArgumentRequiredDetector
import com.google.auto.service.AutoService
import com.linecorp.lich.static_analysis.detectors.LichFactoryDetector

/**
 * A class to register custom [Issue]s
 */
@AutoService(IssueRegistry::class)
class LichIssueRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() = listOf(
            OptionalArgumentRequiredDetector.ISSUE,
            LichFactoryDetector.TYPE_ARGUMENT_ISSUE,
            LichFactoryDetector.OBJECT_ISSUE
        )

    override val api: Int = CURRENT_API

    override val minApi: Int = MIN_API

    companion object {
        const val MIN_API = 2 // Corresponds to android gradle plugin 3.2+
    }
}

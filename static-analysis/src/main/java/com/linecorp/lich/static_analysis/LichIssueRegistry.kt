package com.linecorp.lich.static_analysis

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import com.linecorp.lich.static_analysis.detectors.LichFactoryDetector
import com.linecorp.lich.static_analysis.detectors.OptionalArgumentRequiredDetector

/**
 * A class to register custom [Issue]s
 */
class LichIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = listOf(
        OptionalArgumentRequiredDetector.ISSUE,
        LichFactoryDetector.TYPE_ARGUMENT_ISSUE,
        LichFactoryDetector.OBJECT_ISSUE
    )

    override val api: Int = CURRENT_API
}

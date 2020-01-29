package com.linecorp.lich.static_analysis

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.linecorp.lich.static_analysis.detectors.OptionalArgumentRequiredDetector

class OptionalArgumentRequiredDetectorTest :
    LichLintDetectorTest(testAssetsFolder = "OptionalArgumentRequiredDetector") {

    private val argumentDependencies =
        assetsFolder.getContent("ViewModelArgumentDependencies.kt")

    fun testViewModelArgumentsNoWarnings() {
        val noErrorsTest = assetsFolder.getContent("ViewModelOptionalArgumentTestNoWarnings.kt")

        lint().files(
            kotlin(argumentDependencies),
            kotlin(noErrorsTest)
        ).allowCompilationErrors(false)
            .run()
            .expect("No warnings.")
    }

    fun testViewModelArgumentsShowWarnings() {
        val testWithErrors = assetsFolder.getContent("ViewModelOptionalArgumentTestShowsError.kt")

        lint().files(
            kotlin(argumentDependencies),
            kotlin(testWithErrors)
        ).allowCompilationErrors(false)
            .run()
            .expect(
                """
                src/com/linecorp/lich/viewmodel/ArgumentTest.kt:5: Error: Optional argument initialized as required. [ArgumentInitializationInconsistency]
    val test: Int by savedState.required()
                                ~~~~~~~~
1 errors, 0 warnings""".trimIndent()
            )
    }

    override fun getDetector(): Detector = OptionalArgumentRequiredDetector()

    override fun getIssues(): MutableList<Issue> {
        return mutableListOf(OptionalArgumentRequiredDetector.ISSUE)
    }
}

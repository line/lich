package com.linecorp.lich.static_analysis

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.linecorp.lich.static_analysis.detectors.OptionalArgumentRequiredDetector
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class OptionalArgumentRequiredDetectorTest : LintDetectorTest() {
    private val argumentDependencies =
        getContent(TEST_DIR, "ViewModelArgumentDependencies.kt")

    fun testViewModelArgumentsNoWarnings() {
        val noErrorsTest = getContent(TEST_DIR, "ViewModelOptionalArgumentTestNoWarnings.kt")

        lint().files(
            kotlin(argumentDependencies),
            kotlin(noErrorsTest)
        ).allowCompilationErrors(false)
            .run()
            .expect("No warnings.")
    }

    fun testViewModelArgumentsShowWarnings() {
        val testWithErrors =
            getContent(TEST_DIR, filename = "ViewModelOptionalArgumentTestShowsError.kt")

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

    private fun getContent(dir: File, filename: String): String {
        return String(
            Files.readAllBytes(Paths.get(File(dir, filename).toURI()))
        )
    }

    companion object {
        private val TEST_DIR = File("src/test/test-assets/OptionalArgumentRequiredDetector")
    }
}

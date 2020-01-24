package com.linecorp.lich.static_analysis

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import java.io.File

/**
 * A class to simplify access to test assets.
 */
abstract class LichLintDetectorTest(testAssetsFolder: String) : LintDetectorTest() {
    protected val assetsFolder = File("src/test/test-assets/$testAssetsFolder")

    protected fun File.getContent(filename: String): String {
        return File(this, filename).readText()
    }
}

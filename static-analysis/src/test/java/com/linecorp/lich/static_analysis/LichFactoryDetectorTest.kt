package com.linecorp.lich.static_analysis

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.linecorp.lich.static_analysis.detectors.LichFactoryDetector

class LichFactoryDetectorTest : LichLintDetectorTest(testAssetsFolder = "LichFactoryDetector") {

    fun testComponentFactory() {
        val dependencies = assetsFolder.getContent("ComponentFactoryDependencies.kt")
        val componentFactoryTest = assetsFolder.getContent("ComponentFactoryTest.kt")

        lint().files(
            kotlin(dependencies),
            kotlin(componentFactoryTest)
        ).allowCompilationErrors(false)
            .run()
            .expect("""
                src/com/linecorp/lich/component/ClassFactory.kt:5: Warning: Factories generally should be implemented by object declarations. [FactoryShouldBeObject]
                class ClassFactory : ComponentFactory<ClassFactory>()
                      ~~~~~~~~~~~~
                src/com/linecorp/lich/component/ClassFactory.kt:18: Error: This ComponentFactory's type argument should be TestBar. [InvalidTypeArgumentInFactory]
                    companion object : ComponentFactory<TestFoo>()
                                       ~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 1 warnings
            """.trimIndent())
    }

    fun testViewModelFactory() {
        val dependencies = assetsFolder.getContent("ViewModelFactoryDependencies.kt")
        val viewModelFactoryTest = assetsFolder.getContent("ViewModelFactoryTest.kt")

        lint().files(
            kotlin(dependencies),
            kotlin(viewModelFactoryTest)
        ).allowCompilationErrors(false)
            .run()
            .expect("""
                src/com/linecorp/lich/viewmodel/TestFoo.kt:15: Warning: Factories generally should be implemented by object declarations. [FactoryShouldBeObject]
                class ClassFactory : ViewModelFactory<ClassFactory>()
                      ~~~~~~~~~~~~
                src/com/linecorp/lich/viewmodel/TestFoo.kt:10: Error: This ViewModelFactory's type argument should be TestBar. [InvalidTypeArgumentInFactory]
                    companion object : ViewModelFactory<TestFoo>()
                                       ~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 1 warnings
            """.trimIndent())
    }

    override fun getDetector(): Detector = LichFactoryDetector()

    override fun getIssues(): MutableList<Issue> {
        return mutableListOf(
            LichFactoryDetector.TYPE_ARGUMENT_ISSUE,
            LichFactoryDetector.OBJECT_ISSUE
        )
    }
}

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
                src/com/linecorp/lich/component/Api.kt:7: Error: Factories should be implemented by object declarations. [FactoryShouldBeObject]
                class ClassFactory : ComponentFactory<Api>()
                      ~~~~~~~~~~~~
                src/com/linecorp/lich/component/Api.kt:14: Error: Factories should be implemented by object declarations. [FactoryShouldBeObject]
                val expressionFactory = object : ComponentFactory<Api>() {}
                                        ~~~~~~
                src/com/linecorp/lich/component/Api.kt:23: Error: This ComponentFactory's type argument should be TestBar. [InvalidTypeArgumentInFactory]
                    companion object : ComponentFactory<TestFoo>()
                                       ~~~~~~~~~~~~~~~~~~~~~~~~~
                3 errors, 0 warnings
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
                src/com/linecorp/lich/viewmodel/FooViewModel.kt:7: Error: Factories should be implemented by object declarations. [FactoryShouldBeObject]
                class ClassFactory : ViewModelFactory<FooViewModel>()
                      ~~~~~~~~~~~~
                src/com/linecorp/lich/viewmodel/FooViewModel.kt:14: Error: Factories should be implemented by object declarations. [FactoryShouldBeObject]
                val expressionFactory = object : ViewModelFactory<FooViewModel>() {}
                                        ~~~~~~
                src/com/linecorp/lich/viewmodel/FooViewModel.kt:23: Error: This ViewModelFactory's type argument should be TestBar. [InvalidTypeArgumentInFactory]
                    companion object : ViewModelFactory<TestFoo>()
                                       ~~~~~~~~~~~~~~~~~~~~~~~~~
                3 errors, 0 warnings
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

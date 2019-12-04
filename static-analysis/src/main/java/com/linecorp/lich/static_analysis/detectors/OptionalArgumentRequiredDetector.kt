package com.linecorp.lich.static_analysis.detectors

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.linecorp.lich.static_analysis.extensions.evaluateOrDefault
import com.linecorp.lich.static_analysis.extensions.findClosestParentByType
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UField

/**
 * Detector which checks whether a viewmodel's required argument is annotated as optional.
 */
class OptionalArgumentRequiredDetector : Detector(), SourceCodeScanner {
    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            "ArgumentInitializationInconsistency",
            "Argument inconsistency: the argument is annotated as optional but it is " +
                "initialized as required.",
            "An argument annotated as optional can not be initialized as `required`. Remove the " +
                "annotation or provide another initialization mechanism like `initial`.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            Implementation(OptionalArgumentRequiredDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        private const val argumentAnnotationQualifiedName = "com.linecorp.lich.viewmodel.Argument"
        private const val savedStateQualifiedName = "com.linecorp.lich.viewmodel.SavedState"
    }

    private fun JavaContext.report(node: UCallExpression) {
        report(
            ISSUE,
            node,
            getNameLocation(node),
            "Optional argument initialized as required."
        )
    }

    /**
     * Returns true when the [node] is annotated as a optional argument.
     */
    private fun isOptionalArgument(node: UCallExpression): Boolean {
        val field = node.findClosestParentByType<UField>() ?: return false
        val argumentAnnotation = field.annotations.find {
            it.qualifiedName == argumentAnnotationQualifiedName
        } ?: return false
        val isOptionalAttribute =
            argumentAnnotation.attributeValues.find { it.name == "isOptional" } ?: return false

        return isOptionalAttribute.expression.evaluateOrDefault(false)
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (isKotlin(method) &&
            context.evaluator.isMemberInClass(method, savedStateQualifiedName)
        ) {
            if (isOptionalArgument(node)) {
                context.report(node)
            }
        }
    }

    override fun getApplicableMethodNames(): List<String>? = listOf("required")
}

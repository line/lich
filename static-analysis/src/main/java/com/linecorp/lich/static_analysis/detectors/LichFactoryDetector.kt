package com.linecorp.lich.static_analysis.detectors

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiClassType
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.linecorp.lich.static_analysis.extensions.findClosestParentByType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isAbstract
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UTypeReferenceExpression

/**
 * A detector to find possible misuses of Lich component factories.
 */
class LichFactoryDetector : Detector(), SourceCodeScanner {
    companion object {
        @JvmStatic
        val TYPE_ARGUMENT_ISSUE: Issue = Issue.create(
            "InvalidTypeArgumentInFactory",
            "The factory's type argument should be the companion object's parent class.",
            "Factories in companion objects should never create instances of classes outside of " +
                "the scope of the companion object's parent class. Change the type argument of " +
                "this factory to the companion object's parent class or use a top level *object* " +
                "declaration instead.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            Implementation(LichFactoryDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
        @JvmStatic
        val OBJECT_ISSUE: Issue = Issue.create(
            "FactoryShouldBeObject",
            "It is better practice to implement factories using an *object* declaration.",
            "Factories should be implemented by *object* declarations in order to avoid multiple " +
                "instances of the same factory.",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            Implementation(LichFactoryDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun applicableSuperClasses(): List<String> = Factory.qualifiedNames

    override fun visitClass(context: JavaContext, declaration: UClass) {
        if (!isKotlin(declaration)) {
            return
        }
        val declarationPsi = declaration.sourcePsi
        if (declarationPsi !is KtObjectDeclaration) {
            if (declarationPsi is KtClass && !declarationPsi.isAbstract()) {
                context.reportObject(declaration)
            }
        }
        val factorySupertypeDeclaration = declaration.findFactorySupertype() ?: return
        val factoryType = Factory.find(factorySupertypeDeclaration.getQualifiedName()) ?: return
        val parentClass = declaration.findClosestParentByType<UClass>() ?: return
        val typeArgument = factorySupertypeDeclaration.typeArgument
        val typeArgumentCanonical = typeArgument?.canonicalText.orEmpty()
        if (parentClass.qualifiedName != typeArgumentCanonical) {
            context.reportTypeArgument(factorySupertypeDeclaration, parentClass, factoryType)
        }
    }

    /**
     * Finds the Lich factory super class among the analyzed class' super types:
     *
     * ```
     *     // snip...
     *
     *     companion object : FooInterface, ViewModelFactory<FooViewModel>() {
     *         override fun createViewModel(context: Context, savedState: SavedState): FooViewModel =
     *             FooViewModel(context, savedState)
     *     }
     * ```
     * In this case, we are only interested in [ViewModelFactory] even though the companion object
     * is inheriting from several interfaces/classes.
     *
     * Note: there is a limitation with the current implementation. If the companion object is
     * implementing both [ViewModelFactory] and [ComponentFactory] it will only report the first
     * one.
     */
    private fun UClass.findFactorySupertype(): UTypeReferenceExpression? =
        this.uastSuperTypes.find { superClass -> Factory.contains(superClass.getQualifiedName()) }

    /**
     * Returns the type argument, if any, of the provided class reference.
     */
    private val UTypeReferenceExpression.typeArgument: PsiClassReferenceType?
        get() = (type as? PsiClassType)?.typeArguments()?.first() as? PsiClassReferenceType

    private fun JavaContext.reportTypeArgument(
        node: UElement,
        parentClass: UClass,
        factory: Factory
    ) {
        // Provides a quick fix to replace the type argument with the correct class name.
        val fix: LintFix = LintFix.create().replace()
            .name("Replace ${factory.shortName}'s type argument with ${parentClass.name}")
            .with("${factory.shortName}<${parentClass.name}>")
            .reformat(true)
            .shortenNames()
            .build()

        report(
            TYPE_ARGUMENT_ISSUE,
            node,
            getNameLocation(node),
            "This ${factory.shortName}'s type argument should be ${parentClass.name}.",
            fix
        )
    }

    private fun JavaContext.reportObject(node: UElement) {
        report(
            OBJECT_ISSUE,
            node,
            getNameLocation(node),
            "Factories generally should be implemented by *object* declarations."
        )
    }

    /**
     * An enum representing the different types of factories provided by Lich.
     *
     * @property qualifiedName holds the fully qualified name of the the factory.
     * @property shortName holds the abbreviated name of the factory.
     */
    private enum class Factory(val qualifiedName: String, val shortName: String) {
        VIEWMODEL("com.linecorp.lich.viewmodel.ViewModelFactory", "ViewModelFactory"),
        COMPONENT("com.linecorp.lich.component.ComponentFactory", "ComponentFactory");

        companion object {
            val qualifiedNames: List<String> = values().map { it.qualifiedName }

            fun contains(qualifiedName: String?): Boolean = qualifiedNames.contains(qualifiedName)

            fun find(qualifiedName: String?): Factory? =
                values().find { it.qualifiedName == qualifiedName }
        }
    }
}

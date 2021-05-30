/*
 * Copyright 2021 LINE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linecorp.lich.savedstate.compiler

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.innerArguments
import com.google.devtools.ksp.outerType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName

class KspViewModelArgsProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(GENERATE_ARGS_ANNOTATION_NAME).forEach { symbol ->
            if (symbol is KSClassDeclaration) {
                generateViewModelArgs(symbol, resolver)
            } else {
                logger.warn("@GenerateArgs is only applicable to class declarations.", symbol)
            }
        }
        return emptyList()
    }

    private fun generateViewModelArgs(viewModelClass: KSClassDeclaration, resolver: Resolver) {
        viewModelClass.createViewModelInfo(resolver)?.let { viewModelInfo ->
            val dependencies = viewModelClass.containingFile
                ?.let { Dependencies(false, it) } ?: Dependencies.ALL_FILES
            codeGenerator.createNewFile(
                dependencies,
                viewModelInfo.argsClassName.packageName,
                viewModelInfo.argsClassName.simpleName
            ).writer().use { writer ->
                ViewModelArgsWriter.writeToAppendable(viewModelInfo, writer)
            }
        }
    }

    private fun KSClassDeclaration.createViewModelInfo(resolver: Resolver): ViewModelInfo? {
        val viewModelClassName = toRawClassName()
        if (viewModelClassName == null || viewModelClassName.simpleNames.size != 1) {
            logger.warn("@GenerateArgs is only applicable to top-level classes.", this)
            return null
        }
        val argsClassName = viewModelClassName.peerClass("${viewModelClassName.simpleName}Args")
        val arguments =
            getDeclaredProperties().mapNotNull { it.createArgumentInfo(resolver) }.toList()
        return ViewModelInfo(viewModelClassName, argsClassName, arguments)
    }

    private fun KSPropertyDeclaration.createArgumentInfo(
        resolver: Resolver
    ): ViewModelArgumentInfo? {
        val annotation = getAnnotation(ARGUMENT_ANNOTATION_NAME) ?: return null

        if (extensionReceiver != null) {
            logger.warn("@Argument is not applicable to extension properties.", this)
            return null
        }

        val propertyName = simpleName.asString()
        val isOptional = annotation.getArgumentValue(IS_OPTIONAL_PARAM_NAME) as? Boolean ?: false

        val argumentType = type.resolve().extractTypeArgumentIfLiveData(resolver)
        val argumentTypeName = argumentType.toTypeName()
        if (argumentTypeName == null) {
            logger.warn("Cannot resolve the type of property `$propertyName`.", this)
            return null
        }

        val putMethodName = resolvePutMethodName(
            argumentType.makeNotNullable(),
            argumentTypeName,
            isOptional,
            resolver
        )
        if (putMethodName == null) {
            logger.warn("Type `$argumentTypeName` cannot be put into a Bundle.", this)
        }

        return ViewModelArgumentInfo(propertyName, argumentTypeName, isOptional, putMethodName)
    }

    private fun resolvePutMethodName(
        type: KSType,
        typeName: TypeName,
        isOptional: Boolean,
        resolver: Resolver
    ): String? {
        fun isAssignableTo(qualifiedName: String): Boolean {
            val superType = resolver.getClassDeclarationByName(qualifiedName) ?: return false
            return superType.asStarProjectedType().isAssignableFrom(type)
        }

        fun hasTypeArgumentAssignableTo(qualifiedName: String): Boolean {
            val superType = resolver.getClassDeclarationByName(qualifiedName) ?: return false
            val argumentType = type.arguments.firstOrNull()
                ?.takeUnless { it.variance == Variance.CONTRAVARIANT }
                ?.type?.resolve()?.makeNotNullable() ?: return false
            return superType.asStarProjectedType().isAssignableFrom(argumentType)
        }

        return BundleMethodResolver.resolvePutMethodName(
            typeName,
            isOptional,
            ::isAssignableTo,
            ::hasTypeArgumentAssignableTo
        )
    }

    private fun KSType.extractTypeArgumentIfLiveData(resolver: Resolver): KSType =
        if (arguments.size == 1 && declaration.qualifiedName?.asString() in liveDataClasses) {
            arguments[0].type?.resolve() ?: resolver.builtIns.anyType.makeNullable()
        } else this

    private fun KSAnnotated.getAnnotation(qualifiedName: String): KSAnnotation? =
        annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName
        }

    private fun KSAnnotation.getArgumentValue(name: String): Any? =
        arguments.firstOrNull { it.name?.asString() == name }?.value

    private fun KSType.toTypeName(): TypeName? {
        val rawType = declaration.toRawClassName() ?: return null
        val typeArguments = innerArguments.map { it.toTypeName() ?: return null }
        val outerTypeName = outerType?.toTypeName() as? ParameterizedTypeName
        val nonNullType = when {
            outerTypeName != null -> outerTypeName.nestedClass(rawType.simpleName, typeArguments)
            typeArguments.isEmpty() -> rawType
            else -> rawType.parameterizedBy(typeArguments)
        }
        return if (isMarkedNullable) nonNullType.copy(nullable = true) else nonNullType
    }

    private fun KSDeclaration.toRawClassName(): ClassName? {
        if (this !is KSClassDeclaration && this !is KSTypeAlias) return null
        val qualified = qualifiedName?.asString() ?: return null
        val pkgName = packageName.asString()
        val simpleNames = if (pkgName.isEmpty()) {
            qualified
        } else {
            if (!qualified.startsWith("$pkgName.")) return null
            qualified.substring(pkgName.length + 1)
        }
        return ClassName(pkgName, simpleNames.split('.'))
    }

    private fun KSTypeArgument.toTypeName(): TypeName? {
        if (variance == Variance.STAR) return STAR
        val typeName = type?.resolve()?.toTypeName() ?: return null
        return when (variance) {
            Variance.COVARIANT -> WildcardTypeName.producerOf(typeName)
            Variance.CONTRAVARIANT -> WildcardTypeName.consumerOf(typeName)
            else -> typeName
        }
    }

    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
            KspViewModelArgsProcessor(environment.codeGenerator, environment.logger)
    }
}

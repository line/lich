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

import com.linecorp.lich.savedstate.compiler.BundleMethodResolver.rawCanonicalName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName
import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmClassVisitor
import kotlinx.metadata.KmExtensionType
import kotlinx.metadata.KmPropertyExtensionVisitor
import kotlinx.metadata.KmPropertyVisitor
import kotlinx.metadata.KmTypeVisitor
import kotlinx.metadata.KmVariance
import kotlinx.metadata.flagsOf
import kotlinx.metadata.isLocal
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.JvmPropertyExtensionVisitor
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic
import kotlinx.metadata.ClassName as KmClassName

class KaptViewModelArgsProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf(GENERATE_ARGS_ANNOTATION_NAME, DEPRECATED_GENERATE_ARGS_ANNOTATION_NAME)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        annotations.forEach { annotation ->
            roundEnv.getElementsAnnotatedWith(annotation).forEach { element ->
                if (element is TypeElement) {
                    generateViewModelArgs(element)
                } else {
                    logWarn("@GenerateArgs is only applicable to class declarations.", element)
                }
            }
        }
        return true
    }

    private fun generateViewModelArgs(classElement: TypeElement) {
        val metadataAnnotation = classElement.getAnnotation(Metadata::class.java)
        if (metadataAnnotation == null) {
            logWarn("Kotlin Metadata annotation is not found.", classElement)
            return
        }
        val metadata = KotlinClassMetadata.read(metadataAnnotation.asClassHeader())
        if (metadata !is KotlinClassMetadata.Class) {
            logWarn("Unsupported metadata type: $metadata", classElement)
            return
        }
        val classVisitor = ClassVisitor().also { metadata.accept(it) }

        classVisitor.createViewModelInfo(classElement)?.let { viewModelInfo ->
            ViewModelArgsWriter.writeToFiler(viewModelInfo, classElement, processingEnv.filer)
        }
    }

    private fun ClassVisitor.createViewModelInfo(classElement: TypeElement): ViewModelInfo? {
        val viewModelClassName = className
        if (viewModelClassName == null || viewModelClassName.simpleNames.size != 1) {
            logWarn("@GenerateArgs is only applicable to top-level classes.", classElement)
            return null
        }
        val argsClassName = viewModelClassName.peerClass("${viewModelClassName.simpleName}Args")

        val arguments = classElement.enclosedElements.mapNotNull { enclosedElement ->
            if (enclosedElement.kind != ElementKind.METHOD ||
                Modifier.STATIC !in enclosedElement.modifiers ||
                enclosedElement !is ExecutableElement ||
                enclosedElement.parameters.isNotEmpty()
            ) return@mapNotNull null

            val propertyVisitor = properties.firstOrNull {
                val methodName = it.syntheticMethodName
                methodName != null && enclosedElement.simpleName.contentEquals(methodName)
            } ?: return@mapNotNull null

            val annotation = enclosedElement.annotationMirrors.firstOrNull { annotation ->
                annotation.annotationType.asElement().let {
                    it is TypeElement &&
                        (it.qualifiedName.contentEquals(ARGUMENT_ANNOTATION_NAME) ||
                            it.qualifiedName.contentEquals(DEPRECATED_ARGUMENT_ANNOTATION_NAME))
                }
            } ?: return@mapNotNull null

            propertyVisitor.createArgumentInfo(annotation, classElement)
        }

        return ViewModelInfo(viewModelClassName, argsClassName, arguments)
    }

    private fun PropertyVisitor.createArgumentInfo(
        annotation: AnnotationMirror,
        classElement: TypeElement
    ): ViewModelArgumentInfo? {
        val isOptional = annotation.getArgumentValue(IS_OPTIONAL_PARAM_NAME) as? Boolean ?: false

        val propertyType = returnType?.toTypeName()
        if (propertyType == null) {
            logWarn("Cannot resolve the type of property `$propertyName`.", classElement)
            return null
        }
        val argumentType = propertyType.extractTypeArgumentIfLiveData()

        val putMethodName = resolvePutMethodName(argumentType, isOptional)
        if (putMethodName == null) {
            logWarn("Type `$argumentType` cannot be put into a Bundle.", classElement)
        }

        return ViewModelArgumentInfo(propertyName, argumentType, isOptional, putMethodName)
    }

    private fun AnnotationMirror.getArgumentValue(name: String): Any? =
        elementValues.asIterable()
            .firstOrNull { it.key.simpleName.contentEquals(name) }?.value?.value

    private fun TypeName.extractTypeArgumentIfLiveData(): TypeName =
        if (this is ParameterizedTypeName &&
            typeArguments.size == 1 &&
            rawType.canonicalName in liveDataClasses
        ) {
            val typeArgument = typeArguments[0]
            when {
                typeArgument !is WildcardTypeName -> typeArgument
                typeArgument.inTypes.size == 1 -> typeArgument.inTypes[0]
                else -> typeArgument.outTypes[0]
            }
        } else this

    private fun resolvePutMethodName(typeName: TypeName, isOptional: Boolean): String? {
        fun isAssignableTo(qualifiedName: String): Boolean {
            val superType = qualifiedName.toTypeMirror() ?: return false
            val type = typeName.toTypeMirror() ?: return false
            return type.isAssignableTo(superType)
        }

        fun hasTypeArgumentAssignableTo(qualifiedName: String): Boolean {
            val superType = qualifiedName.toTypeMirror() ?: return false
            val argumentType = (typeName as? ParameterizedTypeName)
                ?.typeArguments?.firstOrNull()?.toTypeMirror() ?: return false
            return argumentType.isAssignableTo(superType)
        }

        return BundleMethodResolver.resolvePutMethodName(
            typeName,
            isOptional,
            ::isAssignableTo,
            ::hasTypeArgumentAssignableTo
        )
    }

    private fun TypeName.toTypeMirror(): TypeMirror? =
        rawCanonicalName?.toTypeMirror()

    private fun String.toTypeMirror(): TypeMirror? =
        processingEnv.elementUtils.getTypeElement(this)?.asType()

    private fun TypeMirror.isAssignableTo(superType: TypeMirror): Boolean =
        processingEnv.typeUtils.isAssignable(this, superType)

    private class ClassVisitor : KmClassVisitor() {

        var className: ClassName? = null

        val properties: MutableList<PropertyVisitor> = mutableListOf()

        override fun visit(flags: Flags, name: KmClassName) {
            className = name.toClassName()
        }

        override fun visitProperty(
            flags: Flags,
            name: String,
            getterFlags: Flags,
            setterFlags: Flags
        ): KmPropertyVisitor = PropertyVisitor(name).also { properties.add(it) }
    }

    private class PropertyVisitor(val propertyName: String) : KmPropertyVisitor() {

        var returnType: TypeVisitor? = null

        var syntheticMethodName: String? = null

        override fun visitReturnType(flags: Flags): KmTypeVisitor =
            TypeVisitor(flags, null).also { returnType = it }

        override fun visitExtensions(type: KmExtensionType): KmPropertyExtensionVisitor? {
            if (type != JvmPropertyExtensionVisitor.TYPE) return null
            return object : JvmPropertyExtensionVisitor() {
                override fun visitSyntheticMethodForAnnotations(signature: JvmMethodSignature?) {
                    // Usually, the synthetic method is static and has no argument.
                    // But, for extension properties, it has a receiver argument.
                    // This class handles non-extension properties only.
                    if (signature?.desc == "()V") syntheticMethodName = signature.name
                }
            }
        }
    }

    private open class TypeVisitor(
        private val flags: Flags,
        private val variance: KmVariance?
    ) : KmTypeVisitor() {

        private var className: ClassName? = null

        private val arguments: MutableList<TypeVisitor> = mutableListOf()

        private var outerType: TypeVisitor? = null

        override fun visitClass(name: KmClassName) {
            className = name.toClassName()
        }

        override fun visitArgument(flags: Flags, variance: KmVariance): KmTypeVisitor =
            TypeVisitor(flags, variance).also { arguments.add(it) }

        override fun visitStarProjection() {
            arguments.add(Star)
        }

        override fun visitOuterType(flags: Flags): KmTypeVisitor =
            TypeVisitor(flags, null).also { outerType = it }

        private val isNullable: Boolean
            get() = Flag.Type.IS_NULLABLE(flags)

        open fun toTypeName(): TypeName? {
            val rawType = className ?: return null
            val typeArguments = arguments.map { it.toTypeName() ?: return null }
            val outerTypeName = outerType?.toTypeName() as? ParameterizedTypeName
            val nonNullType = when {
                outerTypeName != null ->
                    outerTypeName.nestedClass(rawType.simpleName, typeArguments)
                typeArguments.isEmpty() -> rawType
                else -> rawType.parameterizedBy(typeArguments)
            }
            val typeName = if (isNullable) nonNullType.copy(nullable = true) else nonNullType
            return when (variance) {
                KmVariance.IN -> WildcardTypeName.consumerOf(typeName)
                KmVariance.OUT -> WildcardTypeName.producerOf(typeName)
                else -> typeName
            }
        }

        private object Star : TypeVisitor(flagsOf(), null) {
            override fun toTypeName(): TypeName = STAR
        }
    }

    private fun logWarn(message: String, element: Element) {
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, message, element)
    }

    private companion object {

        private fun KmClassName.toClassName(): ClassName? {
            if (isLocal) return null
            val packageName = substringBeforeLast('/', "").replace('/', '.')
            val simpleNames = substringAfterLast('/').split('.')
            return ClassName(packageName, simpleNames)
        }

        private fun Metadata.asClassHeader(): KotlinClassHeader = KotlinClassHeader(
            kind = kind,
            metadataVersion = metadataVersion,
            bytecodeVersion = bytecodeVersion,
            data1 = data1,
            data2 = data2,
            extraString = extraString,
            packageName = packageName,
            extraInt = extraInt
        )
    }
}

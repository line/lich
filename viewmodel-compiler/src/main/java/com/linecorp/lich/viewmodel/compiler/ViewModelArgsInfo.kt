/*
 * Copyright 2019 LINE Corporation
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
package com.linecorp.lich.viewmodel.compiler

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import kotlinx.metadata.ClassName
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
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

private typealias KpClassName = com.squareup.kotlinpoet.ClassName

internal class ViewModelArgsInfo private constructor(
    private val className: KpClassName,
    private val originatingClassName: KpClassName,
    private val originatingElement: TypeElement,
    private val arguments: List<ArgumentInfo>,
    val errorMessages: List<String>
) {
    fun toFileSpec(): FileSpec {
        val constructorSpec = FunSpec.constructorBuilder().apply {
            arguments.forEach { addParameter(it.toConstructorParameterSpec()) }
        }.build()

        val propertySpecs = arguments.map { it.toPropertySpec() }

        val toBundleFunSpec = FunSpec.builder("toBundle")
            .addModifiers(KModifier.OVERRIDE)
            .returns(bundleClass)
            .beginControlFlow("return %T().apply", bundleClass).also { builder ->
                arguments.forEach { it.addPutValueStatement(builder) }
            }.endControlFlow()
            .build()

        val typeSpec = TypeSpec.classBuilder(className)
            .addOriginatingElement(originatingElement)
            .addKdoc("A generated Args class for [%T].", originatingClassName)
            .addSuperinterface(viewModelArgsClass)
            .primaryConstructor(constructorSpec)
            .addProperties(propertySpecs)
            .addFunction(toBundleFunSpec)
            .build()

        return FileSpec.get(className.packageName, typeSpec)
    }

    companion object {
        fun create(
            viewModelClass: TypeElement,
            elements: Elements,
            types: Types
        ): ViewModelArgsInfo {
            val metadataAnnotation = viewModelClass.getAnnotation(Metadata::class.java)
                ?: throw IllegalStateException("Kotlin Metadata annotation is not found.")
            val metadata = KotlinClassMetadata.read(metadataAnnotation.asClassHeader())
            if (metadata !is KotlinClassMetadata.Class) {
                throw UnsupportedOperationException("Unsupported metadata type: $metadata")
            }

            val classVisitor = ClassVisitor(viewModelClass)
            metadata.accept(classVisitor)

            val resolvedArguments = classVisitor.resolvedArguments
            val errorMessages = listOf(
                classVisitor.failedProperties.map { propertyName ->
                    "Failed to resolve the type of a property: $propertyName"
                },
                resolvedArguments.filterNot { it.resolvePutMethod(elements, types) }.map { info ->
                    "Unsupported type: Cannot put \"${info.name}: " +
                        "${info.type.copy(nullable = false)}\" to Bundle."
                }
            ).flatten()

            val viewModelClassName = classVisitor.className?.toKpClassName()
                ?: throw IllegalStateException("Failed to obtain the name of the ViewModelClass.")
            val argsClassName = viewModelClassName.peerClass("${viewModelClassName.simpleName}Args")
            return ViewModelArgsInfo(
                argsClassName,
                viewModelClassName,
                viewModelClass,
                resolvedArguments,
                errorMessages
            )
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

        private val viewModelArgsClass =
            KpClassName("com.linecorp.lich.viewmodel", "ViewModelArgs")
        private val bundleClass = KpClassName("android.os", "Bundle")
    }
}

private class ArgumentInfo(val name: String, val type: TypeName, val isOptional: Boolean) {

    private var putMethodName: String? = null

    fun resolvePutMethod(elements: Elements, types: Types): Boolean {
        putMethodName = resolveMethodNameOrNull(elements, types)
        return putMethodName != null
    }

    fun toConstructorParameterSpec(): ParameterSpec =
        ParameterSpec.builder(name, type).apply {
            if (isOptional) defaultValue("%L", null)
        }.build()

    fun toPropertySpec(): PropertySpec =
        PropertySpec.builder(name, type)
            .initializer("%N", name)
            .build()

    fun addPutValueStatement(builder: FunSpec.Builder) {
        val methodName = putMethodName
        when {
            methodName == null -> {
                builder.addStatement("TODO(%S)", "Unsupported type: Cannot put $name to Bundle.")
            }
            isOptional -> {
                builder.addStatement("if (%N != null) %L(%S, %N)", name, methodName, name, name)
            }
            else -> {
                builder.addStatement("%L(%S, %N)", methodName, name, name)
            }
        }
    }

    private fun resolveMethodNameOrNull(elements: Elements, types: Types): String? {
        val rawTypeName = type.getRawCanonicalName() ?: return null

        // For nullable primitive types, we cannot use put-methods such as
        // "putInt(key: String, value: Int)". So, we use "putSerializable" instead.
        if (!isOptional && type.isNullable && rawTypeName in primitiveTypes) {
            return "putSerializable"
        }

        rawTypeMethodMap[rawTypeName]?.let { return it }

        if (rawTypeName in parcelablesMethodMap &&
            type is ParameterizedTypeName &&
            type.typeArguments.size == 1
        ) {
            // Handles the following types:
            // "putParcelableArray(key: String, value: Array<Parcelable>)"
            // "putParcelableArrayList(key: String, value: ArrayList<Parcelable>)"
            // "putSparseParcelableArray(key: String, value: SparseArray<Parcelable>)"
            type.typeArguments[0].getRawCanonicalName()?.let { parameterTypeName ->
                elements.getTypeMirror(parameterTypeName)
            }?.let { parameterTypeMirror ->
                if (parameterTypeMirror.isSubtypeOf("android.os.Parcelable", elements, types))
                    return parcelablesMethodMap[rawTypeName]
            }

            // Any other "Array<T>" and "ArrayList<T>" can be put using "putSerializable".
            if (rawTypeName != "android.util.SparseArray") return "putSerializable"
        }

        elements.getTypeMirror(rawTypeName)?.let { rawTypeMirror ->
            if (rawTypeMirror.isSubtypeOf("java.lang.CharSequence", elements, types))
                return "putCharSequence"

            if (rawTypeMirror.isSubtypeOf("android.os.IBinder", elements, types))
                return "putBinder"

            if (rawTypeMirror.isSubtypeOf("android.os.Parcelable", elements, types))
                return "putParcelable"

            if (rawTypeMirror.isSubtypeOf("java.io.Serializable", elements, types))
                return "putSerializable"
        }

        return null
    }

    private fun TypeName.getRawCanonicalName(): String? =
        when (val erasure = eraseWildcard()) {
            is KpClassName -> erasure.canonicalName
            is ParameterizedTypeName -> erasure.rawType.canonicalName
            else -> null
        }

    private fun Elements.getTypeMirror(canonicalName: String): TypeMirror? =
        getTypeElement(canonicalName)?.asType()

    private fun TypeMirror.isSubtypeOf(
        superTypeName: String,
        elements: Elements,
        types: Types
    ): Boolean = elements.getTypeMirror(superTypeName)?.let { superType ->
        types.isSubtype(this, superType)
    } ?: false

    companion object {
        private val primitiveTypes: Set<String> = setOf(
            "kotlin.Byte",
            "kotlin.Short",
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Float",
            "kotlin.Double",
            "kotlin.Char",
            "kotlin.Boolean"
        )

        private val rawTypeMethodMap: Map<String, String> = mapOf(
            "kotlin.Byte" to "putByte",
            "kotlin.Short" to "putShort",
            "kotlin.Int" to "putInt",
            "kotlin.Long" to "putLong",
            "kotlin.Float" to "putFloat",
            "kotlin.Double" to "putDouble",
            "kotlin.Char" to "putChar",
            "kotlin.Boolean" to "putBoolean",
            "kotlin.ByteArray" to "putByteArray",
            "kotlin.ShortArray" to "putShortArray",
            "kotlin.IntArray" to "putIntArray",
            "kotlin.LongArray" to "putLongArray",
            "kotlin.FloatArray" to "putFloatArray",
            "kotlin.DoubleArray" to "putDoubleArray",
            "kotlin.CharArray" to "putCharArray",
            "kotlin.BooleanArray" to "putBooleanArray",
            "kotlin.String" to "putString",
            "kotlin.CharSequence" to "putCharSequence",
            "android.os.Bundle" to "putBundle",
            "android.util.Size" to "putSize",
            "android.util.SizeF" to "putSizeF"
        )

        private val parcelablesMethodMap: Map<String, String> = mapOf(
            "kotlin.Array" to "putParcelableArray",
            "java.util.ArrayList" to "putParcelableArrayList",
            "android.util.SparseArray" to "putSparseParcelableArray"
        )
    }
}

private class ClassVisitor(val classElement: TypeElement) : KmClassVisitor() {

    var className: ClassName? = null

    private val allProperties: MutableList<PropertyVisitor> = mutableListOf()

    val resolvedArguments: MutableList<ArgumentInfo> = mutableListOf()

    val failedProperties: MutableList<String> = mutableListOf()

    override fun visit(flags: Flags, name: ClassName) {
        className = name
    }

    override fun visitProperty(
        flags: Flags,
        name: String,
        getterFlags: Flags,
        setterFlags: Flags
    ): KmPropertyVisitor? = PropertyVisitor(name).also { allProperties.add(it) }

    override fun visitEnd() {
        classElement.enclosedElements.forEach { element ->
            if (element.mayBeSyntheticMethodForAnnotations()) {
                val elementName = element.simpleName.toString()
                loop@ for (property in allProperties) {
                    when (val result = property.resolveArgument(elementName, element)) {
                        ResolveArgumentResult.NotMatch -> continue@loop
                        is ResolveArgumentResult.Resolved ->
                            resolvedArguments.add(result.argumentInfo)
                        is ResolveArgumentResult.FailedToResolveType ->
                            failedProperties.add(result.propertyName)
                    }
                    break@loop
                }
            }
        }
    }

    private fun Element.mayBeSyntheticMethodForAnnotations(): Boolean =
        kind == ElementKind.METHOD && Modifier.STATIC in modifiers &&
            this is ExecutableElement && parameters.isEmpty()
}

private sealed class ResolveArgumentResult {
    object NotMatch : ResolveArgumentResult()
    object NotAnnotated : ResolveArgumentResult()
    class Resolved(val argumentInfo: ArgumentInfo) : ResolveArgumentResult()
    class FailedToResolveType(val propertyName: String) : ResolveArgumentResult()
}

private class PropertyVisitor(val propertyName: String) : KmPropertyVisitor() {

    private var returnType: TypeVisitor? = null

    private var syntheticMethodName: String? = null

    override fun visitReturnType(flags: Flags): KmTypeVisitor? =
        TypeVisitor(flags, null).also { returnType = it }

    override fun visitExtensions(type: KmExtensionType): KmPropertyExtensionVisitor? =
        if (type == propertyExtensionVisitor.type) propertyExtensionVisitor else null

    private val propertyExtensionVisitor = object : JvmPropertyExtensionVisitor() {
        override fun visitSyntheticMethodForAnnotations(signature: JvmMethodSignature?) {
            // Usually, the synthetic method is static and has no argument.
            // But, for extension properties, it has a receiver argument.
            // This annotation processor only handles non-extension properties.
            if (signature?.desc == "()V") syntheticMethodName = signature.name
        }
    }

    fun resolveArgument(methodName: String, methodElement: Element): ResolveArgumentResult {
        if (methodName != syntheticMethodName) return ResolveArgumentResult.NotMatch

        val argumentAnnotation = methodElement.getArgumentAnnotationOrNull()
            ?: return ResolveArgumentResult.NotAnnotated
        val isOptional = argumentAnnotation.isOptional()

        val propertyType = returnType?.toTypeName()
            ?: return ResolveArgumentResult.FailedToResolveType(propertyName)

        val argumentType = propertyType.extractTypeArgumentIfLiveData()

        val argumentInfo = ArgumentInfo(
            propertyName,
            if (isOptional) argumentType.copy(nullable = true) else argumentType,
            isOptional
        )
        return ResolveArgumentResult.Resolved(argumentInfo)
    }

    private fun Element.getArgumentAnnotationOrNull(): AnnotationMirror? =
        annotationMirrors.firstOrNull { it.isArgument() }

    private fun AnnotationMirror.isArgument(): Boolean =
        (annotationType.asElement() as? TypeElement)?.qualifiedName
            ?.contentEquals("com.linecorp.lich.viewmodel.Argument") ?: false

    private fun AnnotationMirror.isOptional(): Boolean =
        elementValues.asSequence()
            .filter { it.key.simpleName.contentEquals("isOptional") }
            .map { it.value.value == true }
            .firstOrNull() ?: false

    private fun TypeName.extractTypeArgumentIfLiveData(): TypeName =
        if (this is ParameterizedTypeName &&
            typeArguments.size == 1 &&
            rawType.canonicalName in liveDataClasses
        ) typeArguments[0].eraseWildcard() else this

    companion object {
        private val liveDataClasses: Set<String> = setOf(
            "androidx.lifecycle.LiveData",
            "androidx.lifecycle.MutableLiveData"
        )
    }
}

private open class TypeVisitor(val flags: Flags, val variance: KmVariance?) : KmTypeVisitor() {

    private var className: ClassName? = null

    private val arguments: MutableList<TypeVisitor> = mutableListOf()

    private var outerType: TypeVisitor? = null

    override fun visitClass(name: ClassName) {
        className = name
    }

    override fun visitArgument(flags: Flags, variance: KmVariance): KmTypeVisitor? =
        TypeVisitor(flags, variance).also { arguments.add(it) }

    override fun visitStarProjection() {
        arguments.add(Star)
    }

    override fun visitOuterType(flags: Flags): KmTypeVisitor? =
        TypeVisitor(flags, null).also { outerType = it }

    private val isNullable: Boolean
        get() = Flag.Type.IS_NULLABLE(flags)

    open fun toTypeName(): TypeName? {
        val rawType = className?.toKpClassName() ?: return null
        val typeArguments = arguments.map { it.toTypeName() ?: return null }
        val outerTypeName = outerType?.toTypeName() as? ParameterizedTypeName

        val nonNullType = when {
            outerTypeName != null -> outerTypeName.nestedClass(rawType.simpleName, typeArguments)
            arguments.isEmpty() -> rawType
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
        override fun toTypeName(): TypeName? = STAR
    }
}

private fun ClassName.toKpClassName(): KpClassName? {
    if (isLocal) return null
    val packageName = substringBeforeLast('/', "").replace('/', '.')
    val simpleNames = substringAfterLast('/').split('.')
    return KpClassName(packageName, simpleNames)
}

private fun TypeName.eraseWildcard(): TypeName =
    if (this is WildcardTypeName) outTypes[0] else this

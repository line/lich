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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import javax.annotation.processing.Filer
import javax.lang.model.element.Element

internal object ViewModelArgsWriter {

    fun writeToFiler(viewModelInfo: ViewModelInfo, originatingElement: Element, filer: Filer) {
        viewModelInfo.toFileSpec(originatingElement).writeTo(filer)
    }

    fun writeToAppendable(viewModelInfo: ViewModelInfo, appendable: Appendable) {
        viewModelInfo.toFileSpec().writeTo(appendable)
    }

    private fun ViewModelInfo.toFileSpec(originatingElement: Element? = null): FileSpec {
        val constructorSpec = FunSpec.constructorBuilder().apply {
            arguments.forEach { addParameter(it.toConstructorParameterSpec()) }
        }.build()

        val propertySpecs = arguments.map { it.toPropertySpec() }

        val toBundleFunSpec = FunSpec.builder("toBundle")
            .addModifiers(KModifier.OVERRIDE)
            .returns(bundleClass)
            .beginControlFlow("return %T().also", bundleClass).also { builder ->
                arguments.forEach { it.addStatementTo(builder) }
            }.endControlFlow()
            .build()

        val typeSpecBuilder = TypeSpec.classBuilder(argsClassName)
            .addKdoc("A generated Args class for [%T].", viewModelClassName)
            .addSuperinterface(viewModelArgsClass)
            .primaryConstructor(constructorSpec)
            .addProperties(propertySpecs)
            .addFunction(toBundleFunSpec)
        if (originatingElement != null) {
            typeSpecBuilder.addOriginatingElement(originatingElement)
        }

        return FileSpec.get(argsClassName.packageName, typeSpecBuilder.build())
    }

    private fun ViewModelArgumentInfo.toConstructorParameterSpec(): ParameterSpec =
        ParameterSpec.builder(name, parameterType).apply {
            if (isOptional) defaultValue("%L", null)
        }.build()

    private fun ViewModelArgumentInfo.toPropertySpec(): PropertySpec =
        PropertySpec.builder(name, parameterType)
            .initializer("%N", name)
            .build()

    private fun ViewModelArgumentInfo.addStatementTo(builder: FunSpec.Builder) {
        when {
            putMethodName == null ->
                builder.addStatement("TODO(%S)", "Cannot put `$name` to Bundle directly.")
            isOptional ->
                builder.addStatement(
                    "if (this.%N != null) it.%N(%S, this.%N)",
                    name,
                    putMethodName,
                    name,
                    name
                )
            else ->
                builder.addStatement("it.%N(%S, this.%N)", putMethodName, name, name)
        }
    }

    private val viewModelArgsClass = ClassName("com.linecorp.lich.savedstate", "ViewModelArgs")
    private val bundleClass = ClassName("android.os", "Bundle")
}

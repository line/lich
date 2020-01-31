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

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.NestingKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class ViewModelArgsProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes(): Set<String> =
        setOf("com.linecorp.lich.viewmodel.GenerateArgs")

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (annotation in annotations) {
            for (element in roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element !is TypeElement || element.nestingKind != NestingKind.TOP_LEVEL) {
                    processingEnv.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "@GenerateArgs supports top-level classes only.",
                        element
                    )
                    continue
                }

                val viewModelArgsInfo = try {
                    ViewModelArgsInfo.create(
                        element,
                        processingEnv.elementUtils,
                        processingEnv.typeUtils
                    )
                } catch (e: Exception) {
                    processingEnv.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to parse the metadata: $e",
                        element
                    )
                    continue
                }

                try {
                    viewModelArgsInfo.toFileSpec().writeTo(processingEnv.filer)
                } catch (e: Exception) {
                    processingEnv.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Failed to generate the Args file: $e"
                    )
                }
                viewModelArgsInfo.errorMessages.forEach { errorMessage ->
                    processingEnv.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        errorMessage,
                        element
                    )
                }
            }
        }
        return true
    }
}

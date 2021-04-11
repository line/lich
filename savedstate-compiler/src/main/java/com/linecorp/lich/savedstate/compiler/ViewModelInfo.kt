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
import com.squareup.kotlinpoet.TypeName

internal class ViewModelInfo(
    val viewModelClassName: ClassName,
    val argsClassName: ClassName,
    val arguments: List<ViewModelArgumentInfo>
)

internal class ViewModelArgumentInfo(
    val name: String,
    type: TypeName,
    val isOptional: Boolean,
    val putMethodName: String?
) {
    val parameterType: TypeName = if (isOptional) type.copy(nullable = true) else type
}

internal const val GENERATE_ARGS_ANNOTATION_NAME = "com.linecorp.lich.savedstate.GenerateArgs"
internal const val ARGUMENT_ANNOTATION_NAME = "com.linecorp.lich.savedstate.Argument"
internal const val IS_OPTIONAL_PARAM_NAME = "isOptional"

internal val liveDataClasses: List<String> = listOf(
    "androidx.lifecycle.LiveData",
    "androidx.lifecycle.MutableLiveData"
)

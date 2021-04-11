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
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.WildcardTypeName

internal object BundleMethodResolver {

    fun resolvePutMethodName(
        typeName: TypeName,
        isOptional: Boolean,
        isAssignableTo: (String) -> Boolean,
        hasTypeArgumentAssignableTo: (String) -> Boolean
    ): String? {
        val rawTypeName = typeName.rawCanonicalName ?: return null

        // For nullable primitive types, we cannot use put-methods such as
        // "putInt(key: String, value: Int)". So, we use "putSerializable" instead.
        if (!isOptional && typeName.isNullable && rawTypeName in primitiveTypes)
            return "putSerializable"

        rawTypeMethodMap[rawTypeName]?.let { return it }

        parcelablesMethodMap[rawTypeName]?.let { methodName ->
            if (hasTypeArgumentAssignableTo("android.os.Parcelable")) {
                return methodName
            } else if (rawTypeName != "android.util.SparseArray") {
                // Any other "Array<T>" and "ArrayList<T>" can be put using "putSerializable".
                return "putSerializable"
            }
        }

        assignableTypeMethodMap.forEach { (superType, methodName) ->
            if (isAssignableTo(superType)) return methodName
        }

        return null
    }

    val TypeName.rawCanonicalName: String?
        get() = when (val erasure = if (this is WildcardTypeName) outTypes[0] else this) {
            is ClassName -> erasure.canonicalName
            is ParameterizedTypeName -> erasure.rawType.canonicalName
            else -> null
        }

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
        "kotlin.collections.ArrayList" to "putParcelableArrayList",
        "java.util.ArrayList" to "putParcelableArrayList",
        "android.util.SparseArray" to "putSparseParcelableArray"
    )

    private val assignableTypeMethodMap: Map<String, String> = mapOf(
        "kotlin.CharSequence" to "putCharSequence",
        "java.lang.CharSequence" to "putCharSequence",
        "android.os.IBinder" to "putBinder",
        "android.os.Parcelable" to "putParcelable",
        "java.io.Serializable" to "putSerializable"
    )
}

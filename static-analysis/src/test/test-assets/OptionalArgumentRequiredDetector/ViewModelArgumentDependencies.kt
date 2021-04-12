@file:JvmName("SavedStatesKt")

package com.linecorp.lich.savedstate

/**
 * Defines the basic structure of the classes needed to run the tests.
 */
annotation class Argument(val isOptional: Boolean = false)

class SavedStateHandle

fun <T> SavedStateHandle.required() = Unit

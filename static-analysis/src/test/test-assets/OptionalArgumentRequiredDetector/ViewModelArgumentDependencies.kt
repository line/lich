package com.linecorp.lich.viewmodel

/**
 * Defines the basic structure of the classes needed to run the tests.
 */
annotation class Argument(val isOptional: Boolean = false)

class SavedState() {
    fun <T> required() = Unit
}

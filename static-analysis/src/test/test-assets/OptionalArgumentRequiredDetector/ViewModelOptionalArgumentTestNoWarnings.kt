package com.linecorp.lich.savedstate

class ArgumentTest(val savedStateHandle: SavedStateHandle) {
    @Argument
    val test: Int by savedStateHandle.required()
    @Argument(isOptional = false)
    val test: Int by savedStateHandle.required()
}

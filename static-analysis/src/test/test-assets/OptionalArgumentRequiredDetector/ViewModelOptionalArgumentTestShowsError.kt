package com.linecorp.lich.savedstate

class ArgumentTest(val savedStateHandle: SavedStateHandle) {
    @Argument(isOptional = true)
    val test: Int by savedStateHandle.required()
}

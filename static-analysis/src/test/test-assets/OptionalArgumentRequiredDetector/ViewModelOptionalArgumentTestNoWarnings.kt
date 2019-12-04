package com.linecorp.lich.viewmodel

class ArgumentTest(val savedState: SavedState) {
    @Argument
    val test: Int by savedState.required()
    @Argument(isOptional = false)
    val test: Int by savedState.required()
}

package com.linecorp.lich.viewmodel

class ArgumentTest(val savedState: SavedState) {
    @Argument(isOptional = true)
    val test: Int by savedState.required()
}

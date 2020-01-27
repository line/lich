package com.linecorp.lich.viewmodel

class TestFoo : AbstractViewModel() {
    // This should be correct (lint shoudn't report this line)
    companion object : ViewModelFactory<TestFoo>()
}

class TestBar : AbstractViewModel() {
    // This should be wrong because TestBar's factory is creating TestFoo objects.
    companion object : ViewModelFactory<TestFoo>()
}

// This should show a warning because a class is implementing a factory instead of an object
// declaration.
class ClassFactory : ViewModelFactory<ClassFactory>()

// This is correct (lint shouldn't report this line) because an object declaration is
// implementing the factory.
object ObjecFactory : ViewModelFactory<ClassFactory>()

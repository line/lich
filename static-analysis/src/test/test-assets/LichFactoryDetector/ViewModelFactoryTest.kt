package com.linecorp.lich.viewmodel

class FooViewModel : AbstractViewModel()

// This should show an error because a class is implementing a factory instead of an object
// declaration.
class ClassFactory : ViewModelFactory<FooViewModel>()

// This is correct (lint shouldn't report this line) because an object declaration is
// implementing the factory.
object ObjectFactory : ViewModelFactory<FooViewModel>()

// This should show an error because the factory is implemented using an *object expression*.
val expressionFactory = object : ViewModelFactory<FooViewModel>() {}

class TestFoo : AbstractViewModel() {
    // This should be correct (lint shoudn't report this line)
    companion object : ViewModelFactory<TestFoo>()
}

class TestBar : AbstractViewModel() {
    // This should be wrong because TestBar's factory is creating TestFoo objects.
    companion object : ViewModelFactory<TestFoo>()
}

class TestBaz : AbstractViewModel() {
    // This should be correct because TestFooFactory is not a companion object.
    object TestFooFactory : ViewModelFactory<TestFoo>()
}

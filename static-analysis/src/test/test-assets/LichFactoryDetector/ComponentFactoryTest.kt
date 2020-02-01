package com.linecorp.lich.component

interface Api

// This should show an error because a class is implementing a factory instead of an object
// declaration.
class ClassFactory : ComponentFactory<Api>()

// This is correct (lint shouldn't report this line) because an object declaration is
// implementing the factory.
object ObjectFactory : ComponentFactory<Api>()

// This should show an error because the factory is implemented using an *object expression*.
val expressionFactory = object : ComponentFactory<Api>() {}

class TestFoo {
    // This should be correct (lint shoudn't report this line)
    companion object : ComponentFactory<TestFoo>()
}

class TestBar {
    // This should be wrong because TestBar's factory is creating TestFoo objects.
    companion object : ComponentFactory<TestFoo>()
}

class TestBaz {
    // This should be correct because TestFooFactory is not a companion object.
    object TestFooFactory : ComponentFactory<TestFoo>()
}

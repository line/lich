package com.linecorp.lich.component

// This should show a warning because a class is implementing a factory instead of an object
// declaration.
class ClassFactory : ComponentFactory<ClassFactory>()

// This is correct (lint shouldn't report this line) because an object declaration is
// implementing the factory.
object ObjectFactory : ComponentFactory<ClassFactory>()

class TestFoo : ComponentFactory() {
    // This should be correct (lint shoudn't report this line)
    companion object : ComponentFactory<TestFoo>()
}

class TestBar : ComponentFactory() {
    // This should be wrong because TestBar's factory is creating TestFoo objects.
    companion object : ComponentFactory<TestFoo>()
}

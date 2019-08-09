/*
 * Copyright 2019 LINE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linecorp.lich.component

import android.content.Context

/**
 * A class to declare a factory of a "component".
 *
 * Inherit this class for the companion object of a component class.
 * ```
 * class FooComponent {
 *
 *     // snip...
 *
 *     companion object : ComponentFactory<FooComponent>() {
 *         override fun createComponent(context: Context): FooComponent =
 *             FooComponent()
 *     }
 * }
 * ```
 *
 * The singleton instance of each component can be obtained via [Context.getComponent].
 * Therefore, the singleton instance of `FooComponent` can be obtained by this code:
 *
 * ```
 * val fooComponent = context.getComponent(FooComponent)
 * ```
 *
 * You can also obtain components *lazily* using [Context.component] like this:
 *
 * ```
 * val fooComponent by context.component(FooComponent)
 * ```
 *
 * @param T the type of the component that will be created by this factory.
 * @see Context.getComponent
 * @see Context.component
 */
abstract class ComponentFactory<T : Any> {
    /**
     * Creates a component for this factory.
     *
     * @param context the application context.
     */
    protected abstract fun createComponent(context: Context): T

    /**
     * Delegates the creation of a component to a [DelegatedComponentFactory] specified by name.
     * This function can be used to divide dependencies in a multi-module project.
     *
     * Assumes that there are two modules "base" and "foo" such that "foo" depends on "base".
     * If you want to call some function of "foo" from "base", implement like this:
     * ```
     * // "base" module
     * package module.base.facades
     *
     * interface FooModuleFacade {
     *
     *     fun launchFooActivity()
     *
     *     companion object : ComponentFactory<FooModuleFacade>() {
     *         override fun createComponent(context: Context): FooModuleFacade =
     *             delegateCreation(context, "module.foo.FooModuleFacadeFactory")
     *     }
     * }
     * ```
     *
     * ```
     * // "foo" module
     * package module.foo
     *
     * class FooModuleFacadeFactory: DelegatedComponentFactory<FooModuleFacade>() {
     *     override fun createComponent(context: Context): FooModuleFacade =
     *         FooModuleFacadeImpl(context)
     * }
     *
     * internal class FooModuleFacadeImpl(private val context: Context) : FooModuleFacade {
     *
     *     override fun launchFooActivity() {
     *         // snip...
     *     }
     * }
     * ```
     *
     * @param context the application context.
     * @param delegatedFactoryClassName the fully-qualified name of a class that inherits
     * [DelegatedComponentFactory].
     * @throws FactoryDelegationException If it failed to find or instantiate the class for
     * [delegatedFactoryClassName].
     * @see DelegatedComponentFactory
     */
    protected fun delegateCreation(context: Context, delegatedFactoryClassName: String): T {
        val delegatedFactoryClass = try {
            Class.forName(delegatedFactoryClassName, true, javaClass.classLoader)
        } catch (e: ClassNotFoundException) {
            throw FactoryDelegationException(e)
        }

        val delegatedFactory = try {
            @Suppress("UNCHECKED_CAST")
            delegatedFactoryClass.newInstance() as DelegatedComponentFactory<out T>
        } catch (e: IllegalAccessException) {
            throw FactoryDelegationException(e)
        } catch (e: InstantiationException) {
            throw FactoryDelegationException(e)
        } catch (e: ClassCastException) {
            throw FactoryDelegationException(e)
        }

        return delegatedFactory.create(context)
    }

    internal fun create(context: Context): T = createComponent(context)
}

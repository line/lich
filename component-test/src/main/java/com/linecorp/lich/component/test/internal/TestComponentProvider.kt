package com.linecorp.lich.component.test.internal

import android.content.Context
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.debug.internal.DebugComponentProvider
import com.linecorp.lich.component.internal.ComponentAccessor
import com.linecorp.lich.component.internal.ComponentProvider
import com.linecorp.lich.component.test.MockComponentManager
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * An implementation of [ComponentProvider] for tests.
 *
 * Every component created by this class is tied to its `applicationContext`. And, a different
 * instance of component is created for each `applicationContext`.
 */
class TestComponentProvider : DebugComponentProvider() {

    override val loadPriority: Int
        get() = 20

    private val componentManagers: WeakHashMap<Context, WeakReference<ComponentManager>> =
        WeakHashMap()

    /**
     * The last used [ComponentManager].
     * To prevent garbage collection, this instance is held by a strong reference.
     * Other past ComponentManagers are held in [componentManagers] by weak references.
     */
    private var latestComponentManager: ComponentManager? = null

    private fun getComponentManager(applicationContext: Context): ComponentManager =
        synchronized(componentManagers) {
            latestComponentManager?.takeIf { it.applicationContext == applicationContext }
                ?: componentManagers[applicationContext]?.get()?.also {
                    latestComponentManager = it
                }
                ?: ComponentManager(applicationContext).also {
                    latestComponentManager = it
                    componentManagers[applicationContext] = WeakReference(it)
                }
        }

    override fun <T : Any> getComponent(context: Context, factory: ComponentFactory<T>): T =
        getComponentManager(context.applicationContext).getComponent(factory)

    override fun getManager(applicationContext: Context): Any =
        getComponentManager(applicationContext)

    /**
     * This class manages all components and mocks related to the given [applicationContext].
     */
    private inner class ComponentManager(val applicationContext: Context) : MockComponentManager {

        private val componentContainer: ComponentContainer = ComponentContainer(defaultAccessor)

        private val mocks: ConcurrentHashMap<ComponentFactory<*>, Any> = ConcurrentHashMap()

        fun <T : Any> getComponent(factory: ComponentFactory<T>): T =
            getMockComponent(factory) ?: getRealComponent(factory)

        override fun <T : Any> setComponent(factory: ComponentFactory<T>, component: T) {
            componentContainer.setComponent(factory, component)
        }

        override fun <T : Any> clearComponent(factory: ComponentFactory<T>) {
            componentContainer.setComponent(factory, null)
        }

        override fun <T : Any> getComponentIfAlreadyCreated(factory: ComponentFactory<T>): T? =
            getComponentIfAlreadyCreated(componentContainer, factory)

        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> getMockComponent(factory: ComponentFactory<T>): T? =
            mocks[factory] as T?

        override fun <T : Any> setMockComponent(factory: ComponentFactory<T>, mock: T) {
            mocks[factory] = mock
        }

        override fun <T : Any> clearMockComponent(factory: ComponentFactory<T>) {
            mocks.remove(factory)
        }

        override fun <T : Any> getRealComponent(factory: ComponentFactory<T>): T =
            getOrCreateComponent(applicationContext, componentContainer, factory)

        override fun clearAllComponents() {
            componentContainer.clearAll()
            mocks.clear()
        }
    }

    private class ComponentContainer(private val accessor: ComponentAccessor) : ComponentAccessor {

        private val components: ConcurrentHashMap<ComponentFactory<*>, Any> = ConcurrentHashMap()

        override fun <T : Any> createComponent(
            factory: ComponentFactory<T>,
            applicationContext: Context
        ): T = accessor.createComponent(factory, applicationContext)

        override fun getComponent(factory: ComponentFactory<*>): Any? = components[factory]

        override fun setComponent(factory: ComponentFactory<*>, component: Any?) {
            when {
                component != null -> components[factory] = component
                else -> components.remove(factory)
            }
        }

        override fun compareAndSetComponent(
            factory: ComponentFactory<*>,
            expect: Any?,
            update: Any?
        ): Boolean = when {
            expect != null -> when {
                update != null -> components.replace(factory, expect, update)
                else -> components.remove(factory, expect)
            }
            else -> when {
                update != null -> components.putIfAbsent(factory, update) == null
                else -> components[factory] == null
            }
        }

        fun clearAll() {
            components.clear()
        }
    }
}

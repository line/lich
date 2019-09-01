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
package com.linecorp.lich.viewmodel.test.internal

import android.content.Context
import androidx.lifecycle.ViewModelStoreOwner
import com.linecorp.lich.viewmodel.AbstractViewModel
import com.linecorp.lich.viewmodel.ViewModelFactory
import com.linecorp.lich.viewmodel.internal.DefaultLichViewModelProvider
import com.linecorp.lich.viewmodel.test.MockViewModelManager
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

private typealias MockFactory = (ViewModelStoreOwner) -> AbstractViewModel

/**
 * An implementation of [com.linecorp.lich.viewmodel.internal.LichViewModelProvider] for tests.
 */
class TestLichViewModelProvider : DefaultLichViewModelProvider() {

    override val loadPriority: Int
        get() = 20

    private val viewModelManagers: WeakHashMap<Context, WeakReference<ViewModelManager>> =
        WeakHashMap()

    /**
     * The last used [ViewModelManager].
     * To prevent garbage collection, this instance is held by a strong reference.
     * Other past ViewModelManagers are held in [viewModelManagers] by weak references.
     */
    private var latestViewModelManager: ViewModelManager? = null

    private fun getViewModelManager(applicationContext: Context): ViewModelManager =
        synchronized(viewModelManagers) {
            latestViewModelManager?.takeIf { it.applicationContext == applicationContext }
                ?: viewModelManagers[applicationContext]?.get()?.also {
                    latestViewModelManager = it
                }
                ?: ViewModelManager(applicationContext).also {
                    latestViewModelManager = it
                    viewModelManagers[applicationContext] = WeakReference(it)
                }
        }

    override fun <T : AbstractViewModel> newViewModelFor(
        factory: ViewModelFactory<T>,
        applicationContext: Context,
        viewModelStoreOwner: ViewModelStoreOwner
    ): T = getViewModelManager(applicationContext)
        .newViewModelFor(factory, viewModelStoreOwner)

    override fun getManager(applicationContext: Context): Any =
        getViewModelManager(applicationContext)

    /**
     * This class manages all mock ViewModels related to the given [applicationContext].
     */
    private inner class ViewModelManager(val applicationContext: Context) : MockViewModelManager {

        private val mockFactories: ConcurrentHashMap<ViewModelFactory<*>, MockFactory> =
            ConcurrentHashMap()

        internal fun <T : AbstractViewModel> newViewModelFor(
            factory: ViewModelFactory<T>,
            viewModelStoreOwner: ViewModelStoreOwner
        ): T {
            // If there is a mockFactory, creates a mock and returns it.
            mockFactories[factory]?.let { mockFactory ->
                @Suppress("UNCHECKED_CAST")
                return mockFactory(viewModelStoreOwner) as T
            }
            // Otherwise, returns a real ViewModel.
            return createRealViewModel(factory)
        }

        override fun <T : AbstractViewModel> setMockViewModel(
            factory: ViewModelFactory<T>,
            mockFactory: (ViewModelStoreOwner) -> T
        ) {
            mockFactories[factory] = mockFactory
        }

        override fun <T : AbstractViewModel> clearMockViewModel(factory: ViewModelFactory<T>) {
            mockFactories.remove(factory)
        }

        override fun <T : AbstractViewModel> createRealViewModel(factory: ViewModelFactory<T>): T =
            createViewModel(factory, applicationContext)

        override fun clearAllMockViewModels() {
            mockFactories.clear()
        }
    }
}

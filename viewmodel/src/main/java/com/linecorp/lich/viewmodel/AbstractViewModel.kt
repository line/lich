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
package com.linecorp.lich.viewmodel

import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * The base class of ViewModels.
 *
 * This class can be used in almost the same way as Android Architecture Components' ViewModel
 * [androidx.lifecycle.ViewModel], but instances of this ViewModel should be created via
 * [ViewModelFactory]. For details, please refer the document of [ViewModelFactory].
 *
 * This class also implements [CoroutineScope]. This scope has [kotlinx.coroutines.SupervisorJob]
 * and [kotlinx.coroutines.Dispatchers.Main] context elements, and is cancelled just before
 * [onCleared] is called.
 *
 * @see ViewModelFactory
 */
abstract class AbstractViewModel : CoroutineScope by MainScope() {

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     */
    @MainThread
    protected open fun onCleared() {
    }

    internal fun clear() {
        // Since clear() is final, this line is still called on mock objects.
        // In those cases, coroutineContext may be null.
        @Suppress("UNNECESSARY_SAFE_CALL")
        coroutineContext?.cancel()

        onCleared()
    }
}

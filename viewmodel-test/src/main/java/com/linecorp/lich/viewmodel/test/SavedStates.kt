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
@file:JvmName("SavedStates")

package com.linecorp.lich.viewmodel.test

import com.linecorp.lich.savedstate.createSavedStateHandleForTesting
import com.linecorp.lich.viewmodel.SavedState
import com.linecorp.lich.viewmodel.ViewModelArgs

/**
 * Creates a [SavedState] with the specified contents, given as a list of pairs
 * where the first value is the key and the second is the value.
 */
@Deprecated(
    "Use `createSavedStateHandleForTesting` instead.",
    ReplaceWith(
        "createSavedStateHandleForTesting(*pairs)",
        "com.linecorp.lich.savedstate.createSavedStateHandleForTesting"
    )
)
fun createSavedStateForTesting(vararg pairs: Pair<String, *>): SavedState =
    SavedState(createSavedStateHandleForTesting(*pairs))

/**
 * Creates a [SavedState] initialized with the given [viewModelArgs].
 */
@Deprecated(
    "Use `createSavedStateHandleForTesting` instead.",
    ReplaceWith(
        "createSavedStateHandleForTesting(viewModelArgs)",
        "com.linecorp.lich.savedstate.createSavedStateHandleForTesting"
    )
)
fun createSavedStateForTesting(viewModelArgs: ViewModelArgs): SavedState =
    SavedState(createSavedStateHandleForTesting(viewModelArgs))

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

/**
 * An annotation to generate an *Args* class for the specified ViewModel class.
 *
 * This is a sample code:
 * ```
 * @GenerateArgs
 * class FooViewModel(savedState: SavedState) : AbstractViewModel() {
 *
 *     @Argument
 *     private val userName: String by savedState.required()
 *
 *     @Argument(isOptional = true)
 *     private val tags: Array<String> by savedState.initial(arrayOf("normal"))
 *
 *     @Argument
 *     private var attachment: Parcelable? by savedState
 *
 *     @Argument
 *     private val message: MutableLiveData<CharSequence> by savedState.liveData()
 *
 *     // snip...
 * }
 * ```
 *
 * The above code will generate an Args class like this:
 * ```
 * public class FooViewModelArgs(
 *     public val userName: String,
 *     public val tags: Array<String>? = null,
 *     public val attachment: Parcelable?,
 *     public val message: CharSequence
 * ) : ViewModelArgs {
 *     public override fun toBundle(): Bundle = Bundle().also {
 *         it.putString("userName", this.userName)
 *         if (this.tags != null) it.putSerializable("tags", this.tags)
 *         it.putParcelable("attachment", this.attachment)
 *         it.putCharSequence("message", this.message)
 *     }
 * }
 * ```
 *
 * @see Argument
 * @see ViewModelArgs
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateArgs

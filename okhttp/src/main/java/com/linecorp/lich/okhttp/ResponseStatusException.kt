/*
 * Copyright 2021 LINE Corporation
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
package com.linecorp.lich.okhttp

import java.io.IOException

/**
 * An exception indicating that an unsuccessful status code was returned in the HTTP response.
 *
 * @property code the HTTP status code.
 */
class ResponseStatusException(val code: Int, message: String) : IOException(message) {
    constructor(code: Int) : this(code, "HTTP status code: $code")
}

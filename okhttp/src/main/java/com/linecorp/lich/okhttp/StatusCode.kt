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

/**
 * Defines HTTP response status codes used by this library.
 */
object StatusCode {
    /**
     * HTTP `200 OK`.
     */
    const val OK: Int = 200

    /**
     * HTTP `206 Partial Content`.
     */
    const val PARTIAL_CONTENT: Int = 206

    /**
     * HTTP `416 Range Not Satisfiable`.
     */
    const val RANGE_NOT_SATISFIABLE: Int = 416
}

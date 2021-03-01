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
package com.linecorp.lich.thrift.logging

import org.apache.thrift.TBase

/**
 * A logger for [com.linecorp.lich.thrift.ThriftClientFactory].
 *
 * @see ThriftLogEnabler
 */
interface ThriftLogger {
    /**
     * Logs an invocation of `send_*` method of a `TServiceClient`.
     *
     * @param namespace Java package name of the Thrift service.
     * @param service The name of the Thrift service.
     * @param function The name of the function that has been invoked.
     * @param args The arguments of the invocation of [function].
     */
    fun logSend(namespace: String, service: String, function: String, args: TBase<*, *>)

    /**
     * Logs an invocation of `recv_*` method of a `TServiceClient`.
     *
     * @param namespace Java package name of the Thrift service.
     * @param service The name of the Thrift service.
     * @param function The name of the function that has been invoked.
     * @param result The return value of the invocation of [function].
     */
    fun logReceive(namespace: String, service: String, function: String, result: TBase<*, *>)
}

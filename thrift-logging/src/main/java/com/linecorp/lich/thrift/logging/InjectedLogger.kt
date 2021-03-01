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
 * A class that will be injected into each `TServiceClient` by [ThriftLoggerInjector].
 */
@Suppress("unused")
internal class InjectedLogger(
    private val namespace: String,
    private val service: String,
    private val logger: ThriftLogger
) {
    fun logSend(function: String, args: TBase<*, *>) {
        logger.logSend(namespace, service, function, args)
    }

    fun logReceive(result: TBase<*, *>, function: String) {
        logger.logReceive(namespace, service, function, result)
    }
}

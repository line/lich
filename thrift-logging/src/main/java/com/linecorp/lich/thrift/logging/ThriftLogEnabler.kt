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

import android.content.Context
import com.linecorp.lich.thrift.ThriftClientFactory
import org.apache.thrift.TServiceClient
import org.apache.thrift.transport.TTransport

/**
 * A class that enables logging for [ThriftClientFactory].
 *
 * @param context Android's application [Context].
 * @param logger The logger to be injected into [ThriftClientFactory].
 */
class ThriftLogEnabler(context: Context, logger: ThriftLogger) {

    private val injector = ThriftLoggerInjector(context, logger)

    /**
     * Returns a [ThriftClientFactory] that does transparent logging.
     *
     * @param factory A base [ThriftClientFactory].
     * @return A [ThriftClientFactory] with transparent logging enabled.
     */
    fun <T : TServiceClient> enableLogging(
        factory: ThriftClientFactory<T>
    ): ThriftClientFactory<T> =
        if (factory is LoggingThriftClientFactory) {
            factory
        } else {
            LoggingThriftClientFactory(factory, injector)
        }

    private class LoggingThriftClientFactory<T : TServiceClient>(
        private val delegate: ThriftClientFactory<T>,
        private val injector: ThriftLoggerInjector
    ) : ThriftClientFactory<T> {
        override fun newClient(transport: TTransport): T =
            injector.injectLogger(delegate.newClient(transport))
    }
}

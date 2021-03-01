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
package com.linecorp.lich.thrift

import com.linecorp.lich.sample.thrift.FooParam
import com.linecorp.lich.sample.thrift.FooResponse
import com.linecorp.lich.sample.thrift.FooService
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/**
 * The client class of [FooService] defined in `sample_thrift/src/main/thrift/FooService.thrift`.
 */
class FooServiceClient(
    override val okHttpClient: OkHttpClient,
    override val endpointUrl: HttpUrl
) : AbstractThriftServiceClient<FooService.Client>() {

    override val thriftClientFactory: ThriftClientFactory<FooService.Client> =
        ThriftClientFactory(FooService.Client.Factory())

    suspend fun ping() =
        call({ send_ping() }, { recv_ping() })

    suspend fun callFoo(id: Long, name: String, param: FooParam): FooResponse =
        call({ send_callFoo(id, name, param) }, { recv_callFoo() })
}

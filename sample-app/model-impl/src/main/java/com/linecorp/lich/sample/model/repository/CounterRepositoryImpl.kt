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
package com.linecorp.lich.sample.model.repository

import android.content.Context
import android.util.Log
import com.google.auto.service.AutoService
import com.linecorp.lich.component.ServiceLoaderComponent
import com.linecorp.lich.component.getComponent
import com.linecorp.lich.sample.model.db.CounterDao
import com.linecorp.lich.sample.model.db.CounterDatabase
import com.linecorp.lich.sample.model.entity.Counter
import com.linecorp.lich.sample.model.remote.CounterServiceClient
import java.io.IOException

/**
 * The implementation class of [CounterRepository].
 *
 * @see com.linecorp.lich.component.ComponentFactory.delegateToServiceLoader
 */
// This `@AutoService` annotation will automatically generate a file
// `META-INF/services/com.linecorp.lich.sample.model.repository.CounterRepository`.
@AutoService(CounterRepository::class)
// NOTE: Since this class is instantiated by ServiceLoader, it must have a public empty constructor.
class CounterRepositoryImpl : CounterRepository, ServiceLoaderComponent {

    private lateinit var counterServiceClient: CounterServiceClient

    private lateinit var counterDao: CounterDao

    override fun init(context: Context) {
        counterServiceClient = context.getComponent(CounterServiceClient)
        counterDao = context.getComponent(CounterDatabase).counterDao
    }

    override suspend fun getCounter(counterName: String): CounterResult {
        val dbCounter = counterDao.find(counterName)
        if (dbCounter != null) {
            Log.i("CounterRepository", "Loaded a counter from the local storage: $dbCounter")
            return CounterResult.Success(dbCounter)
        }

        val initialValue = try {
            counterServiceClient.getInitialCounterValue(counterName)
        } catch (e: IOException) {
            // Usually, IOExceptions should be handled by "Repository" layer.
            Log.e("CounterRepository", "Failed to query the initial value for $counterName", e)
            return CounterResult.NetworkError
        }

        val newCounter = Counter(counterName, initialValue)
        counterDao.replace(newCounter)
        Log.i("CounterRepository", "Created a new counter: $newCounter")

        return CounterResult.Success(newCounter)
    }

    override suspend fun storeCounter(counter: Counter) {
        counterDao.replace(counter)
        Log.i("CounterRepository", "Updated the counter: $counter")
    }

    override suspend fun deleteCounter(counter: Counter) {
        counterDao.delete(counter)
        Log.i("CounterRepository", "Deleted the counter: $counter")
    }
}

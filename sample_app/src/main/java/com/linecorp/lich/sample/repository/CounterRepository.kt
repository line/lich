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
package com.linecorp.lich.sample.repository

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.component.getComponent
import com.linecorp.lich.sample.db.CounterDao
import com.linecorp.lich.sample.db.CounterDatabase
import com.linecorp.lich.sample.entity.Counter
import com.linecorp.lich.sample.remote.CounterServiceClient
import java.io.IOException

class CounterRepository @VisibleForTesting internal constructor(
    private val counterServiceClient: CounterServiceClient,
    private val counterDao: CounterDao
) {
    /**
     * Returns the [Counter] for the given name.
     *
     * If the Counter is in the local storage, just returns it. Otherwise, fetches the initial value
     * of the Counter from the server, then stores it into the local storage.
     */
    suspend fun getCounter(counterName: String): CounterResult {
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

    /**
     * Stores the given counter into the local storage.
     */
    suspend fun storeCounter(counter: Counter) {
        counterDao.replace(counter)
        Log.i("CounterRepository", "Updated the counter: $counter")
    }

    /**
     * Deletes the given counter from the local storage.
     */
    suspend fun deleteCounter(counter: Counter) {
        counterDao.delete(counter)
        Log.i("CounterRepository", "Deleted the counter: $counter")
    }

    companion object : ComponentFactory<CounterRepository>() {
        override fun createComponent(context: Context): CounterRepository {
            val serviceClient = context.getComponent(CounterServiceClient)
            val database = context.getComponent(CounterDatabase)
            return CounterRepository(serviceClient, database.counterDao)
        }
    }
}

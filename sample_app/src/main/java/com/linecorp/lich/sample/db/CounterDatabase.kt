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
package com.linecorp.lich.sample.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.linecorp.lich.component.ComponentFactory
import com.linecorp.lich.sample.entity.Counter

@Database(entities = [Counter::class], version = 1)
abstract class CounterDatabase : RoomDatabase() {

    abstract val counterDao: CounterDao

    companion object : ComponentFactory<CounterDatabase>() {
        override fun createComponent(context: Context): CounterDatabase =
            Room.databaseBuilder(context, CounterDatabase::class.java, "counter").build()
    }
}

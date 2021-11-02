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
package com.linecorp.lich.sample.model.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.linecorp.lich.sample.model.entity.Counter

@Dao
interface CounterDao {
    @Query("SELECT * FROM counter WHERE name = :name")
    suspend fun find(name: String): Counter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(counter: Counter)

    @Delete
    suspend fun delete(counter: Counter)
}

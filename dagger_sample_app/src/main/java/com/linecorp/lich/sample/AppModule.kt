/*
 * Copyright 2020 LINE Corporation
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
package com.linecorp.lich.sample

import android.content.Context
import androidx.room.Room
import com.linecorp.lich.component.getComponent
import com.linecorp.lich.sample.db.CounterDao
import com.linecorp.lich.sample.db.CounterDatabase
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
object AppModule {

    @Singleton
    @Provides
    fun provideCounterDatabase(context: Context): CounterDatabase =
        Room.databaseBuilder(context, CounterDatabase::class.java, "counter").build()

    @Provides
    fun provideCounterDao(counterDatabase: CounterDatabase): CounterDao =
        counterDatabase.counterDao

    @Provides
    fun provideGlobalOkHttpClient(context: Context): OkHttpClient =
        context.getComponent(GlobalOkHttpClient)
}

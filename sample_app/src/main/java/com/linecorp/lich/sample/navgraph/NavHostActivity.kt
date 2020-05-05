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
package com.linecorp.lich.sample.navgraph

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.linecorp.lich.sample.R
import com.linecorp.lich.viewmodel.putViewModelArgs

class NavHostActivity : AppCompatActivity(R.layout.nav_host_activity) {

    private lateinit var navController: NavController

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()

    companion object {
        fun newIntent(context: Context, argument: String?): Intent =
            Intent(context, NavHostActivity::class.java).putViewModelArgs(
                NavHostViewModelArgs(argument)
            )

        fun launchDeepLink(context: Context, @IdRes destId: Int, argument: String?) {
            NavDeepLinkBuilder(context)
                .setComponentName(NavHostActivity::class.java)
                .setGraph(R.navigation.main_nav_graph)
                .setDestination(destId)
                .setArguments(NavDestinationViewModelArgs(argument).toBundle())
                .createTaskStackBuilder()
                .startActivities()
        }
    }
}

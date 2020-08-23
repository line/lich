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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.linecorp.lich.sample.R
import com.linecorp.lich.sample.databinding.NavGraphThirdFragmentBinding
import com.linecorp.lich.viewmodel.activityViewModel
import com.linecorp.lich.viewmodel.navGraphViewModel
import com.linecorp.lich.viewmodel.viewModel

class ThirdFragment : Fragment(R.layout.nav_graph_third_fragment) {

    private val navHostViewModel by activityViewModel(NavHostViewModel)

    private val navGraphViewModel
        by navGraphViewModel(NavDestinationViewModel, R.id.child_nav_graph)

    private val fragmentViewModel by viewModel(NavDestinationViewModel)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = NavGraphThirdFragmentBinding.bind(view)

        navHostViewModel.message.observe(viewLifecycleOwner) {
            binding.navHostViewmodelMessage.text = it
        }
        navGraphViewModel.message.observe(viewLifecycleOwner) {
            binding.navGraphViewmodelMessage.text = it
        }
        fragmentViewModel.message.observe(viewLifecycleOwner) {
            binding.fragmentViewmodelMessage.text = it
        }

        binding.goToSecondButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                R.id.action_to_second_fragment,
                NavDestinationViewModelArgs("From ${fragmentViewModel.instanceName}").toBundle()
            )
        )
        binding.upToFirstButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(
                R.id.action_up_to_first_fragment,
                NavDestinationViewModelArgs("From ${fragmentViewModel.instanceName}").toBundle()
            )
        )
    }
}

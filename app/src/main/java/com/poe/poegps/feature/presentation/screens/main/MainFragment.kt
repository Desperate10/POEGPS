package com.poe.poegps.feature.presentation.screens.main

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.poe.poegps.R
import com.poe.poegps.databinding.FragmentMainBinding
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.screens.main.adapter.ObjectsAdapter
import com.poe.poegps.feature.presentation.screens.main.spinner.ObjectsSpinnerAdapter
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.autoCleaned

@AndroidEntryPoint
class MainFragment : Fragment(), ObjectsAdapter.OnObjectClickListener, MenuProvider {

    private var binding: FragmentMainBinding by autoCleaned()
    private var adapter: ObjectsAdapter by autoCleaned()
    private val viewModel by viewModels<MainViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObjectSpinner()
    }

    private fun setupObjectSpinner() {
        binding.objectSpinner.adapter = ObjectsSpinnerAdapter(requireContext())
    }

    override fun onClick(obj: ObjectDisplayable) {
        TODO("Not yet implemented")
    }

    override fun onLongClick(obj: ObjectDisplayable) {
        val popupMenu = PopupMenu(requireContext(), binding.objectsRv)
        popupMenu.inflate(R.menu.menu_context_main)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.deleteOtp -> {
                    true
                }
                R.id.sendObject -> {
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_main, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.search -> {
                true
            }
            R.id.download -> {
                true
            }
            else -> false
        }
    }


}
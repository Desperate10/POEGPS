package com.poe.poegps.feature.presentation.screens.editor

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import com.poe.poegps.R
import com.poe.poegps.databinding.FragmentEditorBinding
import com.poe.poegps.feature.presentation.screens.editor.adapter.OprAdapter
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.autoCleaned

@AndroidEntryPoint
class EditorFragment : Fragment(), MenuProvider {

    private var binding: FragmentEditorBinding by autoCleaned()
    private var adapter: OprAdapter by autoCleaned()
    private val viewModel by viewModels<EditorViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(this)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_editor, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId) {
            R.id.add_savedopr -> {
                Log.d("testim", "add_savedopr")
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().removeMenuProvider(this)
    }

}
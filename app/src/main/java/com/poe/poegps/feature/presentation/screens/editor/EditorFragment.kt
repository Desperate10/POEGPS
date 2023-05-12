package com.poe.poegps.feature.presentation.screens.editor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.poe.poegps.R
import com.poe.poegps.databinding.FragmentEditorBinding
import com.poe.poegps.feature.presentation.screens.editor.adapter.OprAdapter
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.autoCleaned

@AndroidEntryPoint
class EditorFragment : Fragment() {

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

}
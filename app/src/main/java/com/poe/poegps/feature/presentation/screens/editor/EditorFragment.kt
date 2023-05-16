package com.poe.poegps.feature.presentation.screens.editor

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.poe.poegps.R
import com.poe.poegps.databinding.FragmentEditorBinding
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.editor.adapter.OprAdapter
import com.poe.poegps.feature.presentation.screens.editor.dialog.AddPillarDialogFragment
import com.poe.poegps.feature.presentation.screens.editor.dialog.AddPillarFromSaved10
import com.poe.poegps.feature.presentation.screens.editor.dialog.ChoosePillarDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.autoCleaned

@AndroidEntryPoint
class EditorFragment : Fragment(), MenuProvider,
    AddPillarDialogFragment.Listener,
    AddPillarFromSaved10.Listener,
    ChoosePillarDialogFragment.Listener {

    private var binding: FragmentEditorBinding by autoCleaned()
    private var adapter: OprAdapter by autoCleaned()
    private val viewModel by viewModels<EditorViewModel>()
    private val args: EditorFragmentArgs by lazy {
        EditorFragmentArgs.fromBundle(requireArguments())
    }

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
        return when (menuItem.itemId) {
            R.id.add_opr -> {
                openCreatePillarDialog()
                true
            }
            R.id.add_savedopr -> {
                val lines: List<ObjectDisplayable> = viewModel.getLineList10()
                val dialog = AddPillarDialogFragment.newInstance(lines)
                dialog.setListener(this)
                dialog.show(childFragmentManager, "CreateSavedPillar10DialogFragment")
                true
            }
            R.id.add_savedopr_from_small -> {

                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().removeMenuProvider(this)
    }

    private fun openCreatePillarDialog() {
        val dialog = AddPillarDialogFragment.newInstance(
            args.tplnr,
            requireContext().resources.getStringArray(R.array.wires)
        )
        dialog.setListener(this)
        dialog.show(childFragmentManager, "CreatePillarDialogFragment")
    }

    override fun onPillarAdded(pillar: OprDisplayable) {
        //На вьюмоделе берем список опор и добавляем в него новую опору, а здесь оно обновляется автоматически
        viewModel.addPillar(pillar)
    }

    override fun onSavedPillar10Added(obj: ObjectDisplayable) {
        val pillars = viewModel.getPillarList(obj.tplnr)
        val dialog = ChoosePillarDialogFragment.newInstance(pillars, requireContext().resources.getStringArray(R.array.wires))
        dialog.setListener(this)
        dialog.show(childFragmentManager, "ChoosePillarDialogFragment")
    }

    override fun onPillarChoose(pillar: OprDisplayable) {
        viewModel.addPillar(pillar)
    }

}
package com.poe.poegps.feature.presentation.screens.editor

import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poe.poegps.R
import com.poe.poegps.databinding.FragmentEditorBinding
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.editor.adapter.OprAdapter
import com.poe.poegps.feature.presentation.screens.editor.dialog.*
import com.poe.poegps.feature.presentation.screens.main.adapter.ObjectsAdapter
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.autoCleaned
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow

@AndroidEntryPoint
class EditorFragment : Fragment(), MenuProvider, OnClickListener,
    AddPillarDialogFragment.Listener,
    AddPillarFromSaved10.Listener,
    ChoosePillarDialogFragment.Listener,
    AddPillarFromSaved04.Listener,
    AddPillarFromTp.Listener,
    OprAdapter.OnOprClickListener {

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
        binding.lineName.text = args.pltxt
        viewModel.getParentName(args.tplnr)
        requireActivity().addMenuProvider(this)
        setupPillarAdapter()
        collectViewModel()
    }

    private fun collectViewModel() {
        collectLifecycleFlow(viewModel.oprDisplayable) { list ->
            adapter.submitList(list)
        }
    }

    private fun setupPillarAdapter() {
        val linearLayoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        adapter = OprAdapter()
        binding.coordinatesRv.adapter = adapter
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.coordinatesRv.layoutManager = linearLayoutManager
        adapter.onOprClickListener = this
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_editor, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.add_opr -> {
                /*findNavController().navigate(EditorFragmentDirections.actionEditorFragmentToAddPillarDialogFragment(
                    args.tplnr,
                    adapter.itemCount + 1
                   // requireContext().resources.getStringArray(R.array.wires)
                ))*/
                openCreatePillarDialog()
                true
            }
            R.id.add_savedopr -> {
                val dialog = AddPillarFromSaved10.newInstance()
                dialog.setListener(this)
                dialog.show(childFragmentManager, "CreateSavedPillar10DialogFragment")
                true
            }
            R.id.add_savedopr_from_small -> {
                val dialog = AddPillarFromSaved04.newInstance()
                dialog.setListener(this)
                dialog.show(childFragmentManager, "CreateSavedPillar04DialogFragment")
                true
            }
            R.id.add_savedtp_from_small -> {
                val dialog = AddPillarFromTp.newInstance()
                dialog.setListener(this)
                dialog.show(childFragmentManager, "CreateSavedTp04DialogFragment")
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
        //Заменить на запрос к БД и поиск максимального order+1
        val newOrderForOpr = adapter.itemCount + 1
        val dialog = AddPillarDialogFragment.newInstance(
            args.tplnr,
            newOrderForOpr
            //requireContext().resources.getStringArray(R.array.wires)
        )
        dialog.setListener(this)
        dialog.show(childFragmentManager, "CreatePillarDialogFragment")
    }

    private fun openChoosePillarDialog(obj: ObjectDisplayable) {
        //viewModel.getPillarList(obj.tplnr)

        //
        val newOrderForOpr = adapter.itemCount + 1
        val dialog = ChoosePillarDialogFragment.newInstance(
            obj.tplnr,
            newOrderForOpr,
            requireContext().resources.getStringArray(R.array.wires)
        )
        dialog.setListener(this)
        dialog.show(childFragmentManager, "ChoosePillarDialogFragment")
    }


    override fun onSavedPillar04Adding(obj: ObjectDisplayable) {
        openChoosePillarDialog(obj)
    }

    override fun onSavedPillar10Adding(obj: ObjectDisplayable) {
        openChoosePillarDialog(obj)
    }

    override fun onSavedTpAdded(obj: OprDisplayable) {
        viewModel.addPillarToDisplay(obj)
    }

    override fun onPillarAdded(pillar: OprDisplayable) {
        viewModel.addPillarToDisplay(pillar)
    }

    override fun onPillarChoose(pillar: OprDisplayable) {
        viewModel.addPillarToDisplay(pillar)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_btn -> {
                //   requireActivity().onBackPressed()
            }
        }
    }

    override fun onTakeCoordinatesClick(opr: OprDisplayable) {
        TODO("Not yet implemented")
    }

    override fun onLongClick(opr: OprDisplayable) {
        val dialog = PillarLongClickDialogFragment.newInstance(opr)
        dialog.show(childFragmentManager, "CreatePillarDialogFragment")
    }

}
package com.poe.poegps.feature.presentation.screens.editor.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.poe.poegps.R
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.model.PillarType
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow

@AndroidEntryPoint
class ChoosePillarFrom10DialogFragment : DialogFragment() {

    private lateinit var tplnr: String
    private lateinit var newTplnr: String
    private lateinit var lineName: String
    private val viewModel by viewModels<EditorViewModel>(ownerProducer = { requireParentFragment() })

    interface Listener {
        fun onPillarAdded(pillar: OprDisplayable)
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tplnr = it.getString(TPLNR, "")
            newTplnr = it.getString(NEW_TPLNR, "")
            lineName = it.getString(LINE_NAME, "")
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.choose_opr, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val coordSpinner = view.findViewById<Spinner>(R.id.coordSpinner)
        collectLifecycleFlow(viewModel.getPillar10List(tplnr)) {
            coordSpinner.adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, it)
        }
        val wireSpinner = view.findViewById<Spinner>(R.id.wireSpinner)
        collectLifecycleFlow(viewModel.getWires("10")) { wires ->
            wireSpinner.adapter = ArrayAdapter(
                requireActivity(),
                android.R.layout.simple_list_item_1,
                wires.map { it.name }
            )
        }

        view.findViewById<Button>(R.id.save).setOnClickListener {
            val pillarSpinner = coordSpinner.selectedItem as OprDisplayable
            val spinnerValue = wireSpinner.selectedItem.toString()

            val pillar = OprDisplayable(
                id = 0,
                tplnr = newTplnr,
                name = pillarSpinner.name,
                parentName = lineName,
                category = pillarSpinner.category,
                isAbon = pillarSpinner.isAbon,
                pillarType = PillarType.PILLAR.name,
                wire = spinnerValue,
                lat = pillarSpinner.lat,
                lng = pillarSpinner.lng
            )
            listener?.onPillarAdded(pillar)
            dismiss()
        }
        view.findViewById<Button>(R.id.cancel).setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val TPLNR = "tplnr"
        private const val NEW_TPLNR = "newtplnr"
        private const val LINE_NAME = "linename"

        fun newInstance(
            tplnr: String,
            newTplnr: String,
            lineName: String
        ): ChoosePillarFrom10DialogFragment {
            val args = Bundle()
            args.putString(TPLNR, tplnr)
            args.putString(NEW_TPLNR, newTplnr)
            args.putString(LINE_NAME, lineName)
            val fragment = ChoosePillarFrom10DialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
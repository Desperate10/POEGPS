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
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow

@AndroidEntryPoint
class ChoosePillarDialogFragment : DialogFragment() {

    private lateinit var wire: Array<String>
    private lateinit var tplnr: String
    private var order: Int = 0
    private val viewModel by viewModels<EditorViewModel>(ownerProducer = { requireParentFragment() })

    interface Listener {
        fun onPillarChoose(pillar: OprDisplayable)
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            wire = it.getStringArray(WIRE) ?: arrayOf()
            order = it.getInt(ORDER, 0)
            tplnr = it.getString(TPLNR, "")
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
        val view = inflater.inflate(R.layout.choose_opr, container, false)
        val wireSpinner = view.findViewById<Spinner>(R.id.wireSpinner)

        wireSpinner.adapter =
            ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, wire)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val coordSpinner = view.findViewById<Spinner>(R.id.coordSpinner)
        collectLifecycleFlow(viewModel.getPillarList(tplnr)) {
            coordSpinner.adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, it)
        }
        val wireSpinner = view.findViewById<Spinner>(R.id.wireSpinner)

        view.findViewById<Button>(R.id.save).setOnClickListener {
            val pillarSpinner = coordSpinner.selectedItem as OprDisplayable
            val spinnerValue = wireSpinner.selectedItem.toString()

            val pillar = OprDisplayable(
                id = 0,
                tplnr = pillarSpinner.tplnr,
                name = pillarSpinner.name,
                parentName = pillarSpinner.parentName,
                order = order,
                wire = spinnerValue,
                lat = pillarSpinner.lat,
                lng = pillarSpinner.lng
            )
            listener?.onPillarChoose(pillar)
            dismiss()
        }
    }

    companion object {
        private const val WIRE = "wire"
        private const val TPLNR = "tplnr"
        private const val ORDER = "order"

        fun newInstance(
            tplnr: String,
            order: Int,
            wire: Array<String>
        ): ChoosePillarDialogFragment {
            val args = Bundle()
            args.putString(TPLNR, tplnr)
            args.putInt(ORDER, order)
            args.putStringArray(WIRE, wire)
            val fragment = ChoosePillarDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
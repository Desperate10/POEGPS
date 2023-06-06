package com.poe.poegps.feature.presentation.screens.editor.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
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
class AddPillarDialogFragment : DialogFragment() {

    private lateinit var tplnr: String
    private lateinit var lineName: String
    private lateinit var category: String
    private var isAbon: Boolean = false

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
            lineName = it.getString("lineName", "")
            category = it.getString("category", "")
            isAbon = it.getBoolean("isAbon", false)
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
        return inflater.inflate(R.layout.create_opr, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val oprName = view.findViewById<EditText>(R.id.oprName)
        val wireSpinner = view.findViewById<Spinner>(R.id.wireSpinner)

        collectLifecycleFlow(viewModel.getWires(category)) { wires ->
            wireSpinner.adapter = ArrayAdapter(
                requireActivity(),
                android.R.layout.simple_list_item_1,
                wires.map { it.name }
            )
        }

        view.findViewById<Button>(R.id.save).setOnClickListener {
            val oprNameTxt = oprName.text.toString()
            val spinnerValue = wireSpinner.selectedItem.toString()

            val pillar = OprDisplayable(
                tplnr = tplnr,
                name = oprNameTxt,
                parentName = lineName,
                category = category,
                pillarType = PillarType.PILLAR.name,
                isAbon = isAbon,
                wire = spinnerValue
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
        private const val ORDER = "order"

        fun newInstance(
            tplnr: String,
            lineName: String,
            category: String,
            isAbon: Boolean
        ): AddPillarDialogFragment {
            val args = Bundle()
            args.putString(TPLNR, tplnr)
            args.putString("lineName", lineName)
            args.putString("category", category)
            args.putBoolean("isAbon", isAbon)
            //args.putStringArray(WIRE, wire)
            val fragment = AddPillarDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
package com.poe.poegps.feature.presentation.screens.editor.dialog

import android.os.Bundle
import android.util.Log
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
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddPillarDialogFragment : DialogFragment() {

    private lateinit var tplnr: String
    //private lateinit var wire: Array<String>
    private var order: Int = 0
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
            order = it.getInt(ORDER, 0)
           // wire = it.getStringArray(WIRE) ?: arrayOf()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.create_opr, container, false)
        val spinner = view.findViewById<Spinner>(R.id.wireSpinner)
        spinner.adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, requireContext().resources.getStringArray(R.array.wires))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val oprName = view.findViewById<EditText>(R.id.oprName)
        val wireSpinner = view.findViewById<Spinner>(R.id.wireSpinner)
        Log.d("testim", viewModel.parentObjectName.value)

        view.findViewById<Button>(R.id.save).setOnClickListener {
            val oprNameTxt = oprName.text.toString()
            val spinnerValue = wireSpinner.selectedItem.toString()

            val pillar = OprDisplayable(tplnr = tplnr, name = oprNameTxt, parentName = viewModel.parentObjectName.value, wire = spinnerValue, order = order)
            listener?.onPillarAdded(pillar)

            dismiss()
        }
    }

    companion object {
        private const val TPLNR = "tplnr"
        private const val WIRE = "wire"
        private const val ORDER = "order"

        fun newInstance(tplnr: String, order: Int): AddPillarDialogFragment {
            val args = Bundle()
            args.putString(TPLNR, tplnr)
            args.putInt(ORDER, order)
            //args.putStringArray(WIRE, wire)
            val fragment = AddPillarDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
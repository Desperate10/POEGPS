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
class OnCreateOtpDialogFragment: DialogFragment() {

    private val viewModel by viewModels<EditorViewModel>(ownerProducer = { requireParentFragment() })
    private var opr: OprDisplayable? = null

    interface Listener {
        fun onOtpCreated(pillarStart: OprDisplayable, pillarSecond: OprDisplayable)
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            opr = it.getParcelable(PILLAR)
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
        val view = inflater.inflate(R.layout.create_opr, container, false)
        val spinner = view.findViewById<Spinner>(R.id.wireSpinner)
        spinner.adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, requireContext().resources.getStringArray(
            R.array.wires))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val oprName = view.findViewById<EditText>(R.id.oprName)
        val wireSpinner = view.findViewById<Spinner>(R.id.wireSpinner)

        view.findViewById<Button>(R.id.save).setOnClickListener {
            val oprNameTxt = oprName.text.toString()
            val spinnerValue = wireSpinner.selectedItem.toString()

            val lineName = "Відп. від оп. ${opr!!.name} до оп. $oprNameTxt"
            viewModel.setLineName(lineName)

            val oldPillar = opr!!.copy(parentName = lineName, order = 1)
            val pillar = OprDisplayable(tplnr = opr!!.tplnr, name = oprNameTxt, parentName = lineName, wire = spinnerValue, order = 2)
            Log.d("testim", "$oldPillar $pillar")
            listener?.onOtpCreated(oldPillar, pillar)

            dismiss()
        }
        view.findViewById<Button>(R.id.cancel).setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val PILLAR = "pillar"

        fun newInstance(opr: OprDisplayable): OnCreateOtpDialogFragment {
            val args = Bundle()
            args.putParcelable(PILLAR, opr)
            val fragment = OnCreateOtpDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

}
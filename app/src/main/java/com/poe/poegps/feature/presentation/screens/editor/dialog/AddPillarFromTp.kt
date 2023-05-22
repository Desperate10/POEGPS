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
import com.poe.poegps.feature.presentation.mapper.toOprDisplayable
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow

@AndroidEntryPoint
class AddPillarFromTp: DialogFragment() {
    private lateinit var pltxt: String
    private lateinit var tplnr: String
    private var order: Int = 0
    private val viewModel by viewModels<EditorViewModel>(ownerProducer = { requireParentFragment() })

    interface Listener {
        fun onSavedTpAdded(obj : OprDisplayable)
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pltxt = it.getString(PLTXT).toString()
            tplnr = it.getString(TPLNR).toString()
            order = it.getInt(ORDER, 0)
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
        val view = inflater.inflate(R.layout.create_saved_opr, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tpSpinner = view.findViewById<Spinner>(R.id.lineSpinner)
        collectLifecycleFlow(viewModel.getTpList()) {
            tpSpinner.adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, it)
        }

        view.findViewById<Button>(R.id.choose).setOnClickListener {
            val spinnerValue = tpSpinner.selectedItem as OprDisplayable
            val pillar = OprDisplayable(
                id = 0,
                tplnr = tplnr,
                name = spinnerValue.name,
                parentName = pltxt,
                order = order,
                wire = spinnerValue.wire,
                lat = spinnerValue.lat,
                lng = spinnerValue.lng
            )
            listener?.onSavedTpAdded(pillar)
            dismiss()
        }
        view.findViewById<Button>(R.id.cancel).setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val PLTXT = "pltxt"
        private const val TPLNR = "tplnr"
        private const val ORDER = "order"

        fun newInstance(pltxt: String, tplnr: String, order:Int): AddPillarFromTp {
            val args = Bundle()
            args.putString(PLTXT, pltxt)
            args.putString(TPLNR, tplnr)
            args.putInt(ORDER, order)
            val fragment = AddPillarFromTp()
            fragment.arguments = args
            return fragment
        }
    }

}
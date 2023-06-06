package com.poe.poegps.feature.presentation.screens.editor.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.poe.poegps.R
import com.poe.poegps.feature.domain.model.Pillar
import com.poe.poegps.feature.presentation.mapper.toOprDisplayable
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.model.PillarType
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow

@AndroidEntryPoint
class AddPillarFromTp: DialogFragment() {
    private lateinit var pltxt: String
    private lateinit var tplnr: String
    private lateinit var ucat: String
    private var isAbon = false
    private val viewModel by viewModels<EditorViewModel>(ownerProducer = { requireParentFragment() })

    interface Listener {
        fun onPillarAdded(obj : OprDisplayable)
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
            ucat = it.getString(UCAT).toString()
            isAbon = it.getBoolean(ISABON)
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
        return inflater.inflate(R.layout.create_saved_opr, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tpSpinner = view.findViewById<Spinner>(R.id.lineSpinner)
        collectLifecycleFlow(viewModel.getTpList(isAbon)) {
            if (it.isEmpty()) {
                Toast.makeText(requireContext(), "ТП з координатами немає", Toast.LENGTH_SHORT).show()
                dismiss()
                return@collectLifecycleFlow
            }
            tpSpinner.adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, it)
        }

        view.findViewById<Button>(R.id.choose).setOnClickListener {
            val spinnerValue = tpSpinner.selectedItem as OprDisplayable
            val pillar = OprDisplayable(
                id = 0,
                tplnr = tplnr,
                name = spinnerValue.name,
                parentName = viewModel.lineName.value.toString(),
                category = ucat,
                isAbon = spinnerValue.isAbon,
                pillarType = "",
                wire = spinnerValue.wire,
                lat = spinnerValue.lat,
                lng = spinnerValue.lng
            )
            listener?.onPillarAdded(pillar)
            dismiss()
        }
        view.findViewById<Button>(R.id.cancel).setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val PLTXT = "pltxt"
        private const val TPLNR = "tplnr"
        private const val UCAT = "ucat"
        private const val ISABON = "isAbon"

        fun newInstance(pltxt: String, tplnr: String, ucat: String, isAbon: Boolean): AddPillarFromTp {
            val args = Bundle()
            args.putString(PLTXT, pltxt)
            args.putString(TPLNR, tplnr)
            args.putString(UCAT, ucat)
            args.putBoolean(ISABON, isAbon)
            val fragment = AddPillarFromTp()
            fragment.arguments = args
            return fragment
        }
    }

}
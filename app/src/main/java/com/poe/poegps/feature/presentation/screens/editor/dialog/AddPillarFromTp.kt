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
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow

@AndroidEntryPoint
class AddPillarFromTp: DialogFragment() {
    private var tps: ArrayList<ObjectDisplayable>? = null
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
            tps = it.getParcelableArrayList(TPS)
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
            listener?.onSavedTpAdded(spinnerValue)
            dismiss()
        }
        view.findViewById<Button>(R.id.cancel).setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val TPS = "tps"

        fun newInstance(): AddPillarFromTp {
            //val args = Bundle()
            //args.putParcelableArrayList(TPS, tps)
            val fragment = AddPillarFromTp()
            //fragment.arguments = args
            return fragment
        }
    }

}
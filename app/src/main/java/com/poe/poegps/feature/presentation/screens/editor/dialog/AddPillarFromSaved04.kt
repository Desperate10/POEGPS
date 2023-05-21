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
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow

@AndroidEntryPoint
class AddPillarFromSaved04 : DialogFragment() {

    private var lines: ArrayList<ObjectDisplayable>? = null
    private val viewModel by viewModels<EditorViewModel>(ownerProducer = { requireParentFragment() })

    interface Listener {
        fun onSavedPillar04Adding(obj : ObjectDisplayable)
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            lines = it.getParcelableArrayList(LINES)
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

        val lineSpinner = view.findViewById<Spinner>(R.id.lineSpinner)
        collectLifecycleFlow(viewModel.getLineList04()) {
            lineSpinner.adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, it)
        }
        view.findViewById<Button>(R.id.choose).setOnClickListener {
            val spinnerValue = lineSpinner.selectedItem as ObjectDisplayable
            listener?.onSavedPillar04Adding(spinnerValue)
            dismiss()
        }
        view.findViewById<Button>(R.id.cancel).setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val LINES = "lines"

        fun newInstance(): AddPillarFromSaved04 {
            //val args = Bundle()
            //args.putParcelableArrayList(LINES, lines)
            val fragment = AddPillarFromSaved04()
            //fragment.arguments = args
            return fragment
        }
    }
}
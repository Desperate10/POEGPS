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
import com.poe.poegps.R
import com.poe.poegps.feature.presentation.model.ObjectDisplayable


class AddPillarFromSaved10: DialogFragment() {

    private var lines: ArrayList<ObjectDisplayable>? = null

    interface Listener {
        fun onSavedPillar10Added(obj : ObjectDisplayable)
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
        val spinner = view.findViewById<Spinner>(R.id.lineSpinner)
        lines?.let {
            spinner.adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, it)
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lineSpinner = view.findViewById<Spinner>(R.id.lineSpinner)

        view.findViewById<Button>(R.id.save).setOnClickListener {
            val spinnerValue = lineSpinner.selectedItem as ObjectDisplayable
            listener?.onSavedPillar10Added(spinnerValue)
            dismiss()
        }
    }

    companion object {
        private const val LINES = "lines"

        fun newInstance(lines: ArrayList<ObjectDisplayable>): AddPillarFromSaved10 {
            val args = Bundle()
            args.putParcelableArrayList(LINES, lines)
            val fragment = AddPillarFromSaved10()
            fragment.arguments = args
            return fragment
        }
    }

}
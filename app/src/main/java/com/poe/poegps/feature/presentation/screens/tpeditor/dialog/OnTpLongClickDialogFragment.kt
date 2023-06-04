package com.poe.poegps.feature.presentation.screens.tpeditor.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.poe.poegps.R
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import com.poe.poegps.feature.presentation.screens.editor.dialog.ChoosePillarFrom10DialogFragment

class OnTpLongClickDialogFragment: DialogFragment() {

    private var tplnr: String? = null

    interface Listener {
        fun onTpCoordCleared(tplnr: String)
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tplnr = it.getString("tplnr")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.tp_long_tap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.createOtp).setOnClickListener {
            listener?.onTpCoordCleared(tplnr!!)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        fun newInstance(fragmentManager: FragmentManager, tplnr: String) {
            val dialog = OnTpLongClickDialogFragment()
            val args = Bundle()
            args.putString("tplnr", tplnr)
            dialog.arguments = args
            dialog.show(fragmentManager, "OnTpLongClickDialogFragment")
        }
    }
}
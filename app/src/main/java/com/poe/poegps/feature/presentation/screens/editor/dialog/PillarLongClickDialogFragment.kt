package com.poe.poegps.feature.presentation.screens.editor.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.poe.poegps.R
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PillarLongClickDialogFragment: DialogFragment() {

    private val viewModel by viewModels<EditorViewModel>(ownerProducer = { requireParentFragment() })
    private var pillar: OprDisplayable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pillar = it.getParcelable(OBJ)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.opr_long_tap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.createOtp).setOnClickListener {
            pillar?.let { it1 -> viewModel.createOtp(it1) }
            dismiss()
        }
        view.findViewById<Button>(R.id.clear_coord).setOnClickListener {
            viewModel.clearCoord(pillar)
            dismiss()
        }
        view.findViewById<Button>(R.id.deleteOpr).setOnClickListener {
            viewModel.deleteOpr(pillar)
            dismiss()
        }
        view.findViewById<Button>(R.id.cancel).setOnClickListener {
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
        private const val TAG = "PillarLongClickDialogFragment"
        private const val OBJ = "pillar"

        fun newInstance(
            pillar: OprDisplayable
        ): PillarLongClickDialogFragment {
            val args = Bundle()
            args.putParcelable(OBJ, pillar)
            val fragment = PillarLongClickDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
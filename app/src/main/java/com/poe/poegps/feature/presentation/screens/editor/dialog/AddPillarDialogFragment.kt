package com.poe.poegps.feature.presentation.screens.editor.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.poe.poegps.R
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.model.PillarType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddPillarDialogFragment : DialogFragment() {

    private lateinit var tplnr: String
    private lateinit var lineName: String
    private lateinit var category: String
    private var isAbon: Boolean = false

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

        view.findViewById<Button>(R.id.save).setOnClickListener {
            val oprNameTxt = oprName.text.toString()
            val pillarType = when {
                lineName.contains("КВ-0", ignoreCase = true) -> PillarType.KL
                lineName.contains("КВ-6", ignoreCase = true) -> PillarType.KL
                lineName.contains("КВ-10", ignoreCase = true) -> PillarType.KL
                lineName.contains("КЛ-0", ignoreCase = true) -> PillarType.KL
                lineName.contains("КЛ-6", ignoreCase = true) -> PillarType.KL
                lineName.contains("КЛ-10", ignoreCase = true) -> PillarType.KL
                else -> PillarType.PILLAR
            }

            val pillar = OprDisplayable(
                tplnr = tplnr,
                name = oprNameTxt,
                parentName = lineName,
                category = category,
                pillarType = pillarType.name,
                isAbon = isAbon
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
            val fragment = AddPillarDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
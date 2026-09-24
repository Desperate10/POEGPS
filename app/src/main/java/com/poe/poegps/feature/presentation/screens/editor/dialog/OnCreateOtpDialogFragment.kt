package com.poe.poegps.feature.presentation.screens.editor.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.poe.poegps.R
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.model.PillarType
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnCreateOtpDialogFragment : DialogFragment() {

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
        return inflater.inflate(R.layout.create_opr, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val oprName = view.findViewById<EditText>(R.id.oprName)

        view.findViewById<Button>(R.id.save).setOnClickListener {
            val oprNameTxt = oprName.text.toString()

            //Добавить сюда лайннейм основной линии если добавляем поиск по названию
            val lineName = "Відп. від оп. ${opr!!.name} до оп. $oprNameTxt"
            viewModel.setLineName(lineName)

            val oldPillar = opr!!.copy(parentName = lineName)
            val pillar = OprDisplayable(
                tplnr = opr!!.tplnr,
                name = oprNameTxt,
                parentName = lineName,
                category = opr!!.category,
                pillarType = PillarType.PILLAR.name,
                isAbon = opr!!.isAbon
            )
            listener?.onOtpCreated(oldPillar, pillar)

            dismiss()
        }
        view.findViewById<Button>(R.id.cancel).setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val PILLAR = "pillar"

        fun newInstance(lineName: String, opr: OprDisplayable): OnCreateOtpDialogFragment {
            val args = Bundle()
            args.putParcelable(PILLAR, opr)
            val fragment = OnCreateOtpDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

}
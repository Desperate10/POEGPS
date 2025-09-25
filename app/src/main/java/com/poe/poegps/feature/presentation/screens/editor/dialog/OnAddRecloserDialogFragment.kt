package com.poe.poegps.feature.presentation.screens.editor.dialog

import android.os.Bundle
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
import com.poe.poegps.feature.presentation.model.RecloserDisplayable
import com.poe.poegps.feature.presentation.model.RecloserType
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnAddRecloserDialogFragment : DialogFragment() {

    private val viewModel by viewModels<EditorViewModel>(ownerProducer = { requireParentFragment() })
    private var opr: OprDisplayable? = null

    interface Listener {
        fun onRecloserCreated(recloser: RecloserDisplayable)
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
        return inflater.inflate(R.layout.create_recloser, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recloserName = view.findViewById<EditText>(R.id.recloserName)
        val recloserTypeSpinner = view.findViewById<Spinner>(R.id.typeSpinner)
        recloserTypeSpinner.adapter = ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_list_item_1,
            RecloserType.values()
        )

        view.findViewById<Button>(R.id.save).setOnClickListener {
            val recloserNameTxt = recloserName.text.toString()
            val spinnerValue = recloserTypeSpinner.selectedItem as RecloserType
            val recloser = RecloserDisplayable(
                tplnr = opr!!.tplnr,
                name = recloserNameTxt,
                opr = opr!!.name,
                type = spinnerValue,
                lat = opr!!.lat,
                lng = opr!!.lng
            )
            viewModel.addRecloserToPillar(opr!!.id, recloserNameTxt)

            listener?.onRecloserCreated(recloser)

            dismiss()
        }
        view.findViewById<Button>(R.id.cancel).setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val PILLAR = "pillar"

        fun newInstance(opr: OprDisplayable): OnAddRecloserDialogFragment {
            val args = Bundle()
            args.putParcelable(PILLAR, opr)
            val fragment = OnAddRecloserDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }


}
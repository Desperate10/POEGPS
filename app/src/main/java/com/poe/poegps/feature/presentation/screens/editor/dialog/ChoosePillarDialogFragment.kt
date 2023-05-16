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
import com.poe.poegps.feature.presentation.model.OprDisplayable

class ChoosePillarDialogFragment : DialogFragment() {

    private var pillars: ArrayList<OprDisplayable>? = null
    private lateinit var wire: Array<String>

    interface Listener {
        fun onPillarChoose(pillar: OprDisplayable)
    }

    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pillars = it.getParcelableArrayList(PILLARS)
            wire = it.getStringArray(WIRE) ?: arrayOf()
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
        val view = inflater.inflate(R.layout.choose_opr, container, false)
        val coordSpinner = view.findViewById<Spinner>(R.id.coordSpinner)
        val wireSpinner = view.findViewById<Spinner>(R.id.wireSpinner)
        pillars?.let {
            coordSpinner.adapter =
                ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, it)
        }
        wireSpinner.adapter =
            ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, wire)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val coordSpinner = view.findViewById<Spinner>(R.id.coordSpinner)
        val wireSpinner = view.findViewById<Spinner>(R.id.wireSpinner)

        view.findViewById<Button>(R.id.save).setOnClickListener {
            val pillarSpinner = coordSpinner.selectedItem as OprDisplayable
            val spinnerValue = wireSpinner.selectedItem.toString()

            val pillar = OprDisplayable(
                tplnr = pillarSpinner.tplnr,
                name = pillarSpinner.name,
                wire = spinnerValue,
                lat = pillarSpinner.lat,
                lng = pillarSpinner.lng
            )
            listener?.onPillarChoose(pillar)
            dismiss()
        }
    }

    companion object {
        private const val PILLARS = "pillars"
        private const val WIRE = "wire"

        fun newInstance(
            pillars: ArrayList<OprDisplayable>,
            wire: Array<String>
        ): ChoosePillarDialogFragment {
            val args = Bundle()
            args.putParcelableArrayList(PILLARS, pillars)
            args.putStringArray(WIRE, wire)
            val fragment = ChoosePillarDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
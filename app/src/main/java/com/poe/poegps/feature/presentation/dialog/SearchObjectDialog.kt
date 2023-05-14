package com.poe.poegps.feature.presentation.dialog

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.poe.poegps.R

class SearchObjectDialog : DialogFragment() {
    interface OnInputListener {
        fun sendInput(input: String?)
    }

    var mOnInputListener: OnInputListener? = null
    private var objects: java.util.ArrayList<String>? = null
    private var hint: String? = null
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        assert(arguments != null)
        objects = requireArguments().getStringArrayList("OBJECTS")
        hint = requireArguments().getString("HINT")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(
           R.layout.dialog_searchable_spinner, container, false
        )
        val mInput = view.findViewById<EditText>(R.id.input)
        val mList = view.findViewById<ListView>(R.id.list_view)
        val dialogHint = view.findViewById<TextView>(R.id.dialog_hint_tv)
        dialogHint.text = hint
        objects?.let {
            adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, it)
            mList.adapter = adapter
        }
        mInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }

            override fun afterTextChanged(s: Editable) {}
        })
        mList.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                val input = mList.getItemAtPosition(position).toString()
                mOnInputListener!!.sendInput(input)
                dismiss()
            }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mOnInputListener = activity as OnInputListener?
        } catch (e: ClassCastException) {
            Log.e(
                "dialog", "onAttach: ClassCastException: "
                        + e.message
            )
        }
    }

    fun newInstance(
        objects: ArrayList<String?>?,
        hint: String?
    ): SearchObjectDialog {
        val fragment: SearchObjectDialog = SearchObjectDialog()
        val args = Bundle()
        args.putStringArrayList("OBJECTS", objects)
        args.putString("HINT", hint)
        fragment.arguments = args
        return fragment
    }
}
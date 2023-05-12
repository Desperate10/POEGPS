package com.poe.poegps.feature.presentation.screens.main.spinner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import com.poe.poegps.feature.data.remote.model.ObjectType

class ObjectsSpinnerAdapter(context: Context) :
    ArrayAdapter<ObjectType>(context, 0, ObjectType.values()) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var text = convertView as CheckedTextView?
        if (text == null) {
            text = LayoutInflater.from(context).inflate(
                android.R.layout.simple_spinner_dropdown_item,
                parent,
                false
            ) as CheckedTextView
        }

        text.text = getItem(position)?.type
        return text
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        var text = convertView as CheckedTextView?
        if (text == null) {
            text = LayoutInflater.from(context).inflate(
                android.R.layout.simple_spinner_dropdown_item,
                parent,
                false
            ) as CheckedTextView
        }

        text.text = getItem(position)?.type
        return text

    }
}
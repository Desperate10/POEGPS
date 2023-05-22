package com.poe.poegps.feature.presentation.screens.editor.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.poe.poegps.databinding.OprItemBinding
import com.poe.poegps.feature.presentation.model.OprDisplayable

class OprAdapter : ListAdapter<OprDisplayable, OprViewHolder>(OprDiffUtil){

    var onOprClickListener: OnOprClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OprViewHolder {
        val binding = OprItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OprViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OprViewHolder, position: Int) {
        val oprItem = getItem(position)
        with(holder.binding) {
            opr.text = oprItem.name//replace with resource string
            wire.text = oprItem.wire
            lat.text = oprItem.lat
            lng.text = oprItem.lng
            takeCoord.isEnabled = lat.text == "0.0"
            takeCoord.setOnClickListener {
                onOprClickListener?.onTakeCoordinatesClick(oprItem, position)
            }
            textViews.setOnLongClickListener {
                onOprClickListener?.onLongClick(oprItem)
                true
            }
        }
    }

    interface OnOprClickListener {
        fun onTakeCoordinatesClick(opr: OprDisplayable, position: Int)

        fun onLongClick(opr: OprDisplayable)
    }
}
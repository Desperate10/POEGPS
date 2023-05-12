package com.poe.poegps.feature.presentation.screens.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.poe.poegps.databinding.ObjectItemBinding
import com.poe.poegps.feature.presentation.model.ObjectDisplayable

class ObjectsAdapter : ListAdapter<ObjectDisplayable, ObjectsViewHolder>(ObjectsDiffUtil){

    var onObjectClickListener : OnObjectClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObjectsViewHolder {
        val binding = ObjectItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ObjectsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ObjectsViewHolder, position: Int) {
        var objItem = getItem(position)
        with(holder.binding) {
            tplnr.text = objItem.tplnr
            name.text = objItem.name

            root.setOnClickListener {
                onObjectClickListener?.onClick(objItem)
            }
            root.setOnLongClickListener {
                onObjectClickListener?.onLongClick(objItem)
                true
            }
        }
    }

    interface OnObjectClickListener {
        fun onClick(obj: ObjectDisplayable)

        fun onLongClick(obj: ObjectDisplayable)
    }

}
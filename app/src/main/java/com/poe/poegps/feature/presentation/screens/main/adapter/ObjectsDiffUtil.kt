package com.poe.poegps.feature.presentation.screens.main.adapter

import androidx.recyclerview.widget.DiffUtil
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable

object ObjectsDiffUtil: DiffUtil.ItemCallback<ObjectDisplayable>() {

    override fun areItemsTheSame(oldItem: ObjectDisplayable, newItem: ObjectDisplayable): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(
        oldItem: ObjectDisplayable,
        newItem: ObjectDisplayable
    ): Boolean {
        return oldItem == newItem
    }

}
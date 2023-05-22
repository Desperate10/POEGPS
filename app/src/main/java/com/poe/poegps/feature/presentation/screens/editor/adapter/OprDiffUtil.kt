package com.poe.poegps.feature.presentation.screens.editor.adapter

import androidx.recyclerview.widget.DiffUtil
import com.poe.poegps.feature.presentation.model.OprDisplayable

object OprDiffUtil: DiffUtil.ItemCallback<OprDisplayable>() {
    override fun areItemsTheSame(oldItem: OprDisplayable, newItem: OprDisplayable): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: OprDisplayable, newItem: OprDisplayable): Boolean {
        return oldItem == newItem
    }
}
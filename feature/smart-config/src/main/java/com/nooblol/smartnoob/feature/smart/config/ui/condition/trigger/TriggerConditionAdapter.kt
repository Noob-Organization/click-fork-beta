/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nooblol.smartnoob.feature.smart.config.ui.condition.trigger

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.nooblol.smartnoob.core.domain.model.condition.TriggerCondition
import com.nooblol.smartnoob.feature.smart.config.databinding.ItemTriggerConditionBinding
import com.nooblol.smartnoob.feature.smart.config.ui.common.bindings.bind
import com.nooblol.smartnoob.feature.smart.config.ui.common.model.condition.UiTriggerCondition

/**
 * Adapter displaying the conditions for the event displayed by the dialog.
 * Also provide a item displayed in the last position to add a new condition.
 *
 * @param conditionClickedListener the listener called when the user clicks on a condition.
 */
class TriggerConditionAdapter(
    private val conditionClickedListener: (TriggerCondition) -> Unit,
) : ListAdapter<UiTriggerCondition, TriggerConditionViewHolder>(TriggerConditionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TriggerConditionViewHolder(
            ItemTriggerConditionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )

    override fun onBindViewHolder(holder: TriggerConditionViewHolder, position: Int) {
        holder.onBindCondition((getItem(position)), conditionClickedListener)
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [TriggerConditionAdapter] list. */
object TriggerConditionDiffUtilCallback: DiffUtil.ItemCallback<UiTriggerCondition>() {
    override fun areItemsTheSame(oldItem: UiTriggerCondition, newItem: UiTriggerCondition): Boolean =
        oldItem.condition.id == newItem.condition.id
    override fun areContentsTheSame(oldItem: UiTriggerCondition, newItem: UiTriggerCondition): Boolean =
        oldItem == newItem
}

/**
 * View holder displaying a condition in the [TriggerConditionAdapter].
 * @param viewBinding the view binding for this item.
 */
class TriggerConditionViewHolder(
    private val viewBinding: ItemTriggerConditionBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    /**
     * Bind this view holder as a condition item.
     *
     * @param condition the condition to be represented by this item.
     * @param conditionClickedListener listener notified upon user click on this item.
     */
    fun onBindCondition(condition: UiTriggerCondition, conditionClickedListener: (TriggerCondition) -> Unit) {
        viewBinding.bind(condition, conditionClickedListener)
    }
}
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
package com.nooblol.smartnoob.feature.smart.config.ui.action.toggleevent

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.nooblol.smartnoob.core.base.identifier.Identifier
import com.nooblol.smartnoob.core.domain.model.action.ToggleEvent
import com.nooblol.smartnoob.core.ui.bindings.buttons.MultiStateButtonConfig
import com.nooblol.smartnoob.core.ui.bindings.buttons.setChecked
import com.nooblol.smartnoob.core.ui.bindings.buttons.setOnCheckedListener
import com.nooblol.smartnoob.core.ui.bindings.buttons.setup
import com.nooblol.smartnoob.feature.smart.config.R
import com.nooblol.smartnoob.feature.smart.config.databinding.ItemEventToggleBinding
import com.nooblol.smartnoob.core.ui.databinding.ItemListHeaderBinding


class EventToggleAdapter(
    private val onEventToggleStateChanged: (Identifier, ToggleEvent.ToggleType?) -> Unit,
) : ListAdapter<EventTogglesListItem, RecyclerView.ViewHolder>(ItemEventToggleDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is EventTogglesListItem.Header -> R.layout.item_list_header
            is EventTogglesListItem.Item -> R.layout.item_event_toggle
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_list_header ->
                HeaderViewHolder(
                    viewBinding = ItemListHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                )

            R.layout.item_event_toggle ->
                ItemViewHolder(
                    viewBinding = ItemEventToggleBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    onEventToggleStateChanged = onEventToggleStateChanged,
                )

            else -> throw IllegalArgumentException("Unsupported view type $viewType")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.onBind(getItem(position) as EventTogglesListItem.Header)
            is ItemViewHolder -> holder.onBind(getItem(position) as EventTogglesListItem.Item)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ItemViewHolder) holder.onUnbind()
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [EventToggleAdapter] list. */
object ItemEventToggleDiffUtilCallback: DiffUtil.ItemCallback<EventTogglesListItem>() {
    override fun areItemsTheSame(oldItem: EventTogglesListItem, newItem: EventTogglesListItem): Boolean =
          when {
              oldItem is EventTogglesListItem.Header && newItem is EventTogglesListItem.Header ->
                  oldItem.title == newItem.title
              oldItem is EventTogglesListItem.Item && newItem is EventTogglesListItem.Item ->
                  oldItem.eventId == newItem.eventId
              else -> false
          }

    override fun areContentsTheSame(oldItem: EventTogglesListItem, newItem: EventTogglesListItem): Boolean =
        true
}


/**
 * View holder displaying an action in the [EventToggleAdapter].
 * @param viewBinding the view binding for this item.
 */
class HeaderViewHolder(
    private val viewBinding: ItemListHeaderBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: EventTogglesListItem.Header) {
        viewBinding.textHeader.text = item.title
    }
}

/**
 * View holder displaying an action in the [EventToggleAdapter].
 * @param viewBinding the view binding for this item.
 */
class ItemViewHolder(
    private val viewBinding: ItemEventToggleBinding,
    private val onEventToggleStateChanged: (Identifier, ToggleEvent.ToggleType?) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    private companion object {
        private const val BUTTON_ENABLE_EVENT = 0
        private const val BUTTON_TOGGLE_EVENT = 1
        private const val BUTTON_DISABLE_EVENT = 2

        private val BUTTONS_CONFIG = MultiStateButtonConfig(
            icons = listOf(R.drawable.ic_confirm, R.drawable.ic_invert, R.drawable.ic_cancel),
            selectionRequired = false,
            singleSelection = true,
        )
    }

    init {
        viewBinding.toggleTypeButton.setup(BUTTONS_CONFIG)
    }

    fun onBind(item: EventTogglesListItem.Item) {
        viewBinding.apply {
            eventName.text = item.eventName
            textActionsCount.text = item.actionsCount.toString()
            textConditionCount.text = item.conditionsCount.toString()

            toggleTypeButton.setChecked(
                when (item.toggleState) {
                    ToggleEvent.ToggleType.ENABLE -> BUTTON_ENABLE_EVENT
                    ToggleEvent.ToggleType.TOGGLE -> BUTTON_TOGGLE_EVENT
                    ToggleEvent.ToggleType.DISABLE -> BUTTON_DISABLE_EVENT
                    else -> null
                }
            )

            toggleTypeButton.setOnCheckedListener { newChecked ->
                val newState = when (newChecked) {
                    BUTTON_ENABLE_EVENT -> ToggleEvent.ToggleType.ENABLE
                    BUTTON_TOGGLE_EVENT -> ToggleEvent.ToggleType.TOGGLE
                    BUTTON_DISABLE_EVENT -> ToggleEvent.ToggleType.DISABLE
                    else -> null
                }

                onEventToggleStateChanged(item.eventId, newState)
            }
        }
    }

    fun onUnbind() {
        viewBinding.toggleTypeButton.setOnCheckedListener(null)
    }
}



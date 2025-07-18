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
package com.nooblol.smartnoob.feature.smart.config.ui.event.copy

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.nooblol.smartnoob.core.ui.bindings.lists.updateState
import com.nooblol.smartnoob.core.domain.model.event.Event
import com.nooblol.smartnoob.core.ui.bindings.lists.newDividerWithoutHeader
import com.nooblol.smartnoob.core.common.overlays.base.viewModels
import com.nooblol.smartnoob.core.common.overlays.dialog.implementation.CopyDialog
import com.nooblol.smartnoob.feature.smart.config.R
import com.nooblol.smartnoob.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.nooblol.smartnoob.feature.smart.config.ui.common.dialogs.showCopyEventWithToggleEventFromAnotherScenarioDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class EventCopyDialog(
    private val requestTriggerEvents: Boolean,
    private val onEventSelected: (Event) -> Unit,
) : CopyDialog(R.style.ScenarioConfigTheme) {

    /** View model for this content. */
    private val viewModel: EventCopyModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { eventCopyModel() },
    )
    /** Adapter displaying the list of events. */
    private lateinit var eventCopyAdapter: EventCopyAdapter

    override val titleRes: Int = R.string.dialog_overlay_title_copy_from
    override val searchHintRes: Int = R.string.search_view_hint_event_copy
    override val emptyRes: Int = R.string.message_empty_copy

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewModel.setCopyListType(requestTriggerEvents)
        eventCopyAdapter = EventCopyAdapter { event ->
            debounceUserInteraction { onEventClicked(event) }
        }

        viewBinding.layoutLoadableList.list.apply {
            addItemDecoration(newDividerWithoutHeader(context))
            adapter = eventCopyAdapter
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventList.collect(::updateEventList)
            }
        }
    }

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    private fun onEventClicked(event: Event) {
        if (viewModel.eventCopyShouldWarnUser(event)) {
            context.showCopyEventWithToggleEventFromAnotherScenarioDialog {
                notifySelectionAndDestroy(event)
            }
        } else {
            notifySelectionAndDestroy(event)
        }
    }

    private fun updateEventList(newItems: List<EventCopyModel.EventCopyItem>?) {
        viewBinding.layoutLoadableList.updateState(newItems)
        eventCopyAdapter.submitList(newItems)
    }

    private fun notifySelectionAndDestroy(event: Event) {
        back()
        onEventSelected(event)
    }
}
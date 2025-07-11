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
package com.nooblol.smartnoob.feature.smart.config.ui.scenario.triggerevents

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import com.nooblol.smartnoob.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.nooblol.smartnoob.core.common.overlays.dialog.implementation.navbar.viewModels
import com.nooblol.smartnoob.core.ui.bindings.lists.setEmptyText
import com.nooblol.smartnoob.core.ui.bindings.lists.updateState
import com.nooblol.smartnoob.core.ui.databinding.IncludeLoadableListBinding
import com.nooblol.smartnoob.core.domain.model.event.TriggerEvent
import com.nooblol.smartnoob.feature.smart.config.R
import com.nooblol.smartnoob.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.nooblol.smartnoob.feature.smart.config.ui.event.EventDialog
import com.nooblol.smartnoob.feature.smart.config.ui.event.copy.EventCopyDialog
import com.nooblol.smartnoob.feature.smart.config.ui.common.model.event.UiTriggerEvent

import kotlinx.coroutines.launch

class TriggerEventListContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: TriggerEventListViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { triggerEventListViewModel() },
    )

    /** View binding for all views in this content. */
    private lateinit var viewBinding: IncludeLoadableListBinding
    /** Adapter for the list of events. */
    private lateinit var eventAdapter: TriggerEventListAdapter

    override fun createCopyButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        eventAdapter = TriggerEventListAdapter(
            itemClickedListener = ::onTriggerEventItemClicked,
        )

        viewBinding = IncludeLoadableListBinding.inflate(LayoutInflater.from(context), container, false).apply {
            setEmptyText(
                id = R.string.message_empty_trigger_event_list_title,
                secondaryId = R.string.message_empty_trigger_event_list_desc,
            )
            list.apply {
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                adapter = eventAdapter
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.copyButtonIsVisible.collect(::updateCopyButtonVisibility) }
                launch { viewModel.triggerEvents.collect(::updateTriggerEventList) }
            }
        }
    }

    override fun onCreateButtonClicked() {
        debounceUserInteraction {
            showTriggerEventConfigDialog(viewModel.createNewEvent(context))
        }
    }

    override fun onCopyButtonClicked() {
        debounceUserInteraction {
            showTriggerEventCopyDialog()
        }
    }

    private fun onTriggerEventItemClicked(event: TriggerEvent) {
        debounceUserInteraction {
            showTriggerEventConfigDialog(event)
        }
    }

    private fun updateTriggerEventList(newItems: List<UiTriggerEvent>?) {
        viewBinding.updateState(newItems)
        eventAdapter.submitList(newItems)
    }

    private fun updateCopyButtonVisibility(isVisible: Boolean) {
        dialogController.createCopyButtons.buttonCopy.apply {
            if (isVisible) show() else hide()
        }
    }

    /** Opens the dialog allowing the user to copy an event. */
    private fun showTriggerEventCopyDialog() {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = EventCopyDialog(
                requestTriggerEvents = true,
                onEventSelected = { event ->
                    showTriggerEventConfigDialog(viewModel.createNewEvent(context, event as? TriggerEvent))
                },
            ),
        )
    }

    /** Opens the dialog allowing the user to add a new event. */
    private fun showTriggerEventConfigDialog(item: TriggerEvent) {
        viewModel.startEventEdition(item)

        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = EventDialog(
                onConfigComplete = viewModel::saveEventEdition,
                onDelete = viewModel::deleteEditedEvent,
                onDismiss = viewModel::dismissEditedEvent,
            ),
            hideCurrent = true,
        )
    }
}
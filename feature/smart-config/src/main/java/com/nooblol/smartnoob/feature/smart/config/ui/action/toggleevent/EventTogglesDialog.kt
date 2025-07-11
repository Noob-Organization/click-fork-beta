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
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.nooblol.smartnoob.core.domain.model.action.toggleevent.EventToggle
import com.nooblol.smartnoob.core.ui.bindings.dialogs.DialogNavigationButton
import com.nooblol.smartnoob.core.ui.bindings.lists.newDividerWithoutHeader
import com.nooblol.smartnoob.core.ui.bindings.lists.setEmptyText
import com.nooblol.smartnoob.core.ui.bindings.lists.updateState
import com.nooblol.smartnoob.core.common.overlays.base.viewModels
import com.nooblol.smartnoob.core.common.overlays.dialog.OverlayDialog
import com.nooblol.smartnoob.core.ui.bindings.dialogs.setButtonVisibility
import com.nooblol.smartnoob.feature.smart.config.R
import com.nooblol.smartnoob.feature.smart.config.databinding.DialogConfigEventsToggleBinding
import com.nooblol.smartnoob.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class EventTogglesDialog(
    private val onConfirmClicked: (List<EventToggle>) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: EventTogglesViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { eventTogglesViewModel() },
    )

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigEventsToggleBinding

    private lateinit var eventToggleAdapter: EventToggleAdapter

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogConfigEventsToggleBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_events_toggle)
                setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)

                buttonSave.setDebouncedOnClickListener {
                    onConfirmClicked(viewModel.getEditedEventToggleList())
                    back()
                }
                buttonDismiss.setDebouncedOnClickListener {
                    back()
                }
            }

            eventToggleAdapter = EventToggleAdapter(onEventToggleStateChanged = viewModel::changeEventToggleState)

            layoutLoadableList.apply {
                setEmptyText(R.string.message_empty_screen_event_title)

                list.apply {
                    addItemDecoration(newDividerWithoutHeader(context))
                    adapter = eventToggleAdapter
                }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentItems.collect(::updateToggleList) }
            }
        }
    }

    private fun updateToggleList(toggleList: List<EventTogglesListItem>) {
        viewBinding.layoutLoadableList.updateState(toggleList)
        eventToggleAdapter.submitList(toggleList)
    }
}
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
package com.nooblol.smartnoob.feature.smart.config.ui.action.copy

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.nooblol.smartnoob.core.ui.bindings.lists.updateState
import com.nooblol.smartnoob.core.domain.model.action.Action
import com.nooblol.smartnoob.core.ui.bindings.lists.newDividerWithoutHeader
import com.nooblol.smartnoob.core.common.overlays.base.viewModels
import com.nooblol.smartnoob.core.common.overlays.dialog.implementation.CopyDialog
import com.nooblol.smartnoob.feature.smart.config.R
import com.nooblol.smartnoob.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/**
 * [CopyDialog] implementation for displaying the whole list of actions for a copy.
 *
 * @param onActionSelected the listener called when the user select an Action.
 */
class ActionCopyDialog(
    private val onActionSelected: (Action) -> Unit,
) : CopyDialog(R.style.ScenarioConfigTheme) {

    /** View model for this content. */
    private val viewModel: ActionCopyModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { actionCopyViewModel() },
    )

    /** Adapter displaying the list of events. */
    private lateinit var actionCopyAdapter: ActionCopyAdapter

    override val titleRes: Int = R.string.dialog_overlay_title_copy_from
    override val searchHintRes: Int = R.string.search_view_hint_action_copy
    override val emptyRes: Int = R.string.message_empty_copy

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        actionCopyAdapter = ActionCopyAdapter { selectedAction ->
            debounceUserInteraction {
                back()
                onActionSelected(selectedAction.uiAction.action)
            }
        }

        viewBinding.layoutLoadableList.list.apply {
            addItemDecoration(newDividerWithoutHeader(context))
            adapter = actionCopyAdapter
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionList.collect(::updateActionList)
            }
        }
    }

    override fun onSearchQueryChanged(newText: String?) {
        viewModel.updateSearchQuery(newText)
    }

    private fun updateActionList(newList: List<ActionCopyModel.ActionCopyItem>?) {
        viewBinding.layoutLoadableList.updateState(newList)
        actionCopyAdapter.submitList(if (newList == null) ArrayList() else ArrayList(newList))
    }
}
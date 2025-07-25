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
package com.nooblol.smartnoob.feature.smart.config.ui.action.intent.flags

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import com.nooblol.smartnoob.core.ui.bindings.dialogs.DialogNavigationButton
import com.nooblol.smartnoob.core.common.overlays.base.viewModels
import com.nooblol.smartnoob.core.common.overlays.dialog.OverlayDialog
import com.nooblol.smartnoob.core.ui.bindings.dialogs.setButtonVisibility
import com.nooblol.smartnoob.feature.smart.config.R
import com.nooblol.smartnoob.feature.smart.config.databinding.DialogConfigActionIntentFlagsBinding
import com.nooblol.smartnoob.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.nooblol.smartnoob.feature.smart.config.ui.common.starters.newWebBrowserStarterOverlay

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class FlagsSelectionDialog (
    private val currentFlags: Int,
    private val startActivityFlags: Boolean,
    private val onConfigComplete: (flags: Int) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: FlagsSelectionViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { flagsSelectionViewModel() },
    )

    private lateinit var viewBinding: DialogConfigActionIntentFlagsBinding
    private lateinit var flagsAdapter: FlagsSelectionAdapter

    override fun onCreateView(): ViewGroup {
        flagsAdapter = FlagsSelectionAdapter(
            onFlagCheckClicked = viewModel::setFlagState,
            onFlagHelpClicked = { uri -> debounceUserInteraction { onFlagHelpClicked(uri) } },
        )

        viewBinding = DialogConfigActionIntentFlagsBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_title_intent_flags)

                setButtonVisibility(DialogNavigationButton.SAVE, View.GONE)
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                buttonDismiss.setDebouncedOnClickListener {
                    onConfigComplete(viewModel.getSelectedFlags())
                    back()
                }
            }

            flagsList.apply {
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                adapter = flagsAdapter
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        viewModel.setSelectedFlags(currentFlags, startActivityFlags)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.flagsItems.collect(flagsAdapter::submitList) }
            }
        }
    }

    private fun onFlagHelpClicked(uri: Uri) {
        overlayManager.navigateTo(
            context = context,
            newOverlay = newWebBrowserStarterOverlay(uri),
            hideCurrent = true,
        )
    }
}

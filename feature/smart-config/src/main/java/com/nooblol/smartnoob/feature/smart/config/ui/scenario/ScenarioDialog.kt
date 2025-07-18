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
package com.nooblol.smartnoob.feature.smart.config.ui.scenario

import android.util.Log
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.nooblol.smartnoob.core.ui.bindings.dialogs.DialogNavigationButton
import com.nooblol.smartnoob.core.ui.bindings.dialogs.setButtonEnabledState
import com.nooblol.smartnoob.core.common.overlays.base.viewModels
import com.nooblol.smartnoob.core.common.overlays.dialog.implementation.navbar.NavBarDialog
import com.nooblol.smartnoob.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.nooblol.smartnoob.core.ui.bindings.dialogs.setButtonVisibility
import com.nooblol.smartnoob.feature.smart.config.R
import com.nooblol.smartnoob.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.nooblol.smartnoob.feature.smart.config.ui.common.dialogs.showCloseWithoutSavingDialog
import com.nooblol.smartnoob.feature.smart.config.ui.scenario.config.ScenarioConfigContent
import com.nooblol.smartnoob.feature.smart.config.ui.scenario.imageevents.ImageEventListContent
import com.nooblol.smartnoob.feature.smart.config.ui.scenario.more.MoreContent
import com.nooblol.smartnoob.feature.smart.config.ui.scenario.triggerevents.TriggerEventListContent

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigation.NavigationBarView

import kotlinx.coroutines.launch

class ScenarioDialog(
    private val onConfigSaved: () -> Unit,
    private val onConfigDiscarded: () -> Unit,
) : NavBarDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: ScenarioDialogViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { scenarioDialogViewModel() },
    )

    override fun onCreateView(): ViewGroup {
        return super.onCreateView().also {
            topBarBinding.setButtonVisibility(DialogNavigationButton.SAVE, View.VISIBLE)
            topBarBinding.dialogTitle.setText(R.string.dialog_title_scenario_config)
        }
    }

    override fun inflateMenu(navBarView: NavigationBarView) {
        navBarView.inflateMenu(R.menu.menu_scenario_config)
    }

    override fun onCreateContent(navItemId: Int): NavBarDialogContent = when (navItemId) {
        R.id.page_image_events -> ImageEventListContent(context.applicationContext)
        R.id.page_trigger_events -> TriggerEventListContent(context.applicationContext)
        R.id.page_config -> ScenarioConfigContent(context.applicationContext)
        R.id.page_more -> MoreContent(context.applicationContext)
        else -> throw IllegalArgumentException("Unknown menu id $navItemId")
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        super.onDialogCreated(dialog)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch { viewModel.isEditingScenario.collect(::onScenarioEditingStateChanged) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.navItemsValidity.collect(::updateContentsValidity) }
                launch { viewModel.scenarioCanBeSaved.collect(::updateSaveButtonState) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.monitorViews(
            createEventButton = createCopyButtons.buttonNew,
            saveButton = topBarBinding.buttonSave,
        )
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopViewMonitoring()
    }

    override fun onDialogButtonPressed(buttonType: DialogNavigationButton) {
        when (buttonType) {
            DialogNavigationButton.SAVE -> {
                onConfigSaved()
                super.back()
            }

            DialogNavigationButton.DISMISS -> {
                back()
                return
            }

            DialogNavigationButton.DELETE -> Unit
        }
    }

    override fun back() {
        if (viewModel.hasUnsavedModifications()) {
            context.showCloseWithoutSavingDialog {
                onConfigDiscarded()
                super.back()
            }
            return
        }

        onConfigDiscarded()
        super.back()
    }

    private fun updateContentsValidity(itemsValidity: Map<Int, Boolean>) {
        itemsValidity.forEach { (itemId, isValid) ->
            setMissingInputBadge(itemId, !isValid)
        }
    }

    private fun updateSaveButtonState(isEnabled: Boolean) {
        topBarBinding.setButtonEnabledState(DialogNavigationButton.SAVE, isEnabled)
    }

    private fun onScenarioEditingStateChanged(isEditingScenario: Boolean) {
        if (!isEditingScenario) {
            Log.e(TAG, "Closing ScenarioDialog because there is no scenario edited")
            finish()
        }
    }
}

private const val TAG = "ScenarioDialog"
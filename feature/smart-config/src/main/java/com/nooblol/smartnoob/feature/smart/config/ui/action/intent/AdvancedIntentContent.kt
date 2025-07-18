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
package com.nooblol.smartnoob.feature.smart.config.ui.action.intent

import android.content.Context
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nooblol.smartnoob.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.nooblol.smartnoob.core.common.overlays.dialog.implementation.navbar.dialogViewModels

import com.nooblol.smartnoob.core.domain.model.action.intent.IntentExtra
import com.nooblol.smartnoob.core.ui.bindings.dropdown.DropdownItem
import com.nooblol.smartnoob.core.ui.bindings.dropdown.setItems
import com.nooblol.smartnoob.core.ui.bindings.dropdown.setSelectedItem
import com.nooblol.smartnoob.core.ui.bindings.fields.setButtonVisibility
import com.nooblol.smartnoob.core.ui.bindings.fields.setLabel
import com.nooblol.smartnoob.core.ui.bindings.fields.setOnTextChangedListener
import com.nooblol.smartnoob.core.ui.bindings.fields.setText
import com.nooblol.smartnoob.core.ui.bindings.fields.setError
import com.nooblol.smartnoob.core.ui.bindings.fields.setNumericValue
import com.nooblol.smartnoob.core.ui.bindings.fields.setOnCheckboxClickedListener
import com.nooblol.smartnoob.core.ui.bindings.fields.setTextValue
import com.nooblol.smartnoob.core.ui.bindings.fields.setup
import com.nooblol.smartnoob.feature.smart.config.R
import com.nooblol.smartnoob.feature.smart.config.databinding.ContentIntentConfigAdvancedBinding
import com.nooblol.smartnoob.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.nooblol.smartnoob.feature.smart.config.ui.common.dialogs.intent.IntentActionsSelectionDialog
import com.nooblol.smartnoob.feature.smart.config.ui.action.intent.component.ComponentSelectionDialog
import com.nooblol.smartnoob.feature.smart.config.ui.action.intent.extras.ExtraConfigDialog
import com.nooblol.smartnoob.feature.smart.config.ui.action.intent.flags.FlagsSelectionDialog

import kotlinx.coroutines.launch

class AdvancedIntentContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for the container dialog. */
    private val dialogViewModel: IntentViewModel by dialogViewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { intentViewModel() },
    )

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentIntentConfigAdvancedBinding
    /** The adapter for the list of extras in advanced configuration mode. */
    private lateinit var extrasAdapter: ExtrasAdapter

    override fun onCreateView(container: ViewGroup): ViewGroup {
        extrasAdapter = ExtrasAdapter(
            addExtraClickedListener = { debounceUserInteraction { showExtraDialog(dialogViewModel.createNewExtra()) } },
            extraClickedListener = { extra -> debounceUserInteraction { showExtraDialog(extra) } },
        )

        viewBinding = ContentIntentConfigAdvancedBinding.inflate(LayoutInflater.from(context)).apply {
            fieldName.apply {
                setLabel(R.string.generic_name)
                setOnTextChangedListener { dialogViewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            dialogController.hideSoftInputOnFocusLoss(fieldName.textField)

            intentSendingTypeField.setItems(
                label = context.getString(R.string.dropdown_intent_sending_type_label),
                items = dialogViewModel.sendingTypeItems,
                onItemSelected = dialogViewModel::setSendingType,
            )

            editActionLayout.apply {
                setup(R.string.field_intent_action_label, R.drawable.ic_search, disableInputWithCheckbox = false)
                setOnTextChangedListener { dialogViewModel.setIntentAction(it.toString()) }
                setOnCheckboxClickedListener { showActionsDialog() }
            }
            dialogController.hideSoftInputOnFocusLoss(editActionLayout.textField)

            editFlagsLayout.apply {
                setup(R.string.field_intent_flags_label, R.drawable.ic_search, disableInputWithCheckbox = false)
                setOnTextChangedListener {
                    dialogViewModel.setIntentFlags(
                        try { if (it.isNotEmpty()) it.toString().toInt() else null }
                        catch (nfe: NumberFormatException) { null }
                    )
                }
                setOnCheckboxClickedListener { showFlagsDialog() }
            }
            dialogController.hideSoftInputOnFocusLoss(editFlagsLayout.textField)

            editComponentNameLayout.apply {
                setup(R.string.field_intent_component_name_label, R.drawable.ic_search, disableInputWithCheckbox = false)
                setOnTextChangedListener { dialogViewModel.setComponentName(it.toString()) }
                setOnCheckboxClickedListener { showComponentNameDialog() }
            }
            dialogController.hideSoftInputOnFocusLoss(editComponentNameLayout.textField)

            extrasList.adapter = extrasAdapter
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { dialogViewModel.name.collect(viewBinding.fieldName::setText) }
                launch { dialogViewModel.nameError.collect(viewBinding.fieldName::setError)}
                launch { dialogViewModel.sendingType.collect(::updateSendingType) }
                launch { dialogViewModel.action.collect(viewBinding.editActionLayout::setTextValue) }
                launch { dialogViewModel.actionError.collect(viewBinding.editActionLayout::setError) }
                launch { dialogViewModel.flags.collect(viewBinding.editFlagsLayout::setNumericValue) }
                launch { dialogViewModel.componentName.collect(viewBinding.editComponentNameLayout::setTextValue) }
                launch { dialogViewModel.componentNameError.collect(viewBinding.editComponentNameLayout::setError) }
                launch { dialogViewModel.extras.collect(extrasAdapter::submitList) }
            }
        }
    }

    private fun updateSendingType(type: DropdownItem) {
        viewBinding.intentSendingTypeField.setSelectedItem(type)

        when (type) {
            dialogViewModel.sendingTypeActivity -> {
                viewBinding.editComponentNameLayout.setButtonVisibility(true)
                viewBinding.editActionLayout.setButtonVisibility(true)
            }
            dialogViewModel.sendingTypeBroadcast -> {
                viewBinding.editComponentNameLayout.setButtonVisibility(false)
                viewBinding.editActionLayout.setButtonVisibility(false)
            }
        }
    }

    private fun showActionsDialog() {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = IntentActionsSelectionDialog(
                currentAction = dialogViewModel.getConfiguredIntentAction(),
                onConfigComplete = { newAction ->
                    dialogViewModel.setIntentAction(newAction ?: "")
                    viewBinding.editActionLayout.textField.setText(newAction)
                },
            ),
            hideCurrent = true,
        )
    }

    private fun showFlagsDialog() {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = FlagsSelectionDialog(
                currentFlags = dialogViewModel.getConfiguredIntentFlags(),
                startActivityFlags = !dialogViewModel.isConfiguredIntentBroadcast(),
                onConfigComplete = { newFlags ->
                    dialogViewModel.setIntentFlags(newFlags)
                    viewBinding.editFlagsLayout.setNumericValue(newFlags.toString())
                },
            ),
            hideCurrent = true,
        )
    }

    private fun showComponentNameDialog() {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = ComponentSelectionDialog(
                onApplicationSelected = { newCompName ->
                    dialogViewModel.setComponentName(newCompName)
                    viewBinding.editComponentNameLayout.setTextValue(newCompName.flattenToString())
                },
            ),
            hideCurrent = true,
        )
    }

    private fun showExtraDialog(extra: IntentExtra<out Any>) {
        dialogViewModel.startIntentExtraEdition(extra)

        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = ExtraConfigDialog(
                onConfigComplete = dialogViewModel::saveIntentExtraEdition,
                onDeleteClicked = dialogViewModel::deleteIntentExtraEvent,
                onDismissClicked = dialogViewModel::dismissIntentExtraEvent,
            ),
            hideCurrent = false,
        )
    }
}
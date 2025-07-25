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

import android.content.ComponentName
import android.content.Context
import android.content.Intent as AndroidIntent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.nooblol.smartnoob.core.android.application.AndroidApplicationInfo
import com.nooblol.smartnoob.core.android.application.getAndroidApplicationInfo
import com.nooblol.smartnoob.core.domain.model.action.Intent
import com.nooblol.smartnoob.core.domain.model.action.intent.IntentExtra
import com.nooblol.smartnoob.core.ui.bindings.dropdown.DropdownItem
import com.nooblol.smartnoob.feature.smart.config.R
import com.nooblol.smartnoob.feature.smart.config.domain.EditionRepository
import com.nooblol.smartnoob.feature.smart.config.utils.getEventConfigPreferences
import com.nooblol.smartnoob.feature.smart.config.utils.putIntentIsAdvancedConfig

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@OptIn(FlowPreview::class)
class IntentViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    /** The action being configured by the user. */
    private val configuredIntent = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<Intent>()

    private val editedActionHasChanged: StateFlow<Boolean> =
        editionRepository.editionState.editedActionState
            .map { it.hasChanged }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Event configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()
    /** The Android package manager. */
    private val packageManager: PackageManager = context.packageManager

    /** Tells if the user is currently editing an action. If that's not the case, dialog should be closed. */
    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    /** The name of the pause. */
    val name: Flow<String?> = configuredIntent
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredIntent.map { it.name?.isEmpty() ?: true }

    /** The intent action. */
    val action: Flow<String?> = configuredIntent
        .map { it.intentAction }
        .take(1)
    /** Tells if the intent action is valid or not. */
    val actionError: Flow<Boolean> = configuredIntent.map { it.intentAction?.isEmpty() ?: true }

    /** The flags for this intent. */
    val flags: Flow<String> = configuredIntent
        .map { it.flags?.toString() ?: "0" }
        .take(1)

    /** The component name for the intent. */
    val componentName: Flow<String?> = configuredIntent
        .map { it.componentName?.flattenToString() }
        .take(1)
    /** Tells if the intent component name is valid or not. */
    val componentNameError: Flow<Boolean> = configuredIntent.map { intent ->
        !intent.isBroadcast && intent.componentName == null
    }

    val sendingTypeActivity = DropdownItem(title = R.string.dropdown_intent_sending_type_item_activity)
    val sendingTypeBroadcast = DropdownItem(title = R.string.dropdown_intent_sending_type_item_broadcast)
    /** Sending types choices for the dropdown field. */
    val sendingTypeItems = listOf(sendingTypeActivity, sendingTypeBroadcast)
    /** Current choice for the sending type dropdown field. */
    val sendingType: Flow<DropdownItem> = configuredIntent
        .map {
            when (it.isBroadcast) {
                true -> sendingTypeBroadcast
                false -> sendingTypeActivity
            }
        }
        .filterNotNull()

    /** The list of extra items to be displayed. */
    val extras: Flow<List<ExtraListItem>> = editionRepository.editionState.editedActionIntentExtrasState
        .map { intentExtra ->
            buildList {
                intentExtra.value?.forEach { extra ->
                    val lastDotIndex = extra.key!!.lastIndexOf('.', 0)
                    add(
                        ExtraListItem.ExtraItem(
                            extra = extra,
                            name = if (lastDotIndex == -1) extra.key!! else extra.key!!.substring(lastDotIndex),
                            value = extra.value.toString()
                        )
                    )
                }

                add(ExtraListItem.AddExtraItem)
            }
        }

    /** Name and icon of the selected application in simple edition mode. */
    val activityInfo: Flow<AndroidApplicationInfo?> = configuredIntent
        .filter { it.isAdvanced == false }
        .map { intent ->
            if (intent.componentName == null) return@map null

            packageManager.getAndroidApplicationInfo(
                AndroidIntent(intent.intentAction).setComponent(intent.componentName!!)
            )
        }

    /** Tells if the configured intent is valid and can be saved. */
    val isValidAction: Flow<Boolean> = editionRepository.editionState.editedActionState
        .map { it.canBeSaved }


    fun hasUnsavedModifications(): Boolean =
        editedActionHasChanged.value

    fun isAdvanced(): Boolean = editionRepository.editionState.getEditedAction<Intent>()?.isAdvanced ?: false

    /**
     * Set the name of the intent.
     * @param name the new name.
     */
    fun setName(name: String) {
        editionRepository.editionState.getEditedAction<Intent>()?.let { intent ->
            editionRepository.updateEditedAction(intent.copy(name = "" + name))
        }
    }

    /** Set the configuration mode. */
    fun setIsAdvancedConfiguration(isAdvanced: Boolean) {
        editionRepository.editionState.getEditedAction<Intent>()?.let { intent ->
            editionRepository.updateEditedAction(
                intent.copy(
                    isAdvanced = isAdvanced,
                    isBroadcast = if(!isAdvanced) false else intent.isBroadcast
                )
            )
        }
    }

    /**
     * Set the activity selected by the user in simple mode.
     * This will change the component name, but also all other parameters required for a default start activity.
     *
     * @param componentName component name of the selected activity.
     */
    fun setActivitySelected(componentName: ComponentName) {
        editionRepository.editionState.getEditedAction<Intent>()?.let { intent ->
            editionRepository.updateEditedAction(
                intent.copy(
                    isBroadcast = false,
                    intentAction = AndroidIntent.ACTION_MAIN,
                    flags = AndroidIntent.FLAG_ACTIVITY_NEW_TASK or AndroidIntent.FLAG_ACTIVITY_CLEAR_TOP,
                    componentName = componentName,
                )
            )
        }
    }

    /** Set the action for the intent. */
    fun setIntentAction(action: String) {
        editionRepository.editionState.getEditedAction<Intent>()?.let { intent ->
            editionRepository.updateEditedAction(intent.copy(intentAction = action))
        }
    }

    /** Set the action for the intent. */
    fun setIntentFlags(flags: Int?) {
        editionRepository.editionState.getEditedAction<Intent>()?.let { intent ->
            editionRepository.updateEditedAction(intent.copy(flags = flags))
        }
    }

    /** Set the component name for the intent. */
    fun setComponentName(componentName: String) {
        editionRepository.editionState.getEditedAction<Intent>()?.let { intent ->
            editionRepository.updateEditedAction(
                intent.copy(componentName = ComponentName.unflattenFromString(componentName))
            )
        }
    }

    /** Set the component name for the intent. */
    fun setComponentName(componentName: ComponentName) {
        editionRepository.editionState.getEditedAction<Intent>()?.let { intent ->
            editionRepository.updateEditedAction(
                intent.copy(componentName = componentName.clone())
            )
        }
    }

    /** Set the sending type. of the intent */
    fun setSendingType(newType: DropdownItem) {
        editionRepository.editionState.getEditedAction<Intent>()?.let { intent ->
            val isBroadcast = when (newType) {
                sendingTypeBroadcast -> true
                sendingTypeActivity -> false
                else -> return
            }

            editionRepository.updateEditedAction(intent.copy(isBroadcast = isBroadcast))
        }
    }

    fun getConfiguredIntentAction(): String? =
        editionRepository.editionState.getEditedAction<Intent>()?.intentAction

    fun getConfiguredIntentFlags(): Int =
        editionRepository.editionState.getEditedAction<Intent>()?.flags ?: 0

    fun isConfiguredIntentBroadcast(): Boolean =
        editionRepository.editionState.getEditedAction<Intent>()?.isBroadcast ?: false


    /** @return creates a new extra for this intent. */
    fun createNewExtra(): IntentExtra<Any> =
        editionRepository.editedItemsBuilder.createNewIntentExtra()

    /** Start the edition of an intent extra. */
    fun startIntentExtraEdition(extra: IntentExtra<out Any>) = editionRepository.startIntentExtraEdition(extra)

    /** Add or update an extra. If the extra id is unset, it will be added. If not, updated. */
    fun saveIntentExtraEdition() = editionRepository.upsertEditedIntentExtra()

    /** Delete an extra. */
    fun deleteIntentExtraEvent() = editionRepository.deleteEditedIntentExtra()

    /** Drop all changes made to the currently edited extra. */
    fun dismissIntentExtraEvent() = editionRepository.stopIntentExtraEdition()

    fun saveLastConfig() {
        editionRepository.editionState.getEditedAction<Intent>()?.let { intent ->
            sharedPreferences.edit().putIntentIsAdvancedConfig(intent.isAdvanced == true).apply()
        }
    }
}

/** Items displayed in the extra list. */
sealed class ExtraListItem {
    /** The add extra item. */
    data object AddExtraItem : ExtraListItem()
    /** Item representing an intent extra. */
    data class ExtraItem(val extra: IntentExtra<out Any>, val name: String, val value: String) : ExtraListItem()
}

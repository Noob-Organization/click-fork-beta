/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.nooblol.smartnoob.core.settings.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

import com.nooblol.smartnoob.core.base.PreferencesDataStore
import com.nooblol.smartnoob.core.base.di.Dispatcher
import com.nooblol.smartnoob.core.base.di.HiltCoroutineDispatchers.IO
import com.nooblol.smartnoob.core.base.workarounds.isImpactedByInputBlock

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class SettingsDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
) {

    internal companion object {
        const val PREFERENCES_FILE_NAME = "settings"

        val KEY_IS_FILTER_SCENARIO_UI_ENABLED: Preferences.Key<Boolean> =
            booleanPreferencesKey("isFilterScenarioUiEnabled")
        val KEY_IS_LEGACY_ACTION_UI: Preferences.Key<Boolean> =
            booleanPreferencesKey("isLegacyActionUiEnabled")
        val KEY_IS_LEGACY_NOTIFICATION_UI: Preferences.Key<Boolean> =
            booleanPreferencesKey("isLegacyNotificationUiEnabled")
        val KEY_FORCE_ENTIRE_SCREEN: Preferences.Key<Boolean> =
            booleanPreferencesKey("forceEntireScreen")
        val KEY_INPUT_BLOCK_WORKAROUND: Preferences.Key<Boolean> =
            booleanPreferencesKey("inputBlockWorkaround")
    }

    private val dataStore: PreferencesDataStore =
        PreferencesDataStore(
            context = context,
            dispatcher = ioDispatcher,
            fileName = PREFERENCES_FILE_NAME,
            migrations = listOf(LegacySettingsMigration(context, ioDispatcher))
        )

    internal fun isFilterScenarioUiEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_FILTER_SCENARIO_UI_ENABLED] ?: true }

    internal suspend fun toggleFilterScenarioUi() =
        dataStore.edit { preferences ->
            preferences[KEY_IS_FILTER_SCENARIO_UI_ENABLED] = !(preferences[KEY_IS_FILTER_SCENARIO_UI_ENABLED] ?: true)
        }

    internal fun isLegacyActionUiEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_LEGACY_ACTION_UI] ?: false }

    internal suspend fun toggleLegacyActionUi() =
        dataStore.edit { preferences ->
            preferences[KEY_IS_LEGACY_ACTION_UI] = !(preferences[KEY_IS_LEGACY_ACTION_UI] ?: false)
        }

    internal fun isLegacyNotificationUiEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_IS_LEGACY_NOTIFICATION_UI] ?: false }

    internal suspend fun toggleLegacyNotificationUi() =
        dataStore.edit { preferences ->
            preferences[KEY_IS_LEGACY_NOTIFICATION_UI] = !(preferences[KEY_IS_LEGACY_NOTIFICATION_UI] ?: false)
        }

    internal fun isEntireScreenCaptureForced(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_FORCE_ENTIRE_SCREEN] ?: false }

    internal suspend fun toggleForceEntireScreenCapture() =
        dataStore.edit { preferences ->
            preferences[KEY_FORCE_ENTIRE_SCREEN] = !(preferences[KEY_FORCE_ENTIRE_SCREEN] ?: false)
        }

    internal fun isInputBlockWorkaroundEnabled(): Flow<Boolean> =
        dataStore.data.map { preferences -> preferences[KEY_INPUT_BLOCK_WORKAROUND] ?: false }

    internal suspend fun toggleInputBlockWorkaround() {
        if (!isImpactedByInputBlock()) return
        dataStore.edit { preferences ->
            preferences[KEY_INPUT_BLOCK_WORKAROUND] = !(preferences[KEY_INPUT_BLOCK_WORKAROUND] ?: false)
        }
    }
}
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
package com.nooblol.smartnoob.settings

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.nooblol.smartnoob.core.base.workarounds.isImpactedByInputBlock
import com.nooblol.smartnoob.core.common.quality.domain.QualityRepository
import com.nooblol.smartnoob.core.settings.SettingsRepository
import com.nooblol.smartnoob.feature.revenue.IRevenueRepository
import com.nooblol.smartnoob.feature.revenue.UserBillingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val qualityRepository: QualityRepository,
    private val revenueRepository: IRevenueRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val isScenarioFiltersUiEnabled: Flow<Boolean> =
        settingsRepository.isFilterScenarioUiEnabledFlow

    val isLegacyActionUiEnabled: Flow<Boolean> =
        settingsRepository.isLegacyActionUiEnabledFlow

    val isLegacyNotificationUiEnabled: Flow<Boolean> =
        settingsRepository.isLegacyNotificationUiEnabledFlow

    val isEntireScreenCaptureForced: Flow<Boolean> =
        settingsRepository.isEntireScreenCaptureForcedFlow

    val isInputWorkaroundEnabled: Flow<Boolean> =
        settingsRepository.isInputBlockWorkaroundEnabledFlow

    val shouldShowEntireScreenCapture: Flow<Boolean> =
        flowOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)

    val shouldShowPrivacySettings: Flow<Boolean> =
        revenueRepository.isPrivacySettingRequired

    val shouldShowPurchase: Flow<Boolean> =
        revenueRepository.userBillingState.map { billingState ->
            billingState != UserBillingState.PURCHASED
        }

    val shouldShowInputBlockWorkaround: Flow<Boolean> =
        flowOf(isImpactedByInputBlock())


    fun toggleScenarioFiltersUi() {
        settingsRepository.toggleFilterScenarioUi()
    }

    fun toggleLegacyActionUi() {
        settingsRepository.toggleLegacyActionUi()
    }

    fun toggleLegacyNotificationUi() {
        settingsRepository.toggleLegacyNotificationUi()
    }

    fun toggleForceEntireScreenCapture() {
        settingsRepository.toggleForceEntireScreenCapture()
    }

    fun toggleInputBlockWorkaround() {
        settingsRepository.toggleInputBlockWorkaround()
    }

    fun showPrivacySettings(activity: Activity) {
        revenueRepository.startPrivacySettingUiFlow(activity)
    }

    fun showPurchaseActivity(context: Context) {
        revenueRepository.startPurchaseUiFlow(context)
    }

    fun showTroubleshootingDialog(activity: FragmentActivity) {
        qualityRepository.startTroubleshootingUiFlow(activity)
    }
}
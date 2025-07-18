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
package com.nooblol.smartnoob.feature.revenue.ui.paywall

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel

import com.nooblol.smartnoob.core.ui.bindings.buttons.LoadableButtonState
import com.nooblol.smartnoob.feature.revenue.R
import com.nooblol.smartnoob.feature.revenue.domain.InternalRevenueRepository
import com.nooblol.smartnoob.feature.revenue.domain.TRIAL_SESSION_DURATION_DURATION
import com.nooblol.smartnoob.feature.revenue.domain.model.AdState
import com.nooblol.smartnoob.feature.revenue.domain.model.ProModeInfo
import com.nooblol.smartnoob.feature.revenue.domain.model.PurchaseState

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
internal class AdsLoadingViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val revenueRepository: InternalRevenueRepository,
) : ViewModel() {

    val dialogState: Flow<DialogState> = combine(
        revenueRepository.purchaseState,
        revenueRepository.adsState,
        revenueRepository.proModeInfo,
        revenueRepository.trialAvailable,
    ) { purchaseState, adsState, info, isTrialAvailable ->
        when {
            purchaseState == PurchaseState.PURCHASED -> DialogState.Purchased
            adsState == AdState.SHOWING -> DialogState.AdShowing
            adsState == AdState.VALIDATED -> DialogState.AdWatched
            else -> DialogState.NotPurchased(
                trialButtonState = getTrialButtonState(
                    appContext,
                    isTrialAvailable,
                    TRIAL_SESSION_DURATION_DURATION.inWholeMinutes.toInt(),
                ) ,
                adButtonState = adsState.toAdButtonState(appContext),
                purchaseButtonState = getPurchaseButtonState(appContext, purchaseState, info),
            )
        }
    }

    fun loadAdIfNeeded(context: Context) {
        revenueRepository.loadAdIfNeeded(context)
    }

    fun launchPlayStoreBillingFlow(activity: Activity) {
        revenueRepository.startPlayStoreBillingUiFlow(activity)
    }

    fun showAd(activity: Activity) {
        revenueRepository.showAd(activity)
    }

    fun requestTrial() {
        revenueRepository.requestTrial()
    }
}

internal sealed class DialogState {

    internal data class NotPurchased(
        val trialButtonState: LoadableButtonState,
        val adButtonState: LoadableButtonState,
        val purchaseButtonState: LoadableButtonState,
    ): DialogState()

    internal data object Purchased : DialogState()
    internal data object AdShowing : DialogState()
    internal data object AdWatched : DialogState()
}

private fun AdState.toAdButtonState(context: Context): LoadableButtonState = when (this) {
    AdState.INITIALIZED,
    AdState.LOADING -> LoadableButtonState.Loading(
        text = context.getString(R.string.button_text_watch_ad_loading)
    )

    AdState.ERROR,
    AdState.READY -> LoadableButtonState.Loaded.Enabled(
        text = context.getString(R.string.button_text_watch_ad)
    )

    AdState.SHOWING,
    AdState.VALIDATED -> LoadableButtonState.Loaded.Disabled(
        text = context.getString(R.string.button_text_watch_ad)
    )

    AdState.NOT_INITIALIZED -> LoadableButtonState.Loaded.Disabled(
        text = context.getString(R.string.button_text_watch_ad_error)
    )
}

private fun getPurchaseButtonState(context: Context, purchaseState: PurchaseState, info: ProModeInfo?): LoadableButtonState =
    when {
        info?.price.isNullOrEmpty() ->
            LoadableButtonState.Loading(context.getString(R.string.button_text_buy_pro_loading))

        purchaseState == PurchaseState.PENDING ->
            LoadableButtonState.Loaded.Disabled(context.getString(R.string.button_text_buy_pro_pending))

        purchaseState == PurchaseState.CANNOT_PURCHASE ->
            LoadableButtonState.Loaded.Disabled(context.getString(R.string.button_text_buy_pro_error))

        else ->
            LoadableButtonState.Loaded.Enabled(context.getString(R.string.button_text_buy_pro, info?.price))
    }

private fun getTrialButtonState(context: Context, trialAvailable: Boolean, trialDurationMinutes: Int) =
    if (trialAvailable) {
        LoadableButtonState.Loaded.Enabled(context.getString(R.string.button_text_trial, trialDurationMinutes))
    } else {
        LoadableButtonState.Loaded.Disabled(context.getString(R.string.button_text_trial, trialDurationMinutes))
    }

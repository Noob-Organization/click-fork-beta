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
package com.nooblol.smartnoob.feature.revenue.domain

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting

import com.nooblol.smartnoob.core.base.addDumpTabulationLvl
import com.nooblol.smartnoob.core.base.di.Dispatcher
import com.nooblol.smartnoob.core.base.di.HiltCoroutineDispatchers
import com.nooblol.smartnoob.core.base.dumpWithTimeout
import com.nooblol.smartnoob.core.common.quality.domain.Quality
import com.nooblol.smartnoob.core.common.quality.domain.QualityRepository
import com.nooblol.smartnoob.feature.revenue.UserBillingState
import com.nooblol.smartnoob.feature.revenue.UserConsentState
import com.nooblol.smartnoob.feature.revenue.data.ads.InterstitialAdsDataSource
import com.nooblol.smartnoob.feature.revenue.data.ads.RemoteAdState
import com.nooblol.smartnoob.feature.revenue.data.UserConsentDataSource
import com.nooblol.smartnoob.feature.revenue.data.billing.InAppPurchaseState
import com.nooblol.smartnoob.feature.revenue.data.billing.BillingDataSource
import com.nooblol.smartnoob.feature.revenue.data.billing.sdk.InAppProduct
import com.nooblol.smartnoob.feature.revenue.domain.model.AdState
import com.nooblol.smartnoob.feature.revenue.domain.model.ProModeInfo
import com.nooblol.smartnoob.feature.revenue.domain.model.PurchaseState
import com.nooblol.smartnoob.feature.revenue.ui.BillingActivity
import com.nooblol.smartnoob.feature.revenue.ui.paywall.PaywallFragment
import com.nooblol.smartnoob.feature.revenue.ui.purchase.PurchaseProModeFragment

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


@Singleton
internal class RevenueRepository @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(HiltCoroutineDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    private val userConsentDataSource: UserConsentDataSource,
    private val adsDataSource: InterstitialAdsDataSource,
    private val billingDataSource: BillingDataSource,
    qualityRepository: QualityRepository,
): InternalRevenueRepository {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    /** Reset the ad watched state after a while */
    private var resetAdJob: Job? = null

    private val trialRequest: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    private val trialRequestCount: MutableStateFlow<Int> =
        MutableStateFlow(0)

    override val proModeInfo: Flow<ProModeInfo?> =
        billingDataSource.product.map(::toProModeInfo)

    override val userConsentState: Flow<UserConsentState> =
        combine(
            billingDataSource.purchaseState,
            userConsentDataSource.isInitialized,
            userConsentDataSource.isUserConsentingForAds,
            ::toUserConsentState,
        )

    override val isPrivacySettingRequired: Flow<Boolean> =
        userConsentDataSource.isPrivacyOptionsRequired

    override val adsState: StateFlow<AdState> =
        adsDataSource.remoteAdState.map(::toAdState)
            .stateIn(coroutineScopeIo, SharingStarted.Eagerly, AdState.NOT_INITIALIZED)

    override val purchaseState: StateFlow<PurchaseState> =
        combine(
            billingDataSource.purchaseState,
            billingDataSource.product,
            ::toPurchaseState,
        ).stateIn(coroutineScopeIo, SharingStarted.Eagerly, PurchaseState.CANNOT_PURCHASE)

    override val trialAvailable: Flow<Boolean> =
        trialRequestCount.map { requestCount -> requestCount < MAX_TRIAL_COUNT }

    override val userBillingState: StateFlow<UserBillingState> = combine(
        adsState,
        purchaseState,
        trialRequest,
        qualityRepository.quality,
        ::toUserBillingState,
    ).stateIn(coroutineScopeIo, SharingStarted.Eagerly, UserBillingState.AD_REQUESTED)

    override val isBillingFlowInProgress: MutableStateFlow<Boolean> =
        MutableStateFlow(false)


    init {
        // Once the user has given his consent, initialize the ads sdk
        initAdsOnConsentFlow(context, userConsentDataSource.isUserConsentingForAds, adsState)
            .launchIn(coroutineScopeIo)

        // Some user state are temporary, monitor that and act accordingly
        userStateInvalidatorFlow(userBillingState)
            .launchIn(coroutineScopeIo)
    }


    override fun startUserConsentRequestUiFlowIfNeeded(activity: Activity) {
        if (userBillingState.value == UserBillingState.PURCHASED) return
        userConsentDataSource.requestUserConsent(activity)
    }

    override fun startPrivacySettingUiFlow(activity: Activity) {
        userConsentDataSource.showPrivacyOptionsForm(activity)
    }

    override fun loadAdIfNeeded(context: Context) {
        if (userBillingState.value != UserBillingState.AD_REQUESTED) return
        adsDataSource.loadAd(context)
    }

    override fun startPaywallUiFlow(context: Context) {
        isBillingFlowInProgress.value = true
        context.startActivity(BillingActivity.getStartIntent(context, PaywallFragment.FRAGMENT_TAG))
    }

    override fun startPurchaseUiFlow(context: Context) {
        isBillingFlowInProgress.value = true
        context.startActivity(BillingActivity.getStartIntent(context, PurchaseProModeFragment.FRAGMENT_TAG))
    }

    override fun setBillingActivityDestroyed() {
        isBillingFlowInProgress.value = false
    }

    override fun showAd(activity: Activity) {
        when (adsState.value) {
            AdState.READY -> adsDataSource.showAd(activity)
            AdState.ERROR -> adsDataSource.forceShown()
            else -> Unit
        }
    }

    override fun startPlayStoreBillingUiFlow(activity: Activity) {
        if (purchaseState.value != PurchaseState.NOT_PURCHASED) return
        billingDataSource.launchBillingFlow(activity)
    }

    override fun requestTrial() {
        Log.d(TAG, "User requesting trial period")
        trialRequest.value = true
    }

    override fun refreshPurchases() {
        coroutineScopeIo.launch {
            billingDataSource.refreshPurchases()
        }
    }

    override fun consumeTrial(): Duration? {
        if (!trialRequest.value) return null

        Log.d(TAG, "User consuming trial period")

        trialRequest.value = false
        trialRequestCount.value += 1
        return TRIAL_SESSION_DURATION_DURATION
    }


    private fun toProModeInfo(product: InAppProduct?): ProModeInfo? =
        product?.let { ProModeInfo(it.title, it.description, it.price) }

    private fun initAdsOnConsentFlow(context: Context, consent: Flow<Boolean>, adsState: Flow<AdState>) : Flow<Unit> =
        combine(consent, adsState) { isConsenting, state ->
            if (!isConsenting || state != AdState.NOT_INITIALIZED) return@combine

            Log.i(TAG, "User consenting for ads, initialize ads SDK")
            adsDataSource.initialize(context)
        }

    private fun userStateInvalidatorFlow(userBillingState: Flow<UserBillingState>) : Flow<Any> =
        userBillingState.onEach { state ->
            if (state != UserBillingState.AD_WATCHED) return@onEach

            resetAdJob?.cancel()
            resetAdJob = coroutineScopeIo.launch {
                Log.i(TAG, "Ad watched, starting grace delay")
                delay(AD_WATCHED_STATE_DURATION)
                Log.i(TAG, "Ad watched grace delay over, reset ad data source")

                adsDataSource.reset()
                resetAdJob = null
            }
        }


    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        super.dump(writer, prefix)
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(contentPrefix)
                .append("- userConsentState=${userConsentState.dumpWithTimeout()}; ")
                .append("adsState=${adsState.value}; ")
                .append("purchaseState=${purchaseState.value}; ")
                .println()
            append(contentPrefix)
                .append("- proModeInfo=${proModeInfo.dumpWithTimeout()}; ")
                .println()
        }
    }
}

private fun toUserConsentState(purchaseState: InAppPurchaseState, isConsentInit: Boolean, isConsenting: Boolean): UserConsentState =
    when {
        purchaseState == InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED -> UserConsentState.ADS_NOT_NEEDED
        isConsenting -> UserConsentState.CAN_REQUEST_ADS
        isConsentInit && !isConsenting -> UserConsentState.CANNOT_REQUEST_ADS
        else -> UserConsentState.UNKNOWN
    }

private fun toAdState(remoteAdState: RemoteAdState): AdState =
    when (remoteAdState) {
        RemoteAdState.SdkNotInitialized -> AdState.NOT_INITIALIZED
        RemoteAdState.Initialized -> AdState.INITIALIZED
        RemoteAdState.Loading -> AdState.LOADING
        is RemoteAdState.NotShown -> AdState.READY
        RemoteAdState.Showing -> AdState.SHOWING
        is RemoteAdState.Shown -> AdState.VALIDATED

        is RemoteAdState.Error.LoadingError,
        is RemoteAdState.Error.ShowError,
        RemoteAdState.Error.NoImpressionError -> AdState.ERROR
    }

private fun toPurchaseState(state: InAppPurchaseState, product: InAppProduct?): PurchaseState =
    when {
        state == InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED -> PurchaseState.PURCHASED
        state == InAppPurchaseState.PURCHASED -> PurchaseState.PENDING
        state == InAppPurchaseState.PENDING -> PurchaseState.PENDING
        state == InAppPurchaseState.NOT_PURCHASED && product != null -> PurchaseState.NOT_PURCHASED
        else -> PurchaseState.CANNOT_PURCHASE
    }

private fun toUserBillingState(adState: AdState, purchaseState: PurchaseState, trial: Boolean, quality: Quality): UserBillingState =
    when {
        purchaseState == PurchaseState.PURCHASED -> UserBillingState.PURCHASED
        quality != Quality.High -> UserBillingState.EXEMPTED
        adState == AdState.VALIDATED -> UserBillingState.AD_WATCHED
        trial -> UserBillingState.TRIAL
        else -> UserBillingState.AD_REQUESTED
    }

internal val TRIAL_SESSION_DURATION_DURATION = 30.minutes
@VisibleForTesting internal val AD_WATCHED_STATE_DURATION = 30.minutes

private const val MAX_TRIAL_COUNT = 3
private const val TAG = "RevenueRepository"
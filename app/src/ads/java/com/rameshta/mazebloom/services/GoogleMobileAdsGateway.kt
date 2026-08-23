package com.rameshta.mazebloom.services

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.ConsentInformation
import com.google.android.ump.UserMessagingPlatform
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Google Mobile Ads adapter. UMP consent is refreshed once per Activity session before the SDK is
 * initialized or any request is made. Every public operation fails open for gameplay.
 */
class GoogleMobileAdsGateway(
    private val activity: ComponentActivity,
    private val interstitialAdUnitId: String,
    private val rewardedAdUnitId: String,
) : AdsGateway, ConsentGateway, DefaultLifecycleObserver, AutoCloseable {
    override val enabled = interstitialAdUnitId.isNotBlank() && rewardedAdUnitId.isNotBlank()
    private val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
    override val privacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val presentationMutex = Mutex()
    private val interstitialLoadMutex = Mutex()
    private val rewardedLoadMutex = Mutex()
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private val readiness = CompletableDeferred<Boolean>()

    init {
        activity.lifecycle.addObserver(this)
        scope.launch {
            val ready = runCatching { gatherConsentAndInitialize() }.getOrDefault(false)
            Log.i(TAG, "Initialization ${if (ready) "ready" else "unavailable"}; consent=${consentInformation.consentStatus}")
            readiness.complete(ready)
            if (ready) {
                launch { ensureInterstitialLoaded() }
                launch { ensureRewardedLoaded() }
            }
        }
    }

    override suspend fun showInterstitial(): AdResult = presentExclusively {
        if (!awaitReady()) return@presentExclusively AdResult.UNAVAILABLE
        val ad = ensureInterstitialLoaded() ?: return@presentExclusively AdResult.UNAVAILABLE
        interstitialAd = null
        presentInterstitial(ad).also { scope.launch { ensureInterstitialLoaded() } }
    }

    override suspend fun showRewarded(onVerifiedReward: (String) -> Unit): AdResult = presentExclusively {
        if (!awaitReady()) return@presentExclusively AdResult.UNAVAILABLE
        val ad = ensureRewardedLoaded() ?: return@presentExclusively AdResult.UNAVAILABLE
        rewardedAd = null
        presentRewarded(ad, onVerifiedReward).also { scope.launch { ensureRewardedLoaded() } }
    }

    override suspend fun resolve(): ConsentState {
        awaitReady()
        return when (consentInformation.consentStatus) {
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> ConsentState.NOT_REQUIRED
            ConsentInformation.ConsentStatus.OBTAINED -> ConsentState.GRANTED
            ConsentInformation.ConsentStatus.REQUIRED -> ConsentState.DENIED
            else -> ConsentState.UNAVAILABLE
        }
    }

    override suspend fun openPrivacyOptions(): Boolean = withContext(Dispatchers.Main.immediate) {
        if (!privacyOptionsRequired || activity.isFinishing || activity.isDestroyed) {
            return@withContext false
        }
        suspendCancellableCoroutine { continuation ->
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
                if (continuation.isActive) continuation.resume(error == null)
            }
        }
    }

    private suspend fun presentExclusively(block: suspend () -> AdResult): AdResult =
        withContext(Dispatchers.Main.immediate) {
            if (!enabled || activity.isFinishing || activity.isDestroyed ||
                !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            ) return@withContext AdResult.UNAVAILABLE
            if (!presentationMutex.tryLock()) return@withContext AdResult.UNAVAILABLE
            try {
                block()
            } catch (_: Exception) {
                AdResult.FAILED
            } finally {
                presentationMutex.unlock()
            }
        }

    private suspend fun awaitReady(): Boolean =
        withTimeoutOrNull(INITIALIZATION_TIMEOUT_MS) { readiness.await() } == true

    private suspend fun gatherConsentAndInitialize(): Boolean {
        if (!enabled || !requestConsent()) return false
        return withTimeoutOrNull(INITIALIZATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                MobileAds.initialize(activity.applicationContext) {
                    if (continuation.isActive) continuation.resume(true)
                }
            }
        } == true
    }

    private suspend fun requestConsent(): Boolean = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            fun finish() {
                if (continuation.isActive) continuation.resume(consentInformation.canRequestAds())
            }
            consentInformation.requestConsentInfoUpdate(
                activity,
                ConsentRequestParameters.Builder().build(),
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { finish() }
                },
                { error ->
                    Log.w(TAG, "Consent info update failed (${error.errorCode}): ${error.message}")
                    finish()
                },
            )
        }
    }

    private suspend fun ensureInterstitialLoaded(): InterstitialAd? = interstitialLoadMutex.withLock {
        interstitialAd ?: withTimeoutOrNull(AD_LOAD_TIMEOUT_MS) { loadInterstitial() }
            ?.also { interstitialAd = it }
    }

    private suspend fun ensureRewardedLoaded(): RewardedAd? = rewardedLoadMutex.withLock {
        rewardedAd ?: withTimeoutOrNull(AD_LOAD_TIMEOUT_MS) { loadRewarded() }
            ?.also { rewardedAd = it }
    }

    private suspend fun loadInterstitial(): InterstitialAd? = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            InterstitialAd.load(
                activity.applicationContext,
                interstitialAdUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.d(TAG, "Interstitial loaded")
                        if (continuation.isActive) continuation.resume(ad)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Interstitial load failed (${error.code}/${error.domain}): ${error.message}")
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }
    }

    private suspend fun loadRewarded(): RewardedAd? = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            RewardedAd.load(
                activity.applicationContext,
                rewardedAdUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(TAG, "Rewarded loaded")
                        if (continuation.isActive) continuation.resume(ad)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Rewarded load failed (${error.code}/${error.domain}): ${error.message}")
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }
    }

    private suspend fun presentInterstitial(ad: InterstitialAd): AdResult =
        suspendCancellableCoroutine { continuation ->
            fun finish(result: AdResult) {
                if (continuation.isActive) continuation.resume(result)
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial dismissed")
                    finish(AdResult.SHOWN)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.w(TAG, "Interstitial show failed (${error.code}/${error.domain}): ${error.message}")
                    finish(AdResult.FAILED)
                }
            }
            runCatching { ad.show(activity) }.onFailure { finish(AdResult.FAILED) }
        }

    private suspend fun presentRewarded(
        ad: RewardedAd,
        onVerifiedReward: (String) -> Unit,
    ): AdResult = suspendCancellableCoroutine { continuation ->
        fun finish(result: AdResult) {
            if (continuation.isActive) continuation.resume(result)
        }
        var rewardDelivered = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded dismissed; rewardDelivered=$rewardDelivered")
                finish(AdResult.SHOWN)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded show failed (${error.code}/${error.domain}): ${error.message}")
                finish(AdResult.FAILED)
            }
        }
        runCatching {
            ad.show(activity) {
                if (!rewardDelivered) {
                    rewardDelivered = true
                    Log.d(TAG, "Reward callback verified")
                    onVerifiedReward("admob-reward-${UUID.randomUUID()}")
                }
            }
        }.onFailure { finish(AdResult.FAILED) }
    }

    override fun onDestroy(owner: LifecycleOwner) = close()

    override fun close() {
        activity.lifecycle.removeObserver(this)
        scope.cancel()
        if (!readiness.isCompleted) readiness.complete(false)
        interstitialAd = null
        rewardedAd = null
    }

    companion object {
        private const val TAG = "MazeBloomAds"
        private const val INITIALIZATION_TIMEOUT_MS = 35_000L
        private const val AD_LOAD_TIMEOUT_MS = 20_000L
    }
}

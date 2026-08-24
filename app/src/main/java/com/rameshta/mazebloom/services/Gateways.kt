package com.rameshta.mazebloom.services

enum class AdResult { SHOWN, UNAVAILABLE, FAILED }
enum class PurchaseResult { PURCHASED, PENDING, CANCELLED, UNAVAILABLE, FAILED }
enum class ConsentState { NOT_REQUIRED, GRANTED, DENIED, UNAVAILABLE }

interface AdsGateway {
    val enabled: Boolean
    suspend fun showAppOpen(): AdResult
    suspend fun showInterstitial(): AdResult
    suspend fun showRewarded(onVerifiedReward: (String) -> Unit): AdResult
}

object NoOpAdsGateway : AdsGateway {
    override val enabled = false
    override suspend fun showAppOpen() = AdResult.UNAVAILABLE
    override suspend fun showInterstitial() = AdResult.UNAVAILABLE
    override suspend fun showRewarded(onVerifiedReward: (String) -> Unit) = AdResult.UNAVAILABLE
}

class FakeAdsGateway(
    override val enabled: Boolean = true,
    var appOpenResult: AdResult = AdResult.SHOWN,
    var interstitialResult: AdResult = AdResult.SHOWN,
    var rewardedResult: AdResult = AdResult.SHOWN,
) : AdsGateway {
    private val grantedTransactions = mutableSetOf<String>()
    private var rewardSequence = 0
    override suspend fun showAppOpen() = appOpenResult
    override suspend fun showInterstitial() = interstitialResult
    override suspend fun showRewarded(onVerifiedReward: (String) -> Unit): AdResult {
        if (rewardedResult == AdResult.SHOWN) {
            val transaction = "fake-reward-${++rewardSequence}"
            if (grantedTransactions.add(transaction)) onVerifiedReward(transaction)
        }
        return rewardedResult
    }
}

object AppOpenAdPolicy {
    const val COOLDOWN_MS = 60L * 60L * 1_000L
    const val REQUIRED_AD_FREE_LAUNCHES = 3

    fun isCooldownElapsed(lastShownAtMs: Long, nowMs: Long): Boolean =
        lastShownAtMs <= 0L || (nowMs >= lastShownAtMs && nowMs - lastShownAtMs >= COOLDOWN_MS)

    fun isLaunchEligible(recordedLaunches: Int): Boolean =
        recordedLaunches > REQUIRED_AD_FREE_LAUNCHES
}

object RewardedToInterstitialPolicy {
    const val PROTECTION_MS = 60_000L

    fun isProtected(lastRewardCompletedAtMs: Long, nowMs: Long): Boolean =
        lastRewardCompletedAtMs > 0L &&
            (nowMs < lastRewardCompletedAtMs || nowMs - lastRewardCompletedAtMs < PROTECTION_MS)
}

interface BillingGateway {
    val configured: Boolean
    suspend fun purchaseRemoveAds(): PurchaseResult
    suspend fun restoreRemoveAds(): Boolean
}

object NoOpBillingGateway : BillingGateway {
    override val configured = false
    override suspend fun purchaseRemoveAds() = PurchaseResult.UNAVAILABLE
    override suspend fun restoreRemoveAds() = false
}

class FakeBillingGateway(var entitled: Boolean = false) : BillingGateway {
    override val configured = true
    override suspend fun purchaseRemoveAds(): PurchaseResult {
        entitled = true
        return PurchaseResult.PURCHASED
    }
    override suspend fun restoreRemoveAds() = entitled
}

interface ConsentGateway {
    val enabled: Boolean
    val privacyOptionsRequired: Boolean
    suspend fun resolve(): ConsentState
    suspend fun openPrivacyOptions(): Boolean
}

object NoOpConsentGateway : ConsentGateway {
    override val enabled = false
    override val privacyOptionsRequired = false
    override suspend fun resolve() = ConsentState.UNAVAILABLE
    override suspend fun openPrivacyOptions() = false
}

data class InterstitialContext(
    val successfulCompletion: Boolean,
    val completedLevels: Int,
    val removeAdsEntitled: Boolean,
    val leavingCompletedLevel: Boolean,
)

object InterstitialPolicy {
    fun isEligible(context: InterstitialContext): Boolean =
        context.successfulCompletion &&
            context.completedLevels > 0 &&
            context.completedLevels % 5 == 0 &&
            !context.removeAdsEntitled &&
            context.leavingCompletedLevel
}

enum class AnalyticsEvent {
    APP_OPENED, ONBOARDING_STEP, LEVEL_STARTED, LEVEL_COMPLETED, LEVEL_ABANDONED,
    LEVEL_RESTARTED, UNDO_USED, HINT_REQUESTED, HINT_SHOWN, DAILY_STARTED,
    DAILY_COMPLETED, REPLAY_STARTED, SHARE_REQUESTED, GENERATOR_FALLBACK_OR_ERROR,
    AD_REQUEST, AD_RESULT, PURCHASE_REQUEST, PURCHASE_RESULT, COMPANION_PURCHASED,
    COMPANION_SELECTED,
}

interface Analytics {
    fun record(event: AnalyticsEvent, parameters: Map<String, String> = emptyMap())
}

object NoOpAnalytics : Analytics {
    override fun record(event: AnalyticsEvent, parameters: Map<String, String>) = Unit
}

class DebugAnalytics(private val sink: (String) -> Unit) : Analytics {
    override fun record(event: AnalyticsEvent, parameters: Map<String, String>) {
        sink("MazeBloom analytics: ${event.name.lowercase()} $parameters")
    }
}

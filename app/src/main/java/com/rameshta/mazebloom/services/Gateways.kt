package com.rameshta.mazebloom.services

enum class AdResult { SHOWN, UNAVAILABLE, FAILED }
enum class PurchaseResult { PURCHASED, PENDING, CANCELLED, UNAVAILABLE, FAILED }
enum class ConsentState { NOT_REQUIRED, GRANTED, DENIED, UNAVAILABLE }

interface AdsGateway {
    val enabled: Boolean
    suspend fun showInterstitial(): AdResult
    suspend fun showRewarded(onVerifiedReward: (String) -> Unit): AdResult
}

object NoOpAdsGateway : AdsGateway {
    override val enabled = false
    override suspend fun showInterstitial() = AdResult.UNAVAILABLE
    override suspend fun showRewarded(onVerifiedReward: (String) -> Unit) = AdResult.UNAVAILABLE
}

class FakeAdsGateway(
    override val enabled: Boolean = true,
    var interstitialResult: AdResult = AdResult.SHOWN,
    var rewardedResult: AdResult = AdResult.SHOWN,
) : AdsGateway {
    private val grantedTransactions = mutableSetOf<String>()
    override suspend fun showInterstitial() = interstitialResult
    override suspend fun showRewarded(onVerifiedReward: (String) -> Unit): AdResult {
        if (rewardedResult == AdResult.SHOWN) {
            val transaction = "fake-reward-1"
            if (grantedTransactions.add(transaction)) onVerifiedReward(transaction)
        }
        return rewardedResult
    }
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
    suspend fun resolve(): ConsentState
    suspend fun openPrivacyOptions(): Boolean
}

object NoOpConsentGateway : ConsentGateway {
    override suspend fun resolve() = ConsentState.UNAVAILABLE
    override suspend fun openPrivacyOptions() = false
}

data class InterstitialContext(
    val firstSession: Boolean,
    val campaignOrder: Int,
    val successfulCompletion: Boolean,
    val completedEligibleLevelsSinceAd: Int,
    val secondsSinceAd: Long,
    val removeAdsEntitled: Boolean,
    val returningToGarden: Boolean,
)

object InterstitialPolicy {
    fun isEligible(context: InterstitialContext): Boolean =
        !context.firstSession &&
            context.campaignOrder > 5 &&
            context.successfulCompletion &&
            context.completedEligibleLevelsSinceAd >= 3 &&
            context.secondsSinceAd >= 180 &&
            !context.removeAdsEntitled &&
            context.returningToGarden
}

enum class AnalyticsEvent {
    APP_OPENED, ONBOARDING_STEP, LEVEL_STARTED, LEVEL_COMPLETED, LEVEL_ABANDONED,
    LEVEL_RESTARTED, UNDO_USED, HINT_REQUESTED, HINT_SHOWN, DAILY_STARTED,
    DAILY_COMPLETED, REPLAY_STARTED, SHARE_REQUESTED, GENERATOR_FALLBACK_OR_ERROR,
    AD_REQUEST, AD_RESULT, PURCHASE_REQUEST, PURCHASE_RESULT,
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

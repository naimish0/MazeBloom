package com.rameshta.mazebloom.services

import androidx.activity.ComponentActivity
import com.rameshta.mazebloom.BuildConfig

fun createAdsGateway(activity: ComponentActivity): AdsGateway =
    if (BuildConfig.ADMOB_ENABLED) {
        GoogleMobileAdsGateway(
            activity = activity,
            interstitialAdUnitId = BuildConfig.ADMOB_INTERSTITIAL_ID,
            rewardedAdUnitId = BuildConfig.ADMOB_REWARDED_ID,
        )
    } else {
        NoOpAdsGateway
    }

# Monetization and reward policy

The current MazeBloom economy and ad rules are:

- The app credits 30 Coins once when it first opens on a strictly later local calendar day. Reopening on the same day or moving the clock backward does not credit again.
- A first completion of a Campaign, Daily, or Endless level credits exactly 10 Coins; replaying an already completed level grants zero.
- Hint and Undo each cost 30 Coins. Skip Level costs 50 Coins and is unavailable on Daily levels and Campaign levels that are not eligible to advance.
- When the current balance is below an action's cost, that action truthfully shows an ad affordance. A user click requests a rewarded ad; the action occurs only after the Google Mobile Ads reward callback is received and its random transaction ID is recorded once. The rewarded path authorizes the action directly and does not add or subtract Coins.
- A skipped level grants no completion Coins or stars. Skipped Endless definitions remain in uniqueness history.
- Every fifth first completion makes one standard interstitial due. Generation, recomposition, tutorial animation, celebration rendering, navigation restoration, Undo, Restart, Hint, and Skip never count as completions.
- The due interstitial is taken and attempted immediately after the solved board and its completion transaction are persisted. Next and Back retain a recovery check for any due state that survived interruption, but normal navigation does not create another request. An unavailable, consent-blocked, cooldown-blocked, or failed request never blocks gameplay.
- Completing a rewarded ad starts a durable 60-second standard-interstitial protection window. An interstitial attempted during that period is skipped; clock rollback is treated as still protected.
- App Open ads may be requested when the activity enters the foreground. They require a resumed activity and completed consent/SDK initialization, use a maximum five-second foreground wait, and start a durable two-hour cooldown only after a confirmed impression. Cached App Open inventory older than four hours is discarded.
- All full-screen formats share one presentation lock, and App Open ads also observe a ten-second in-memory guard after another full-screen ad is dismissed. Rewarded ads have no separate time or daily cap because they are explicit user choices for an action; inventory, consent, lifecycle, and the one-action-in-flight guard still apply.

Debug uses Google Mobile Ads/UMP with Google's official App Open, interstitial, and rewarded test inventory; deterministic fakes remain the automated-test seam. The debug-only Developer garden exposes non-mutating standard-interstitial and rewarded diagnostics, while App Open inventory is exercised by foregrounding the app. The offline `release` build does not package either Google SDK and uses no-op gateways. The separate live-ads `production` build requires four externally supplied production identifiers—one app ID and App Open, interstitial, and rewarded unit IDs—that pass format checks and do not equal Google's test IDs.

Ad requests are ViewModel/lifecycle actions, never Compose-side mutations. Duplicate rewarded transaction IDs are rejected by the `ad_grants` primary key. The app uses the client SDK reward callback and local idempotency; it does not claim server-side verification.

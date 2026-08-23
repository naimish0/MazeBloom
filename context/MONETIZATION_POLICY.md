# Monetization and reward policy

The Endless Garden/UI work does not change the established economy:

- A first completion credits exactly 10 Coins; replaying an already completed level grants zero.
- A Coin-funded Hint costs exactly 30 Coins in one atomic spend.
- When the balance is insufficient, the Hint button truthfully shows an ad marker; a click requests the existing rewarded flow and no reward is assumed before its success callback.
- Skip always displays an ad marker. A successful, idempotently recorded rewarded callback advances exactly once and grants no Coins. Skipped Endless definitions remain in uniqueness history.
- Every fifth first completion makes one interstitial due. Generation, recomposition, tutorial animation, celebration, navigation restoration, and Skip never count as completions.
- Interstitials display only at the existing post-result navigation boundary. Campaign level 2,000 persists any due state but defers it through the entire Endless Garden unlock experience.
- Debug uses Google Mobile Ads/UMP with Google’s official test inventory; deterministic fakes remain the automated-test seam. The debug-only Developer garden exposes non-mutating interstitial and rewarded diagnostics. The offline `release` build does not package either SDK and uses the no-op gateway. The separate live-ads `production` build requires externally supplied production IDs that pass format checks and do not equal Google test IDs.

Rewarded and interstitial requests are ViewModel/repository actions, never Compose-side mutations. Duplicate transaction IDs are rejected by the existing `ad_grants` primary key.

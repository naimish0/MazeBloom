# QA and release checklist

## Automated gates

- [x] Exact transition/invariant tests.
- [x] BFS, budgets, replay codec/checksum, SplitMix64, D4, strict-similarity tests.
- [x] Fast exact packaged-content verification for 2,000 Campaign levels, 100 shards, 120 Daily levels, and 100 Progressive levels.
- [x] Full re-solve of 2,220 levels and exhaustive 2,463,090-pair / 11,691,120-alignment uniqueness certification.
- [x] Progression and 100→2,000 compatibility fixtures plus persistence, daily/first-completion Coins, atomic Hint/Undo/Skip spending, rewarded-ad idempotency, every-five cadence, App Open cooldown, and post-reward protection tests.
- [x] Exact 2,220-record Endless baseline and 1,000 generated-level horizon: 2,719,500 new obligations / 5,182,590 independent corpus pairs / zero new collisions.
- [x] Room v1→2→3→4→5→6→7 and real exported migration tests compile; the new v6→7 execution still requires a connected instrumentation run.
- [x] Final debug unit tests, lint, APK, release-shrunk bundle, and aggregate check (recorded in the implementation report).
- [x] R8 full-mode optimization and optimized resource shrinking enabled with retraceable line information and app debug/info log stripping.
- [x] Offline release variant excludes Google ads/UMP dependencies and ad/network permissions; live-ads production variant is separately gated.
- [x] Release manifest disables backup/device transfer and cleartext traffic; private FileProvider is limited to the share cache.
- [x] Static privacy policy, in-app policy link, Data safety worksheet, secure signing injection, and fail-closed Play bundle gates implemented.
- [x] Six connected instrumentation tests on Samsung SM-S928B / Android 16 cover the previous Room v1→6/v2→6 migration path and 1,000-row storage growth.
- [x] Samsung SM-S928B official rewarded test creative completed through the real SDK callback; an immediate standard-interstitial diagnostic was suppressed by the persisted 60-second protection.
- [ ] Connected Compose UI automation and the full manual device/accessibility matrix.

## Manual device matrix

- [ ] Fresh install and animated-finger tutorials 1–5.
- [ ] Later Bloom-assisted level; Undo, Restart, Hint, replay, Next.
- [ ] “Continue growing” → Game → System Back returns directly to Home; verify the equivalent Garden, Chapter, Daily, and Endless return targets.
- [ ] Background/process restoration.
- [ ] Daily in airplane mode; same-day relaunch, forward date, backward date, and timezone cases grant 30 Coins only on a forward day.
- [ ] Share captures the visible emoji/confetti/result screen, grants only the cache URI, and includes the Play Store app URL.
- [ ] Phone, foldable, 7-inch, and 10-inch layouts.
- [ ] Large font, reduced motion, high contrast, direction buttons, TalkBack.
- [ ] Completion emoji/confetti with standard and reduced motion, including emoji visibility behind/in front of the result sheet.
- [ ] 10-Coin first-completion reward; 30-Coin Hint and Undo; 50-Coin Skip; insufficient-balance rewarded affordances; duplicate/unavailable callbacks; and unchanged balance on rewarded authorization.
- [ ] Fifth first completion attempts the standard interstitial immediately after solve; a completed reward suppresses it for 60 seconds, including across process recreation.
- [ ] App Open on cold/foreground entry, consent/lifecycle failures, no full-screen overlap, and persisted two-hour cooldown from a confirmed impression.
- [ ] Campaign 2,000 → Endless unlock → immutable starter 1–100 → generated 101+, including Coin-funded and rewarded Skip plus process relaunch.
- [ ] Low-storage `STORAGE_BLOCKED` retry preserves the same history/candidate; capture actual Room growth for 1,000 representative rows.
- [ ] Representative low-memory, mid-range, and high-end generation latency/memory/heat/battery runs.

## External release blockers

- Product name/package/trademark clearance; representative human difficulty review and closed testing.
- Production signing and Play App Signing.
- Publicly host the completed privacy page, submit the matching Data safety form, and finish the store listing, content rating, and screenshots.
- Approved ads/consent/analytics vendors and configuration, if desired.
- Play Console one-time Remove Ads product and real purchase/restore testing, if desired.

# QA and release checklist

## Automated gates

- [x] Exact transition/invariant tests.
- [x] BFS, budgets, replay codec/checksum, SplitMix64, D4, strict-similarity tests.
- [x] Fast exact packaged-content verification for 2,000 Campaign levels, 100 shards, 120 Daily levels, and 100 Progressive levels.
- [x] Full re-solve of 2,220 levels and exhaustive 2,463,090-pair / 11,691,120-alignment uniqueness certification.
- [x] Progression and 100→2,000 compatibility fixtures plus persistence, coin, rewarded-ad idempotency, skip, and every-five policy tests.
- [x] Exact 2,220-record Endless baseline and 1,000 generated-level horizon: 2,719,500 new obligations / 5,182,590 independent corpus pairs / zero new collisions.
- [x] Room v1→2→3→4→5→6 and real exported migration tests compile; latest execution still requires a connected device.
- [x] Final debug unit tests, lint, APK, release-shrunk bundle, and aggregate check (recorded in the implementation report).
- [x] R8 full-mode optimization and optimized resource shrinking enabled with retraceable line information and app debug/info log stripping.
- [x] Offline release variant excludes Google ads/UMP dependencies and ad/network permissions; live-ads production variant is separately gated.
- [x] Release manifest disables backup/device transfer and cleartext traffic; private FileProvider is limited to the share cache.
- [x] Static privacy policy, in-app policy link, Data safety worksheet, secure signing injection, and fail-closed Play bundle gates implemented.
- [x] Six connected instrumentation tests on Samsung SM-S928B / Android 16, including Room v1→6/v2→6 migration and 1,000-row storage growth.
- [ ] Connected Compose UI automation and the full manual device/accessibility matrix.

## Manual device matrix

- [ ] Fresh install and animated-finger tutorials 1–5.
- [ ] Later Bloom-assisted level; Undo, Restart, Hint, replay, Next.
- [ ] Background/process restoration.
- [ ] Daily in airplane mode and clock/timezone cases.
- [ ] Share card and cache URI.
- [ ] Phone, foldable, 7-inch, and 10-inch layouts.
- [ ] Large font, reduced motion, high contrast, direction buttons, TalkBack.
- [ ] Completion emoji/confetti with standard and reduced motion.
- [ ] 10-Coin reward, 30-Coin hint, insufficient-balance ad icon, rewarded skip, fifth-completion interstitial, and unavailable-ad behavior.
- [ ] Campaign 2,000 → Endless unlock → immutable starter 1–100 → generated 101+, including rewarded Skip and process relaunch.
- [ ] Low-storage `STORAGE_BLOCKED` retry preserves the same history/candidate; capture actual Room growth for 1,000 representative rows.
- [ ] Representative low-memory, mid-range, and high-end generation latency/memory/heat/battery runs.

## External release blockers

- Product name/package/trademark clearance; representative human difficulty review and closed testing.
- Production signing and Play App Signing.
- Publicly host the completed privacy page, submit the matching Data safety form, and finish the store listing, content rating, and screenshots.
- Approved ads/consent/analytics vendors and configuration, if desired.
- Play Console one-time Remove Ads product and real purchase/restore testing, if desired.

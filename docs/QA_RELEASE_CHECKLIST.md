# QA and release checklist

## Automated gates

- [x] Exact transition/invariant tests.
- [x] BFS, budgets, replay codec/checksum, SplitMix64, D4, duplicate tests.
- [x] 100 campaign + 120 daily exact packaged-content verification.
- [x] Persistence fake and monetization-policy tests.
- [x] Debug unit tests, lint, APK, release-shrunk bundle, and aggregate check (see implementation report for commands).
- [ ] Connected instrumentation/UI tests: requires a compatible running device/emulator.

## Manual device matrix

- [ ] Fresh install and onboarding 1–5.
- [ ] Later Bloom-assisted level; Undo, Restart, Hint, replay, Next.
- [ ] Background/process restoration.
- [ ] Daily in airplane mode and clock/timezone cases.
- [ ] Share card and cache URI.
- [ ] Phone, foldable, 7-inch, and 10-inch layouts.
- [ ] Large font, reduced motion, high contrast, direction buttons, TalkBack.
- [ ] Consent/ad unavailable and fake rewarded/billing callbacks.

## External release blockers

- Product name/package/trademark clearance; representative human difficulty review and closed testing.
- Production signing and Play App Signing.
- Store listing, privacy policy/data safety, content rating, screenshots.
- Approved ads/consent/analytics vendors and configuration, if desired.
- Play Console one-time Remove Ads product and real purchase/restore testing, if desired.

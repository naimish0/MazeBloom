# MazeBloom

MazeBloom is an offline, deterministic Android puzzle game: **swipe through every Bud; every cell the Seed leaves Blooms into a wall**. It uses Kotlin, Jetpack Compose, Material 3, and the existing single `:app` module.

## Build and run

Requirements:

- Android Studio with Android SDK 37 installed (the app targets API 36 and supports API 24+).
- JDK 17 or the repository's checked-in compatible Gradle daemon criteria (currently JDK 21). Source and target compatibility are Java 17.
- Node.js only when regenerating or directly verifying content with the authoring CLI. Normal source compilation does not regenerate content.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleRelease # unsigned local QA, fully R8 optimized
./gradlew :app:bundleRelease   # signed, privacy-first offline Play bundle
./gradlew :app:bundleProduction # signed, live-AdMob Play bundle
```

Content commands:

```bash
node tools/content-cli.mjs solve campaign-004
node tools/content-cli.mjs analyze campaign-096
node tools/content-cli.mjs render campaign-001
node tools/content-cli.mjs replay campaign-004
node tools/content-cli.mjs dedupe
node tools/content-cli.mjs compare campaign-0101 campaign-0102
node tools/content-cli.mjs analyze progressive-0001
./gradlew :app:verifyBundledContentFast
./gradlew :app:certifyCampaignFull :app:verifyBundledContent
./gradlew :app:certifyAutoProgressiveHorizon
./gradlew :app:verifyEndlessHorizonStamp
./gradlew :app:generateBundledContent # intentional content update only
```

Content regeneration preserves the immutable 100-level source prefix and intentionally updates 100 Chapter shards, the Daily pool, the 100-level Endless Garden starter prefix, roots, and reports. Endless Garden unlocks after Campaign level 2,000; ordinal 101 onward is generated and fully certified offline just in time against the fixed 2,220-level baseline and retained local history. Every generated 100-level block keeps the exact established difficulty/board-size mix. Debug packaging runs the fast exact-asset gate; release bundling runs full re-solving and verifies a current exact-key 1,000-level horizon stamp. Ordinary builds never regenerate levels, and horizon boards are not shipped as a pack.

Campaign levels 1–5 are tutorials with an animated finger cue. First completions award 10 Coins; a hint costs 30 Coins and falls back to a rewarded-ad affordance when the balance is insufficient. Skip always uses a rewarded-ad affordance, and an interstitial is due after every fifth first completion. Debug binds Google Mobile Ads and UMP with Google’s official test app/ad-unit IDs. The optimized `release` build omits both SDKs and all advertising/network permissions. The separate optimized `production` build packages the real adapter only for a signed live-ads release and fails closed unless three valid external, non-test production IDs are configured.

## External configuration

Gameplay remains usable without a network or account. Deterministic fake gateways remain available to automated tests, while the debug-only Developer garden can present non-mutating rewarded/interstitial test inventory on a device. Release builds use R8 full-mode code optimization and optimized resource shrinking, disable debugging/backups/cleartext traffic, and expose the hosted privacy policy from Settings. Play bundle tasks require externally injected upload-key credentials; the live-ads bundle additionally requires approved production IDs. No live ID, password, or signing material is checked in.

See [docs/PRODUCTION_RELEASE.md](docs/PRODUCTION_RELEASE.md) for the release workflow, [docs/PLAY_DATA_SAFETY.md](docs/PLAY_DATA_SAFETY.md) for exact build disclosures, [docs/index.html](docs/index.html) for the publishable privacy policy, and [docs/IMPLEMENTATION_REPORT.md](docs/IMPLEMENTATION_REPORT.md) for implementation evidence.

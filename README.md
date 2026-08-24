# MazeBloom

MazeBloom is an offline, deterministic Android puzzle game: **swipe through every Bud; every cell your mover leaves Blooms into a wall**. It uses Kotlin, Jetpack Compose, Material 3, and the existing single `:app` module.

## Build and run

Requirements:

- Android Studio with Android SDK 37 installed (the app targets API 36 and supports API 24+).
- JDK 17 or the repository's checked-in compatible Gradle daemon criteria (currently JDK 21). Source and target compatibility are Java 17.
- Node.js only when regenerating or directly verifying content with the authoring CLI. Normal source compilation does not regenerate content.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleRelease # unsigned local QA, fully R8 optimized
./gradlew :app:bundleRelease    # requires Android Studio/CI-injected signing
./gradlew :app:bundleProduction # requires Android Studio/CI-injected signing; includes live AdMob
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

Campaign levels 1–5 are tutorials with an animated finger cue. After 10 Campaign completions, Animal Companions unlock with Meadow Mouse free. The 100-companion roster includes 22 mammals, 22 birds, 22 insects, 17 garden/reptile friends, and 17 aquatic friends; permanent cosmetic unlocks cost 100 Coins for the second companion, 150 for the third, then increase by 50 through 5,000. Alternatively, every completed rewarded ad applies 50 Coins of persistent credit only to the chosen Companion; credits can accumulate to a full unlock or reduce the Coins needed for that same Companion. The selected companion replaces only the Seed's presentation—rules, replays, solutions, stars, and certified content remain unchanged. Category-specific Compose art animates on the collection, board, and celebration screens and respects Reduced motion.

Settings includes four persistent color themes—Living Garden, Rose Garden, Moonlit Pond, and Golden Meadow. Each theme has coordinated light and dark Material 3 tokens plus board colors, works with System/Light/Dark appearance, and preserves the separate high-contrast option.

Every solved board presents visible flower emojis and confetti, and Share sends a screenshot of that celebration with the Play Store app URL. The app grants 30 Coins once per forward local day, and first completions award 10 Coins. Hint and Undo each cost 30 Coins; Skip Level costs 50 Coins. Each action falls back to a rewarded-ad affordance when the balance is below its cost. An interstitial is due after every fifth first completion and is attempted immediately when that board is solved; a completed rewarded ad suppresses interstitials for the following 60 seconds. The first three process launches remain App Open ad-free; later ads use only preloaded inventory on a warm foreground, with a durable one-hour cooldown between confirmed impressions. Debug binds Google Mobile Ads and UMP with Google's official test app/ad-unit IDs. The optimized `release` build omits both SDKs and all advertising/network permissions. The separate optimized `production` build packages the real adapter with the four checked-in public, non-test production identifiers.

## External configuration

Gameplay remains usable without a network or account. Deterministic fake gateways remain available to automated tests, while the debug-only Developer garden can present non-mutating rewarded/interstitial test inventory on a device. Release builds use R8 full-mode code optimization and optimized resource shrinking, disable debugging/backups/cleartext traffic, and expose the hosted privacy policy from Settings. Use Android Studio’s standard **Build → Generate Signed Bundle / APK** flow for Play bundles; the custom gate recognizes its process-injected credentials. Public AdMob identifiers are checked in, while keystore passwords and signing material are not.

See [PRODUCTION_RELEASE.md](context/PRODUCTION_RELEASE.md) for the release workflow, [PLAY_DATA_SAFETY.md](context/PLAY_DATA_SAFETY.md) for exact build disclosures, [the public app-details page](docs/project.html), [docs/index.html](docs/index.html) for the publishable privacy policy, and [IMPLEMENTATION_REPORT.md](context/IMPLEMENTATION_REPORT.md) for implementation evidence.

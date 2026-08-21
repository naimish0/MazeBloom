# MazeBloom

MazeBloom is an offline, deterministic Android puzzle game: **swipe through every Bud; every cell the Seed leaves Blooms into a wall**. It uses Kotlin, Jetpack Compose, Material 3, and the existing single `:app` module.

## Build and run

Requirements:

- Android Studio with Android SDK 37 installed (the app targets API 36 and supports API 24+).
- The checked-in Gradle wrapper. Its daemon criteria select JDK 21; do not run Gradle with unsupported JDK 26 despite it being the shell default on some machines.
- Node.js only when regenerating or directly verifying content with the authoring CLI. Normal source compilation does not regenerate content.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:bundleRelease
```

Content commands:

```bash
node tools/content-cli.mjs solve campaign-004
node tools/content-cli.mjs analyze campaign-096
node tools/content-cli.mjs render campaign-001
node tools/content-cli.mjs replay campaign-004
node tools/content-cli.mjs dedupe
node tools/content-cli.mjs certify
./gradlew :app:generateBundledContent :app:certifyCampaign
```

Content regeneration is intentional and updates checked-in assets and reports. Ordinary packaging runs `verifyBundledContent` but never regenerates levels.

## External configuration

Gameplay requires no network, account, credentials, ads, billing, or analytics. The app currently binds safe no-op gateways. Production signing, approved ad/consent SDK configuration, a Play Console non-consumable product, an approved analytics vendor, privacy disclosures, store listing, and product-name/package clearance remain external release work. No live IDs or secrets are present.

See [docs/IMPLEMENTATION_REPORT.md](docs/IMPLEMENTATION_REPORT.md) for implemented scope and verification evidence.

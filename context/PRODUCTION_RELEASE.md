# Production release guide

MazeBloom has two optimized non-debug build types:

| Build | Purpose | Network/ad SDKs |
| --- | --- | --- |
| `release` | Preferred privacy-first Play release | None; game remains fully offline |
| `production` | Optional monetized Play release | Google Mobile Ads 25.4.0 and UMP 4.0.0 |

Both builds target API 36, support API 24+, disable debugging, run R8 full-mode code optimization and optimized resource shrinking, strip app verbose/debug/info logging, preserve retraceable line numbers, block cleartext traffic, and disable app-data backup/device transfer.

## Secure configuration

Supply secrets in protected CI environment variables or `~/.gradle/gradle.properties`. Never put them in this repository.

```properties
MAZEBLOOM_KEYSTORE_FILE=/absolute/path/to/mazebloom-upload.jks
MAZEBLOOM_KEYSTORE_PASSWORD=...
MAZEBLOOM_KEY_ALIAS=upload
MAZEBLOOM_KEY_PASSWORD=...
MAZEBLOOM_VERSION_CODE=1
MAZEBLOOM_VERSION_NAME=1.0
```

The optional ad-supported build also requires three real, non-test identifiers:

```properties
MAZEBLOOM_ADMOB_APP_ID=ca-app-pub-0000000000000000~0000000000
MAZEBLOOM_ADMOB_INTERSTITIAL_ID=ca-app-pub-0000000000000000/0000000000
MAZEBLOOM_ADMOB_REWARDED_ID=ca-app-pub-0000000000000000/0000000000
```

Local QA builds default to `https://naimish0.github.io/MazeBloom/`. A Play bundle requires `MAZEBLOOM_PRIVACY_POLICY_URL` to be set explicitly after that GitHub Pages URL (or another host) is live and verified.

## Build and verify

Local unsigned artifacts are allowed only for optimization and QA:

```bash
./gradlew :app:assembleRelease
./gradlew :app:assembleProduction
```

Play bundles fail closed when signing is absent. The ad-supported bundle also fails if IDs are missing, malformed, or equal Google's test IDs:

```bash
./gradlew :app:bundleRelease
./gradlew :app:bundleProduction
```

Each bundle command runs the exact packaged-content gate, the full 2,220-level solve/uniqueness certification, the Endless baseline check, and the pinned 1,000-level horizon-stamp check. Archive these outputs for every shipped version:

- the signed `.aab`;
- `mapping.txt` for crash deobfuscation;
- `seeds.txt`, `usage.txt`, and `resources.txt` for shrinker auditing;
- the Git revision and CI build log; and
- the upload certificate SHA-256 fingerprint.

Run `./gradlew :app:assembleRelease` after changing reflection, serialization, manifests, navigation, or keep rules, then review the generated mapping, seeds, usage, and resource-shrinker outputs. Do not add package-wide `-keep` rules for AndroidX, Compose, Room, Kotlin, Google Mobile Ads, or UMP; these libraries supply their own focused consumer rules.

## Play Console completion

Before promotion beyond internal testing:

- Verify the developer identity and confirm the final package/product name is cleared for use.
- Enroll the app in Play App Signing; protect a separate upload key and recovery material.
- Increment `versionCode` for every upload and set the customer-facing `versionName`.
- Enable GitHub Pages from the repository `docs/` folder (or host the same static page elsewhere), verify the public URL without authentication or JavaScript, link it in Play Console, and test the in-app Settings link.
- Complete Data safety using [PLAY_DATA_SAFETY.md](PLAY_DATA_SAFETY.md) for the exact build being uploaded.
- Declare whether the release contains ads, complete the content-rating questionnaire, provide app-access instructions, and select the actual target audience. The current ad-supported implementation is for a non-child audience; do not select child age groups without implementing and validating Families-compliant age/ads handling first.
- Create the store listing, feature graphic, phone/tablet screenshots, short/full descriptions, support email, and content classification.
- Use an internal track first, then complete the required closed-testing and pre-launch-report review for the developer account.
- Test the Play-generated split APK on at least one supported phone and one supported tablet, including offline play, process restoration, sharing, accessibility, and—if applicable—live consent, rewarded ads, and interstitial cadence.
- Upload `mapping.txt` and retain Play vitals/crash/ANR monitoring ownership for every release.

Production ad IDs, the upload key, AdMob privacy-message configuration, Play Console declarations, store assets, trademark clearance, and closed-testing approval are operator-owned external state and are intentionally not committed.

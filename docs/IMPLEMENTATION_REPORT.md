# Implementation report

## Scope and structure

MazeBloom replaces the starter greeting with a functional offline garden puzzle. The single existing `:app` module is preserved; pure core, local data, UI/services, and JVM authoring responsibilities are separated by package/tool boundaries documented in `ARCHITECTURE.md`.

Implemented player flows: Home, five-chapter Campaign, Game, completion, replay, Collection, Settings, and Daily Bloom after level ten. Gameplay includes swipe/direction input, ordered presentation, free Undo/Restart, exact current-state hints, DEAD/DOOMED handling, stars, sequential unlocks, best replay, active-run restoration, daily selection/history/streak, local share cards, feedback, accessibility settings, responsive layout, and debug content inspection.

The deliberate non-goals remain accounts, backend, network-required play, currencies/lives, gameplay purchases, enemies/gimmicks, level editor, cloud/global competition, and vendor SDKs without approved configuration.

## Content certification

- Campaign: 100 (5 Tutorial, 15 Easy, 20 Normal, 30 Hard, 25 Expert, 5 Master; 70 5×5, 30 6×6).
- Daily pool: 120 (80 5×5, 40 6×6). The aspirational 365 target was not pursued beyond the mandatory certified minimum in this implementation pass.
- Campaign SHA-256: `cd4b519827dfc41bfd741c179511e53bbfcbc209e7fb7116d0ff65b60243224c`.
- Daily SHA-256: `5e59e244bf0da0e63a9ff0c020793c3102ce57d60bb45ac148b84817a82673c0`.
- Accepted corpus exact geometric/hard-near duplicates: 0. Maximum shortest discovered states: 96. Maximum audit reachable states: 96.
- Generator attempts: 8,338 campaign and 5,678 daily. Full rejection counts are machine-readable under `reports/content`.
- Human approval: **PENDING**; no manual review is fabricated.

## Services and release state

Ads, billing, consent, and analytics have safe interfaces, no-op defaults, deterministic fakes, and policy tests. No vendor, live ID, test inventory, permission, or credential was added. Gameplay fails open when every service is unavailable. Real production integrations, signing, Play configuration, name clearance, human review, and closed testing remain external work.

## Verification

Actual results on macOS 26.5.1 arm64 with Gradle 9.3.1 and Eclipse Temurin JDK 21:

| Command | Result |
| --- | --- |
| `./gradlew --version` | PASS |
| `./gradlew projects` | PASS; root plus `:app` |
| `node tools/generate-content.mjs` | PASS; deterministic assets/reports regenerated |
| `node tools/verify-content.mjs` | PASS; 100 campaign, 120 daily |
| `node tools/content-cli.mjs dedupe` | PASS; 0 exact geometric groups |
| `./gradlew :app:testDebugUnitTest` | PASS; 21 tests, 0 failures/skips |
| `./gradlew :app:certifyCampaign` | PASS |
| `./gradlew :app:lintDebug` | PASS; 20 non-fatal advisories (intentional target/dependency pins, plural/KTX suggestions) |
| `./gradlew :app:assembleDebug` | PASS; 11 MB APK |
| `./gradlew :app:bundleRelease` | PASS with R8/resource shrinking; 2.4 MB AAB |
| `./gradlew check` | PASS |
| `./gradlew :app:connectedDebugAndroidTest` | SKIPPED; `adb devices -l` reported no attached device/emulator |

The final 220-level JVM diagnostic measured exact shortest solve p50 **0.09325 ms** and p95 **0.338666 ms** in one warm test process. These timings are non-canonical diagnostics, not acceptance inputs. Authoritative corpus maxima are 96 shortest discovered states and 96 audit reachable states.

No manual device smoke claims were made because no compatible target was attached. Fresh install, TalkBack, large font/device matrix, process recreation, airplane-mode Daily, share URI, and release-like launch remain on the manual checklist.

Artifacts:

- Debug APK SHA-256: `a8f69ba3b3b73b3dbf997bba515407dc5803c3b86957aacb78a428f0ac007e63`.
- Release AAB SHA-256: `4eb6f09e00ee895968d8d501c16005028b364a89e20c9ae8fb8cd0c205189b6f`.

The workspace is not a Git repository (`git status --short` reports “not a git repository”), so a Git diff/status cannot be produced. The implementation adds 16 production Kotlin files, focused core/data/service/UI tests, three content assets, two authoring CLIs, two machine reports, and the requested documentation; it updates Gradle, manifest/resources, theme, and launcher art.

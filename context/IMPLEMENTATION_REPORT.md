# Implementation report

## Outcome

The missing project snapshot was recovered from the retained stash commit and pinned on `recovery/abc-2026-08-22`. MazeBloom now implements the one-go specification in the existing `:app` module, expanded by the user's later requirements.

Implemented player flows include Home, 20-Garden/100-Chapter Campaign, Daily Bloom, Collection, Animal Companions, Settings, Game, completion/replay/share, and a separately versioned Auto Progressive pack unlocked after Campaign level 2,000. Campaign levels 1–5 are Tutorials with a directional animated-finger cue. Every solved level shows flower/emoji/confetti celebration, with static reduced-motion behavior.

The local economy grants 30 Coins once per forward local day and awards 10 Coins once per Campaign, Daily, or Progressive definition. Hint and Undo each cost 30 Coins, while Skip Level costs 50 Coins; any of those actions uses an explicit rewarded-ad fallback when the balance is insufficient. Progressive skips persist separately and never fabricate completion Coins or stars. Animal Companions unlock after 10 Campaign completions with one free mover and 99 permanent Coin unlocks; each verified Companion rewarded ad contributes 50 Coins of non-transferable credit only to the chosen animal. Every fifth first completion marks one standard interstitial due for an immediate post-solve attempt, subject to the 60-second protection after a completed rewarded ad. Room schema v8 persists progress, economy/cadence state, daily-grant bookkeeping, attempts, skip markers, Daily data, Companion selection/ownership/ad credit, Endless generation history, and consumed ad transaction IDs through explicit non-destructive migrations.

Debug builds use Google Mobile Ads/UMP with Google’s official test inventory, while automated tests retain deterministic fake gateways. The optimized offline `release` build physically excludes both SDKs and their network/advertising permissions. A separate optimized `production` build contains the live adapter but cannot be bundled until approved non-test identifiers, a public policy URL, and secure release signing are supplied.

## Certified content

- Campaign: 2,000 levels, 20 Gardens, 100 Chapters, and 100 independently hashed 20-level shards. Tutorials 1–5 remain byte/checksum compatible; levels 6–2,000 use profile v3.
- Campaign mix: 5 Tutorial, 319 Easy, 552 Normal, 562 Hard, 405 Expert, and 157 Master; 564 are 5×5 and 1,436 are 6×6.
- Daily pool v2: 120 levels; 30 each Easy/Normal/Hard/Expert; 40 are 5×5 and 80 are 6×6.
- Auto Progressive v1: 100 levels; exactly 16 Easy, 28 Normal, 28 Hard, 20 Expert, and 8 Master; 26 are 5×5 and 74 are 6×6.
- Exhaustive certification: 2,220 solved definitions, 2,463,090 unordered pairs, and 11,691,120 D4 alignments. Illegal new collision count: 0. Only Tutorials 1–5 are eligible for grandfathering.
- Authoritative maxima: 142 shortest-path discovered states and 167 audit-reachable states.
- Human review: PENDING. The 583-row worksheet has blank review fields and makes no fabricated playtest claim.

Strict uniqueness cannot support an infinite stream on finite 5×5/6×6 geometry. Auto Progressive is therefore a deterministic finite 100-level pack, generated only after Campaign and Daily have populated the uniqueness registry. A later pack version can extend it only after certification against every retained definition.

Pinned roots:

- Campaign root: `b072e72774dab836109b98a66d9bc0789f2b94f3054e5864d85df64c282f2b04`
- Progressive content root: `6e756b18a03302ed52ddb9972ec96f4b282d596d8eb77831ba90e1dbe39ad33c`
- Global uniqueness root: `3b2cd45d7418d755a21e1fb00b8e760b685b2a68a0560148fa34bc2b3bc5e9de`
- Release content root: `de75db8049088f0a9125c490aa6a6ada0f773a041300aa1e401935f7127edf7c`

## Final verification

Actual results on macOS 26.5.1 arm64, Gradle 9.3.1, launcher JDK 26, and Gradle daemon JDK 21:

| Command/gate | Result |
| --- | --- |
| `./gradlew --version` and `./gradlew projects` | PASS; root plus `:app` |
| deterministic generation and fast exact-asset verification | PASS |
| `node tools/content-cli.mjs dedupe` | PASS; 2,220 levels, zero exact geometric duplicate groups |
| `./gradlew :app:certifyCampaignFull` | PASS; 2,220 solves / 2,463,090 pairs / 11,691,120 alignments |
| `./gradlew :app:testDebugUnitTest` | PASS; 28 tests, 0 failures/errors/skips |
| `./gradlew :app:lintDebug` | PASS; 0 errors, 18 non-fatal warnings |
| `./gradlew :app:assembleDebug` | PASS |
| `./gradlew check` | PASS |
| `./gradlew :app:bundleRelease` | PASS with R8 and resource shrinking |
| connected device/UI verification | SKIPPED; `adb devices -l` reported no attached device/emulator |
| `git diff --check` | PASS |

The lint warnings are advisory: pinned target/tool/dependency versions, plural-resource suggestions, one unused generic Hint string, and a bitmap KTX suggestion. The warm JVM bundled-content diagnostic measured exact solve p50 0.075417 ms and p95 0.443625 ms; timings are diagnostic rather than checksum inputs.

Artifacts:

- Debug APK: 13,712,465 bytes; SHA-256 `e5c6cfd45e1ba57a9170921e9ba8fd9fb1c1a94828252c0363f0fb1cd8ab525d`.
- Release AAB: 4,094,059 bytes; SHA-256 `71335760b5e5f9c4bf6e618d1d32e9efb5d29e3c5f5146c9e20f205d8e6828e1`.

## External release work

No approval is needed to finish, build, or test the local implementation. Shipping live ads requires the product owner's vendor choice and approvals, ad/consent SDK configuration and IDs, privacy/Data Safety disclosures, and signed-device inventory testing. Store publication also requires production signing, Play Console setup, listing assets, content rating, product/package clearance, representative human difficulty review, and a device/accessibility matrix run.

---

## Attractive UI + Endless Garden v1.1 — 2026-08-22

This section supersedes the earlier report’s finite-100-level and Room-v2 descriptions while preserving that historical delivery record.

### Player-visible result

MazeBloom now uses a cohesive living-paper-garden Compose system across Home, Campaign, Garden, Chapter, Game, completion, Endless Garden, and Settings, with parchment/dark/high-contrast semantic palettes and saved System/Light/Dark selection. The Home hero and completion flower, board roles, confetti, restrained result emojis, and Campaign 1–5 finger tutorial are programmatically drawn; no bitmap, Lottie, font, or network-art dependency was added. Endless Garden is always discoverable and has locked, immutable starter 1–100, generated 101+, generating, storage/integrity failure, and retry-capable states. “Continue growing” routes to the actual next Campaign or Endless level; bundled shard paths retain their leading-slash compatibility fix so a false retry is not shown for a valid level.

The established economy remains transactional: +10 Coins once per first completion (including Daily and generated levels), 30 Coins per paid Hint, ad-marked Hint when insufficient, rewarded/ad-marked Skip with no Coins, idempotent rewarded transaction IDs, and one pending interstitial after every fifth first completion. Campaign 2,000 unlocks Endless without consuming a newly due interstitial during the unlock transition.

### Runtime generation and persistence

Room schema v6 retains `auto_progressive_state`, non-unique-start `generation_segments`, `generated_levels`, eight-transform `generated_uniqueness`, and `generation_checkpoint`. The exported fixtures migrate through the explicit 1→2→3→4→5→6 chain. Profile-v3 rebasing preserves completion/coin data when no generated history exists, clears only active attempts for replaced non-Tutorial definitions, and fails closed rather than rewriting accepted generated history. Completion/Skip lifecycle, full definition, canonical replay, certificate payload/hash, versions, fingerprints, CandidateKey, seed, and append-only history root are persisted atomically.

Generated ordinals use checked `Long` arithmetic through `281,474,976,710,756`. Attempts are deterministic `0..65,535` in resumable 256-attempt windows with 1,024 probe and 128 full-proof slot quotas. Generation is single-worker/off-main, begins near starter 97, and requests at most three certified levels ahead. Before commit it enforces the conservative 128 MiB registry working-set policy, deterministic 1 MiB per-level payload cap, and an independent 8 MiB free-space reserve. Storage failure preserves the checkpoint, accepted history, and candidate identity.

Every accepted board passes construction bounds, shortest probe, full shortest/audit solution, scheduled machine profile, open-cell coverage, serialization round trip, definition/D4, dynamic, structural, solution-grammar, hard-near, and review-similarity checks against the complete bundled and retained registry. The guarantee is local-history-dependent: Clear Data/uninstall without restore removes it, and finite puzzle space means the mode is practically unbounded rather than mathematical infinity.

### Preservation and horizon evidence

- Baseline: 2,000 Campaign + 120 Daily + Progressive starter 1–100 = 2,220 definitions; only Campaign Tutorials 1–5 remain checksum-locked.
- Campaign root: `b072e72774dab836109b98a66d9bc0789f2b94f3054e5864d85df64c282f2b04`.
- Progressive root: `6e756b18a03302ed52ddb9972ec96f4b282d596d8eb77831ba90e1dbe39ad33c`.
- Global uniqueness root: `3b2cd45d7418d755a21e1fb00b8e760b685b2a68a0560148fa34bc2b3bc5e9de`.
- Release content root: `de75db8049088f0a9125c490aa6a6ada0f773a041300aa1e401935f7127edf7c`.
- Endless baseline root: `cec815d9e353fc390ed5e2e8cc4d32f031f506adf6bb57bca7f726867f31a8cd`.
- Horizon: 1,000 accepted in 290,031 attempts in one segment; 289,031 categorized rejections and zero segment exhaustions, fully accounted.
- Rejections: construction 135,607; profile 19,865; structural 56; grammar 1,028; hard-near 1,078; review similarity 8,161; metric prefilter 117,836; shortest probe 5,400; all other strict causes 0.
- Schedule: every one of ten blocks has 16 Easy / 28 Normal / 28 Hard / 20 Expert / 8 Master and 26 5×5 / 74 6×6.
- New obligations: 2,719,500. Independent full-corpus comparator calls: 5,182,590. New collisions: 0.
- Horizon root: `1736d76e3b71ba85e02fecb57fda7bba3577204b9d06bdfcb5778933eaabd85c`.
- History root: `fc40c7fa694d58fe7ba6b09e7a55070c359f6b5cb3890cb54b43974e25229cd3`.
- Non-canonical verification diagnostics: 96,389,280-byte sampled peak heap under the 512 MiB process cap and 24.189 s independent comparator time. These are desktop diagnostics, not on-device generation latency.
- Solver totals for accepted horizon definitions: optimal moves 5–13, 40,434 expanded/discovered states, 1,033 optimal solutions counted.

The 1,000 horizon boards are not in the app. The release artifact carries only a current exact-key stamp whose baseline, rules/solver/schedule/namespace/profile/fingerprint/uniqueness/segment/tooling keys are verified before `bundleRelease`.

### Verification and remaining external work

Host results: Gradle 9.3.1; `projects` found only `:app`; 37 unit tests passed with zero failures/errors/skips; exact full 2,220-level certification and baseline/profile-v3 horizon stamp checks passed. The full bundled verifier solved 2,220 levels and compared 2,463,090 pairs / 11,691,120 D4 alignments without root drift. Android instrumentation sources and the v1→6 migration chain compiled, lint reported 0 errors/5 version advisories, and debug plus minified release APK assembly passed.

Final artifacts:

- Debug APK: 18,260,042 bytes; SHA-256 `84ad8c5b6794da4b29d646ba75f2ad0752df94f7c68c6c249216ebab06ee7987`.
- Instrumentation APK: 1,105,606 bytes; SHA-256 `6a277f085a2bf7924f4c7d6915e4d7ab885b5bad4754724c58d7a3fbe073b097`.
- Release AAB: 4,232,101 bytes; SHA-256 `54cb3f8768d0c7f67a35b834d00d9620c6ffa98e96fa5b2338a6d18a754fe0cc`.

The current instrumentation APK compiles and includes real v1→2→3/v2→3 migrations, same-ordinal segment upgrades, and an actual Room 1,000-row growth measurement. No device/emulator was discoverable for the final run, so those newest instrumentation tests, final screenshots, TalkBack/RTL/200%-font/rotation/process-death checks, physical low-storage behavior, actual database-growth bytes, and low/mid/high-device latency/memory/heat/battery benchmarks are **SKIPPED/PENDING**, not claimed. A Samsung device had passed the earlier three-test instrumentation suite before the final added tests, but its screen was unavailable for reliable final visual inspection.

Representative Hard/Expert/Master human playtesting is also pending; automatic bands do not prove perceived difficulty or fun. Live ads remain no-op in the default release until approved non-test IDs, privacy/Data Safety disclosures, Remove Ads/entitlement policy, and signed inventory validation are supplied. Play Console publication additionally needs production signing and store assets. None of those external approvals is required to build and fully verify the local implementation.

### Google test ads follow-up — 2026-08-22

Debug now binds Google Mobile Ads 25.4 and UMP 4.0 with Google’s official test app/interstitial/rewarded IDs. The Samsung SM-S928B (Android 16) resolved UMP, initialized the SDK, and preloaded both formats. The normal rewarded Skip path showed the visible “Test Ad” creative, received the verified reward callback, persisted two distinct idempotency IDs across two intentional manual runs, advanced from Campaign 50 to 52 exactly once per run, and left the 50-Coin balance unchanged.

The debug-only Developer garden now presents rewarded and interstitial diagnostics without claiming a transaction or touching economy/progress/cadence. Both visible official test creatives returned `SHOWN`; rewarded reported `reward verified: yes`. Before/after Room checks remained Campaign 52, 50 Coins, no pending interstitial, and 13 existing grants. The default release merged manifest has an empty application ID and `MobileAdsInitProvider enabled=false`; release BuildConfig has ads disabled and empty unit IDs. The current production gate validates the complete checked-in four-ID configuration and rejects Google's test identifiers.

Follow-up host verification passed 37 debug unit tests, `lintDebug` with 0 errors/5 version advisories, debug and minified release APK assembly, R8, exact bundled-content/baseline gates, and `git diff --check`. Production serving remains disabled until approved non-test IDs, privacy/Data Safety disclosures, Remove Ads/entitlement policy, and signed internal-test validation are complete.

### Production-readiness hardening — 2026-08-23

The build now uses Android Gradle Plugin 9.1.0 and Gradle 9.5.0 to match the supported Android Studio toolchain. Both non-debug variants use the documented pre-9.3 R8 configuration with minification, optimized resource shrinking, `proguard-android-optimize.txt`, and the focused `src/main/keepRules/mazebloom.keep` rules. Those rules preserve retraceable line information with renamed source files and remove app verbose/debug/info logs; there are no package-wide AndroidX, Kotlin, Room, Compose, ads, or UMP keep rules. Compile SDK remains 37 because current AndroidX artifacts require it, while target SDK remains 36; the project acknowledges that combination explicitly for the IDE-supported AGP version.

The offline `release` source graph excludes Google Mobile Ads and UMP entirely. Its final merged manifest has no Internet, network-state, Android advertising-ID, Privacy Sandbox advertising, or Google ads permission/component, and DEX inspection found no Google ads/UMP classes. The minified offline APK is 2,617,926 bytes versus 4,737,798 bytes for the ad-capable `production` APK. The production gate validates that all four checked-in identifiers are well formed and differ from Google's test IDs.

All variants disable Android cloud backup and device transfer, deny cleartext traffic with system-only trust anchors, and retain only a non-exported FileProvider limited to the generated share-card cache. Release signing is injected only from protected Gradle properties/environment variables; keystores and signing files are ignored. `bundleRelease` requires signing plus an explicitly confirmed public HTTPS privacy URL. `bundleProduction` additionally requires all production AdMob IDs. Both Play bundle tasks retain the full content, baseline, and horizon gates.

`docs/index.html` is the responsive, dark-mode/print-friendly, JavaScript-free MazeBloom Privacy Policy. It covers local gameplay data, optional Google ads/UMP behavior, sharing, permissions, security, retention/deletion, children, choices, developer identity, email, and postal contact. The W3C Nu HTML validator returned zero messages. Settings links to the configured policy URL, and `PLAY_DATA_SAFETY.md` separates the offline and ad-supported Play Console answers. The default GitHub Pages address still returns HTTP 404 until Pages is enabled, so the signed bundle gate requires an explicit URL after hosting is live.

Final local verification passed 37 unit tests, debug and release lint with 0 errors/3 version-target advisories, debug APK assembly, both minified non-debug APK assemblies, the exact fast content/baseline gates, and the R8 analyzer. On the Samsung SM-S928B running Android 16/API 36, all six instrumentation tests passed, including the v1→6/v2→6 migrations, same-ordinal v3→4 segment migration, actual 1,000-row Room growth bound, Android regex decode, and package-context check. The debug app installed and cold-started successfully without a fatal exception.

Final QA artifacts:

- Debug APK: 19,607,389 bytes; SHA-256 `7cc30520c8585459505821ce846d9263aef3659851649455246d2b8776c456dd`.
- Offline minified APK: 2,617,926 bytes; SHA-256 `3e9367a496eea1f879b5f868c504390f47f06eca61a8fe34fbb2920960753cd8`.
- Ad-capable minified APK: 4,737,798 bytes; SHA-256 `fe8551e0abddd6ab4dcef18cf1f17fff20403de4e5fd40cdd8d9fa00c539ab23`.
- Instrumentation APK: 1,170,178 bytes; SHA-256 `3e70e36417057bc4a005482729a0a5f8ffb84c238fe5392e1b8e576268448d13`.

The remaining publication work is intentionally external: create/protect the real upload key and enroll in Play App Signing; enable/verify public policy hosting; supply real AdMob IDs and publish the required regional UMP messages if shipping the ad build; finish accurate Play Data safety/ads/target-audience/content-rating declarations; provide store assets; clear the product/package name; complete representative human difficulty/accessibility/device testing and the developer-account testing requirements; and promote the signed AAB through Play tracks.

### Economy, sharing, navigation, and ad policy follow-up — 2026-08-23

This section supersedes the earlier monetization descriptions in this historical report. Room schema v7 adds the last credited local date/epoch day and grants 30 Coins only when startup observes a strictly later day. First completions still grant 10 Coins. Hint and Undo now cost 30 Coins each, and Skip Level costs 50 Coins; when the balance is below the applicable cost, one successful, idempotently recorded Google Mobile Ads reward callback authorizes that action directly without changing Coins. Skips still grant no completion reward or stars.

Every fifth first completion is now consumed and attempted immediately after the solved-board transaction, with Next/Back retained only as an interruption-recovery check. Completing a rewarded ad persists a 60-second protection timestamp that blocks standard interstitials across process recreation and treats clock rollback as protected. App Open inventory is requested on foreground entry after consent/initialization, starts a persisted one-hour cooldown on a confirmed impression, discards cached ads after four hours, waits at most five seconds in the foreground, and shares the full-screen presentation lock. Production configuration now requires the AdMob app ID plus App Open, standard-interstitial, and rewarded unit IDs.

The completion UI renders a visible animated emoji layer in addition to confetti and fixed flower accents. Share captures the settled celebration screen as a cache-only PNG and sends it with the package-specific Play Store URL. “Continue growing” carries Home as its explicit game return destination, preventing the earlier multi-screen Back traversal.

Latest host verification passed 47 debug unit tests, `lintDebug`, debug assembly, minified R8 release assembly, and `git diff --check` under Gradle 9.5.0 / Android Gradle Plugin 9.1.0. On the Samsung SM-S928B, Google's official rewarded test creative completed through the real SDK reward callback; a standard-interstitial diagnostic attempted immediately afterward returned `UNAVAILABLE`, confirming the active post-reward protection rather than opening another full-screen ad. A connected v6→7 migration execution and the remaining manual UI/device matrix remain pending.

The privacy policy and release worksheets now disclose the App Open/rewarded/interstitial placements, local timing records, celebration screenshot sharing, and four-ID production gate. The publisher contact is Naimish Gupta, `naimish.app@gmail.com`, Sandi, Hardoi, 241403, Uttar Pradesh.

### Animal Companions — 2026-08-23

Animal Companions now replace the Seed's presentation after 10 Campaign completions while preserving the engine's stable `seedCell`, every certified definition, replay, star rule, and solver result. The launch catalog contains 48 code-drawn characters: 12 mammals, 12 birds, 12 insects, six garden/reptile creatures, and six aquatic creatures. Meadow Mouse is free; roster positions 2–48 cost 100–2,400 Coins in 50-Coin increments. Purchases are permanent local unlocks, switching an owned Companion is free, and no Companion purchase has an ad fallback.

The adaptive collection screen supplies family filters, animated previews, ownership/selection/price states, and accessible descriptions. Family- and trait-specific Canvas anatomy avoids remote art and platform-emoji variability. Bounce, hop, prowl, waddle, flap, hover, scuttle, crawl, swim, and pulse profiles animate the collection, board travel, and completion sheet; Reduced motion settles them. The pre-unlock Seed remains on Campaign levels 1–9, and the level-10 first-completion result announces the new feature.

Room schema v8 adds one selected-Companion row and a primary-keyed permanent-unlock table through the explicit non-destructive 7→8 migration. Coin debit, unlock insertion, and selection share one Room transaction. The exported v8 schema and v1/v2→8 migration sources compile. A dedicated real-Room instrumentation test covers successful atomic purchase and insufficient-Coin rollback; it could not run because `adb devices -l` found no attached Samsung device or emulator in this pass.

Host verification passed 49 unit tests with zero failures/errors/skips, Android instrumentation compilation/packaging, `lintDebug` with zero errors, debug APK assembly, and minified R8 release APK assembly. Both public HTML pages returned zero Nu HTML validator messages, and `git diff --check` passed. On Samsung SM-S928B, five focused tests passed: every one of the 48 Companion renderers, the atomic Room purchase/insufficient-Coin rollback, both v1/v2→8 migration chains, and the retained v3→4 index migration.

The first device build exposed an Android-only crash when Continue growing drew Meadow Mouse: the catalog's conventional 32-bit ARGB values had been passed to Compose's packed `ULong` wide-color constructor, producing `IllegalArgumentException: Invalid ID: 38` from `ColorSpace`. The renderer now explicitly uses the ARGB `Int` constructor. A regression test cycles all 48 catalog entries through real Android Canvas; the preserved-data Continue growing flow now opens Campaign level 31 with Meadow Mouse and no fatal Logcat event.

The corrected debug APK is 19,739,601 bytes (SHA-256 `f224d406e9e67c945e3a4df8c011cc06506be628ce3cae4bfb14f6747efad94e`); the unsigned optimized offline release APK is 2,684,370 bytes (SHA-256 `728396fb5e3658c70e83c1d61a6fee2e036e2e79ab0cf9f548e83b9bb143dc7e`); and the instrumentation APK is 1,158,215 bytes (SHA-256 `744ae0430f44abd716b5c8b93c6e8a54ee9e14a6c7cbddb52644f346ac2b63f8`). Full manual visual-quality review, TalkBack/font-scale/RTL coverage, and the wider device matrix remain pending rather than claimed.

### Companion Roster Expansion — 2026-08-24

The catalog now contains 60 code-drawn Companions: 14 mammals, 14 birds, 14 insects, nine garden/reptile creatures, and nine aquatic creatures. The 12 additions are Acorn Squirrel, Alpine Goat, Lavender Crane, Cherry Cockatoo, Poppy Cricket, Iris Weevil, Rain Salamander, Vine Snake, Velvet Spider, Sunset Koi, Tidepool Crab, and Moon Jellyfish. They are appended at roster positions 49–60, preserving every existing ID and Coin price; the new prices continue the 50-Coin progression from 2,450 through 3,000.

New tail and snout traits plus dedicated snake, spider, crab, and jellyfish Canvas anatomy keep the expansion visually distinct. All 49 host unit tests, Android-test Kotlin compilation, `lintDebug`, and `git diff --check` pass. The existing connected renderer test automatically covers all 60 entries, but no device or emulator was attached for this pass, so the expanded roster's connected Canvas and manual visual checks remain pending.

### 100-Companion Roster — 2026-08-24

A further 40 Companions extend the catalog to 100, adding eight mammals, eight birds, eight insects, eight garden/reptile creatures, and eight aquatic creatures. The additions occupy roster positions 61–100, so all previous IDs and prices remain stable while the 50-Coin progression continues from 3,050 through 5,000. The final family totals are 22 mammals, 22 birds, 22 insects, 17 garden/reptile creatures, and 17 aquatic creatures.

The renderer now recognizes wool, facial masks, snouts, and tusks, with dedicated anatomy for the Mushroom Slug, Clover Worm, Moss Alligator, Pearl Seal, Arctic Walrus, Emerald Pufferfish, Sapphire Manta, and Sunstar Starfish. All 49 host unit tests, Android-test Kotlin compilation, `lintDebug`, and `git diff --check` pass. The dynamic connected renderer test will cover all 100 entries when a device or emulator is attached; connected and manual visual checks remain pending for this pass.

### Companion Rewarded-Ad Unlocks — 2026-08-24

Every locked Companion card now offers a separate rewarded-ad button in ad-supported builds. A verified callback contributes exactly 50 Coins of visible, persistent credit to that Companion's stable ID without modifying the player's general Coin balance. Credit cannot transfer to another Companion. Reaching the catalog price unlocks and selects the animal automatically; a partial balance reduces the Coins charged if the player later pays the remainder. Thus the 100-Coin Moon Bunny unlocks after two completed ads, or after one ad plus 50 regular Coins.

The implementation reuses the existing append-only `ad_grants` ledger with `companion:<id>` reward labels, so no Room schema change is required. Reward transaction insertion, final ownership insertion, and selection execute atomically; consumed transaction IDs make callbacks globally idempotent. Unavailable, failed, incomplete, and duplicate rewards apply no credit. The collection UI shows accumulated/total ad credit, the remaining Coin price, loading and failure states, and localized accessibility descriptions.

All 51 host unit tests pass, including full-ad unlock, duplicate-callback rejection, cross-Companion isolation, unchanged global balance, and partial-ad-plus-Coin payment. Android-test Kotlin compilation, `lintDebug`, and `git diff --check` pass. Three focused connected tests pass on Samsung SM-S928B / Android 16: all 100 catalog entries render through real Android Canvas, the original atomic Coin purchase/rollback path passes, and scoped idempotent rewarded-ad credit unlocks in one Room transaction.

### Multiple App Themes — 2026-08-24

Settings now separates color theme from brightness appearance. Players can choose Living Garden, Rose Garden, Moonlit Pond, or Golden Meadow, then independently use System, Light, or Dark appearance. Every theme defines coordinated Material 3 application surfaces and board-specific Stone, Bloom, Bud, Seed, and paper roles in both light and dark variants. The existing high-contrast setting remains an explicit palette-independent override.

The selected `ThemePalette` is stored in DataStore by a stable enum name, defaults safely to Living Garden for existing installs or unknown future values, and updates the complete Compose hierarchy immediately. The selector uses text, selected-chip state, and a color swatch, wraps for larger fonts, and keeps 48dp minimum touch targets.

Host verification passes 53 unit tests with zero failures/errors/skips, Android-test Kotlin compilation, and `lintDebug`. New tests lock the four stored names, verify distinct application and board tokens across every light/dark theme pair, and enforce at least 4.5:1 contrast for their semantic text/background pairs. A Samsung SM-S928B visual pass confirms immediate full-screen switching, clean two-row selector layout, light/dark rendering, and persisted restoration; 200%-font, TalkBack, and the complete device matrix remain release work rather than claimed automation.

### One-Hour App Open Cooldown — 2026-08-24

The durable App Open cooldown is now 3,600,000 ms (one hour) between confirmed impressions instead of two hours. Eligibility is still measured from the persisted confirmed-impression timestamp; unavailable or failed ads do not start the cooldown, and backward clock movement remains ineligible. Existing installs need no data migration because the stored timestamp format is unchanged.

The policy regression test explicitly locks the duration to 3,600,000 ms and verifies the pre-boundary, exact-boundary, never-shown, and backward-clock cases. All 53 host unit tests, Android-test Kotlin compilation, `lintDebug`, and `git diff --check` pass.

### Standard Dialog Signing and Checked-In Ad IDs — 2026-08-24

The four public production AdMob identifiers now live directly in `app/build.gradle.kts`; debug retains Google test inventory and the offline `release` variant remains SDK/network-free. The production validation task still rejects malformed or test identifiers without requiring user-level Gradle properties.

Android Studio’s standard **Generate Signed Bundle / APK** flow is now first-class: the release gate recognizes the IDE’s injected keystore path, alias, and transient passwords while retaining the optional protected-environment path for CI. No signing password or private-key material is stored in the repository or a Gradle properties file. The hosted privacy-policy URL is also a checked-in public build constant.

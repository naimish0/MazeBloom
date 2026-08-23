# Architecture

The repository was an existing minimal one-module Compose app. To avoid disproportionate Gradle/module churn, the prompt’s conceptual modules map to packages and tools:

| Conceptual responsibility | Actual owner |
| --- | --- |
| Pure `game-core` | `app/src/main/java/com/rameshta/mazebloom/core` |
| Local `data` | `app/src/main/java/com/rameshta/mazebloom/data` |
| Android app/UI | `ui`, `services`, and `MainActivity` in `:app` |
| JVM `content-tools` | `tools/*.mjs`; never packaged into the APK |

The core package has no Android, Compose, persistence, clock, ad, or billing dependency. It owns immutable models, catalog value types, validation, transitions, replay encoding, BFS, bounded difficulty analysis, D4 transforms, strict similarity checks, and SplitMix64.

`MazeBloomViewModel` is the controller/composition boundary. It exposes immutable `StateFlow` UI state, owns the current `GameSession`, rejects input during transition presentation, runs current-state hints on `Dispatchers.Default`, and writes progress through `ProgressRepository`. Navigation passes stable Garden, Chapter, and level IDs. A game launch also stores one explicit back destination, so System Back returns directly to the originating screen instead of replaying intermediate screens. Composables render and emit actions; they never decide collision or collection.

The simulation completes before presentation. `TransitionResult` drives Seed travel, ordered Bud removal, and the post-stop Bloom sequence. Lifecycle interruption cannot change the already-decided `afterState`.

`BundledContentRepository` reads only the small root catalog at startup. Chapter JSON is SHA-256 checked and decoded on `Dispatchers.IO`; concurrent requests share one deferred load, and an access-ordered LRU holds at most three decoded 20-level Chapters. Missing/corrupt shards return typed failures and never alter progress. Daily's bounded 120-level pool is loaded separately. The 100-level Auto Progressive asset is decoded, root-checked, request-coalesced, and cached only after Campaign completion opens it.

`RoomProgressRepository` stores sparse, indexed progress rows, contiguous Campaign state, exact replay-backed active attempts (including definition checksum), Daily selection/history/streak, Endless skip markers, coin balance, pending-interstitial state, consumed rewarded-ad transaction IDs, and the last local day granted daily Coins. Completion, reward, spend, cadence, and Daily changes are transactional. Schema version 7 and explicit non-destructive 1→2→3→4→5→6→7 migrations are exported under `app/schemas`; v3 adds generated-history tables, v4–v6 evolve the certified-generation baseline/indexes, and v7 adds rollback-resistant daily-grant bookkeeping. A one-time compatibility importer preserves the previous SharedPreferences data. Player settings live in Preferences DataStore. Static boards remain assets rather than database rows. Interfaces and `InMemoryProgressRepository` keep migration/progression/economy behavior unit-testable; incompatible active attempts are discarded without clearing unrelated progress.

Optional advertising is isolated behind `AdsGateway` and `ConsentGateway`. Debug and `production` source sets bind the Google adapter; the offline `release` source set binds no-op implementations and excludes the Google dependencies. The adapter resolves UMP before initialization, preloads all three ad formats, serializes full-screen presentation, persists the two-hour App Open cooldown and 60-second post-reward interstitial protection, and fails open for gameplay. `MainActivity` owns foreground App Open requests; the ViewModel owns rewarded action authorization and consumes an every-fifth-completion interstitial immediately after solved-board persistence.

Celebration sharing captures the settled activity content into a cache-only PNG, exposes it through the non-exported `FileProvider`, and sends it with the Play Store URL through Android's chooser. The receiving app receives temporary read access only after the player chooses it.
# Endless Garden v1.1 extension

Endless generation follows `Compose -> MazeBloomViewModel -> EndlessRepository -> EndlessLevelGenerator / production solver and fingerprints -> Room v7`. `EndlessLevelGenerator` is pure deterministic Kotlin and runs on `Dispatchers.Default`; all Room and bundled-content reads run off the main thread. `RoomEndlessRepository` owns a mutex for ordered generation, verifies the pinned baseline, requests no more than three future definitions, and exposes typed snapshots to immutable UI state.

Static Campaign/Daily/starter definitions remain assets. Room holds only sparse player progress plus authoritative generated records, certificates, uniqueness transforms, checkpoints, segments, and the append-only history root. Stable legacy `progressive-*` identifiers remain intact while presentation uses Endless Garden.

Generation checkpoints are written after deterministic 256-attempt windows and restore the attempt boundary, survivor quotas, rejection counts, baseline, segment, target ordinal, and starting history root. A single repository mutex preserves ordinal order. Storage is checked separately from deterministic identity: a conservative serialized-registry ceiling bounds the decoded/indexed working set below 128 MiB, each accepted payload is sized before commit, and insufficient operational free space returns `STORAGE_BLOCKED` without advancing the history root or candidate.

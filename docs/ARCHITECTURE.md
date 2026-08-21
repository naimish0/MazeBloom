# Architecture

The repository was an existing minimal one-module Compose app. To avoid disproportionate Gradle/module churn, the prompt’s conceptual modules map to packages and tools:

| Conceptual responsibility | Actual owner |
| --- | --- |
| Pure `game-core` | `app/src/main/java/com/rameshta/mazebloom/core` |
| Local `data` | `app/src/main/java/com/rameshta/mazebloom/data` |
| Android app/UI | `ui`, `services`, and `MainActivity` in `:app` |
| JVM `content-tools` | `tools/*.mjs`; never packaged into the APK |

The core package has no Android, Compose, persistence, clock, ad, or billing dependency. It owns immutable models, validation, transitions, replay encoding, BFS, bounded difficulty analysis, D4 transforms, duplicate checks, and SplitMix64.

`MazeBloomViewModel` is the controller/composition boundary. It exposes immutable `StateFlow` UI state, owns the current `GameSession`, rejects input during transition presentation, runs current-state hints on `Dispatchers.Default`, and writes progress through `ProgressRepository`. Composables render and emit actions; they never decide collision or collection.

The simulation completes before presentation. `TransitionResult` drives Seed travel, ordered Bud removal, and the post-stop Bloom sequence. Lifecycle interruption cannot change the already-decided `afterState`.

`SharedPreferencesProgressRepository` stores versioned primitives for progression, exact replay-backed active attempts, settings, daily selection/history, and streak. Interfaces and `InMemoryProgressRepository` keep behavior unit-testable. This is intentionally smaller than introducing Room/DataStore/KSP into the existing one-module shell; future schema changes must add explicit key/schema migration rather than destructive clearing.

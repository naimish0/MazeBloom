# CODEX ONE-GO IMPLEMENTATION PROMPT — MAZEBLOOM 2,000-LEVEL CAMPAIGN

You are Codex working inside the current repository. Implement MazeBloom end to end in one continuous execution.

Do not stop after analysis, return only a plan, or wait for approval between normal implementation phases. Inspect first, make the changes, generate and certify the content, run the available verification, fix failures caused by your work, and finish with an evidence-based completion report.

This request authorizes local source-code, test, asset, configuration, and documentation changes required for the game. It does not authorize commits, pushes, branches, pull requests, store uploads, production releases, use of live credentials, or destructive operations.

The approved product direction is:

> Swipe through every Bud. Every path you leave Blooms into a wall.

The player should feel that they are growing the maze they later use—not destroying the board or moving a generic gravity ball.

---

## 1. Execution rules

### 1.1 Repository safety

- Read every applicable AGENTS.md before making changes.
- Inspect git status, relevant diffs, repository structure, Gradle configuration, existing documentation, and current tests before editing.
- Treat existing code, architecture, names, dependencies, application ID, SDK levels, versioning, and design conventions as the source of truth.
- Preserve all unrelated and uncommitted user changes.
- Do not commit, push, create a branch, open a pull request, alter Git identity, or modify remotes.
- Do not use destructive Git commands.
- Use rg and targeted inspection instead of repeatedly scanning the repository.
- Never overwrite a working subsystem merely because a different architecture is preferred.
- Do not perform broad dependency, Kotlin, AGP, Gradle, Compose, SDK, or architectural upgrades unless the current project cannot build without the change.
- Use JDK 17 for Gradle unless the repository explicitly requires another supported JDK.
- Never add secrets, signing files, service-account files, production ad IDs, API keys, or credentials to source control.
- Do not weaken tests, lint, release checks, or compiler warnings to make the build appear green.

### 1.2 Existing versus empty repository

If an Android project already exists:

- Integrate MazeBloom into the existing project with the smallest coherent change set.
- Preserve its application ID, signing configuration, navigation, dependency injection, persistence, module layout, theme, and build conventions unless a change is necessary and documented.
- Use package boundaries instead of adding modules when new modules would cause disproportionate churn.
- First establish that the repository or a clearly designated module is actually intended for MazeBloom. Never repurpose or overwrite an unrelated application. In a monorepo whose conventions clearly support another app, add an isolated MazeBloom application module without disturbing other apps; if the target is ambiguous or mutually incompatible, stop after the audit and report the exact blocker.

If the repository is empty:

- Bootstrap a production-capable Android project using Kotlin, Gradle Kotlin DSL, Jetpack Compose, Material 3, the installed Android SDK, and stable compatible dependencies.
- Use a minimal multi-module structure described later.
- Use a clearly documented provisional application ID such as com.example.mazebloom. Do not claim that the package or MazeBloom product name is cleared for Play release.
- Keep the application ID and player-facing product name easy to replace before release.

### 1.3 Autonomy and blockers

- Make reasonable implementation decisions from this contract and existing repository conventions.
- Do not ask for routine clarification.
- If production AdMob IDs, billing product configuration, signing credentials, an emulator, or an approved analytics provider are unavailable, implement the safe adapter/test/no-op path, keep gameplay fully functional, document the missing external configuration, and continue.
- Stop only for a genuine permission, destructive-action, missing-source, or mutually incompatible repository blocker.
- Never fabricate a passing result, human playtest, production credential, store configuration, or manual QA result.

### 1.4 One-go completion behavior

Maintain an internal implementation plan and execute these dependencies in order:

1. Repository audit and build baseline.
2. Pure deterministic game core.
3. Exact solver and certification.
4. Generator, duplicate rejection, and content tooling.
5. Certified campaign and daily content.
6. Persistence and repositories.
7. Compose gameplay and navigation.
8. Progression, hints, daily challenge, sharing, settings.
9. Ads, billing, consent, and analytics abstractions.
10. Accessibility, animation, audio, haptics, and responsive polish.
11. Automated tests, build verification, smoke checks, and documentation.

Do not leave a later phase as a plan when it can be implemented locally.

### 1.5 Mandatory deliverables versus configuration-dependent integrations

The following are mandatory in this execution and may not be replaced by TODOs or mock screenshots:

- Repository audit and a preserved build baseline.
- Pure rules engine, exact solver, deterministic generator, certification, and exactly 2,000 bundled campaign levels. Preserve the approved first 100 and add levels 101–2,000.
- A certified bundled daily pool of at least 120 levels, with 365 as the target when the bounded generation budget succeeds.
- Local persistence, campaign progression, gameplay, Undo, Restart, current-state hints, Daily Bloom, Collection, Settings, replay, and local sharing.
- Garden/paper visual treatment, responsive layout, reduced motion, high contrast, direction controls, semantics, lightweight audio/haptics, tests, documentation, and actual verification evidence.
- Ads, consent, billing, and analytics interfaces with no-op/fake implementations and policy tests so the app works with every external service absent.

The following production adapters are conditional on compatible dependencies and valid external configuration already being available: live ad/consent SDK wiring, Play Billing product wiring, an approved analytics SDK, release signing, and store submission. Do not install or guess a vendor merely to claim completion. When an integration is unavailable, keep its player-facing production entry disabled, use the no-op/fake path, document the exact setup still required, and do not let it block the mandatory offline game.

All mandatory work is one-go work. “Conditional” means dependent on credentials, console configuration, an approved vendor, or signing material that cannot truthfully be created in this repository; it is not permission to omit the corresponding abstraction, safe fallback, tests, or documentation.

---

## 2. Product definition

### 2.1 Working identity

- Working title: MazeBloom.
- Public name status: not cleared.
- Platform: Android.
- Orientation: portrait-first, responsive on phones and tablets.
- Play style: one hand, short deterministic puzzles.
- Connectivity: completely playable offline.
- Accounts/backend: none.
- Primary fantasy: grow a small garden maze and awaken every Bud.
- Product promise: every swipe creates the collision geometry required by later swipes.

### 2.2 Product principles

- Understandable within approximately 10 seconds.
- Easy to control, difficult to master.
- Deterministic and trustworthy.
- Every newly accepted campaign level 101–2,000 and every bundled Daily level is globally unique against the frozen first 100 and the complete accepted corpus under the strict geometric, behavioral, structural, and near-similarity contract; rotations, reflections, cosmetic mutations, and repeated decision grammar do not count as new content. A disclosed collision wholly inside the immutable legacy prefix is the only grandfathered case and never permits a matching new level.
- Fast experimentation with free Undo and Restart.
- Difficulty from consequences of the base rule, not from a catalogue of gimmicks.
- Warm, tactile, restorative, and creative—not neon space, generic sci-fi, or a glowing-ball gravity maze.
- Global-ready and text-light.
- Offline-first with no login, lives, energy, artificial waiting, or pay-to-win.

### 2.3 Strict core scope

Implement only these gameplay objects:

- Floor: traversable.
- Stone: permanent static blocker.
- Bud: collectible on a traversable cell.
- Seed Spirit: the single player-controlled piece.
- Bloom: a permanent blocker created from cells departed by the Seed Spirit.

Supported campaign board sizes:

- 5×5.
- 6×6.

Do not add:

- Multiple Seed Spirits.
- Colored-goal matching.
- Portals, switches, doors, keys, magnets, spikes, enemies, moving tiles, one-way gates, conveyors, teleporters, breakable walls, or temporary Bloom.
- Device tilt, physics-engine movement, timing, reflex requirements, ricochet aiming, or precision dragging.
- Directly drawing walls.
- Command queues or a separate Play/Execute phase.
- Replayed past actors or echo clones.
- 7×7 or larger campaign boards.
- Runtime-generated campaign levels.
- A level editor, user-generated content, multiplayer, chat, accounts, cloud saves, or global leaderboards.
- Coins, boosters, lives, energy, loot boxes, subscriptions, or gameplay purchases.
- A heavy game engine when Compose Canvas is sufficient.

---

## 3. Exact deterministic gameplay rules

### 3.1 Static level model

A LevelDefinition contains at least:

~~~text
id
schemaVersion
contentVersion
rulesVersion
width
height
staticWallMask
startCell
initialBudMask
gardenId
chapterId
chapterOrderWithinGarden
campaignOrder
generatorVersion
generatorSeed
solverVersion
certificationProfileVersion
fingerprintVersion
~~~

Validation requirements:

- Campaign and daily boards must be square and exactly 5×5 or 6×6. Do not emit 5×6, 6×5, or any other shape in this version.
- Masks must contain no out-of-bounds bits.
- The start must be in bounds and not a Stone.
- Buds must be in bounds and not Stones.
- The start cell must not contain a Bud.
- A level contains between 1 and 7 Buds. Only the earliest tutorial levels may contain one Bud; normal campaign levels should contain 2–7.
- IDs are stable and unique.
- Campaign `chapterId` is globally stable, such as `garden-02/chapter-03`; it is never a bare Chapter number. New campaign definitions checksum `gardenId`, `chapterId`, `chapterOrderWithinGarden`, and `campaignOrder`. If an immutable level 1–100 payload has a legacy `chapter` field, preserve that field and its original checksum byte-for-byte, and map it to the global `chapterId` only in the catalog compatibility layer.

### 3.1.1 Coordinates, masks, and external serialization

Use one coordinate convention everywhere:

- Origin is the top-left cell `(x=0, y=0)`.
- `x` increases to the right and `y` increases downward.
- `cellIndex = y * width + x` in row-major order.
- The internal packed bit for a cell is `1L shl cellIndex`.
- Direction deltas are `UP=(0,-1)`, `RIGHT=(1,0)`, `DOWN=(0,1)`, and `LEFT=(-1,0)`.

Packed masks are an internal implementation detail. In versioned JSON, serialize `stones`, `buds`, and any persisted `bloom` as strictly ascending arrays of non-negative integer cell indices and serialize `start` as one integer cell index. Do not expose signed decimal `Long` masks as the authoritative external schema. Reject duplicates, unsorted arrays when canonical input is required, unknown required enum values, and out-of-range cells.

Serialize every 64-bit generator/base seed as exactly 16 lowercase hexadecimal digits representing its unsigned bit pattern, never as a JSON floating-point/decimal number. Serialize saturated path counts that may exceed JavaScript’s safe integer range as canonical base-10 strings plus the explicit overflow flag.

Canonical JSON/checksum encoding must be explicitly versioned, UTF-8, stable-key-order, whitespace-independent, locale-independent, and contain no unordered collection. Hash only normalized logical fields listed by the checksum contract; never hash file paths, timestamps, elapsed durations, machine details, review notes, or JSON object iteration order.

### 3.2 Dynamic state

GameState contains at least:

~~~text
seedCell
bloomMask
remainingBudMask
moveCount
status: ACTIVE | SOLVED | DEAD
~~~

Use immutable state. Undo stores exact prior logical states or equivalent immutable snapshots.

Create initial state through the same rules layer: `SOLVED` only if validation explicitly permits a no-Bud fixture (campaign/daily validation does not), `DEAD` if Buds remain and no direction can move, otherwise `ACTIVE`.

### 3.3 Player action

The only gameplay action is one cardinal swipe:

- UP
- RIGHT
- DOWN
- LEFT

Optional accessibility direction buttons must invoke exactly the same action and transition function.

### 3.4 Transition resolution

If the supplied state is not `ACTIVE`, return `INVALID` with no mutation. Otherwise, for every direction:

1. Start from seedCell.
2. Inspect the adjacent cell in the selected direction.
3. Stop when the next cell is outside the board, a Stone, or a Bloom that existed before this swipe began.
4. Otherwise append the current cell to departedCells, move into the next cell, and collect any Bud entered.
5. Continue until blocked.
6. If no movement occurred, return INVALID. Change no state, collect no Bud, create no Bloom, and do not increment moveCount.
7. After the Seed Spirit reaches its final resting cell, atomically add every departed cell to bloomMask.
8. The final resting cell does not become Bloom.
9. Bloom created during this swipe must not block this same swipe.
10. Increment moveCount exactly once for a valid swipe.
11. If every Bud has been collected, finish the complete slide and Bloom commit, then mark the state SOLVED.
12. Crossing the final Bud does not stop the Seed Spirit early.
13. If Buds remain and no direction produces a valid move, mark the state DEAD. `DEAD` means locally immobile; a still-mobile state that has no solution is instead reported by analysis as `DOOMED` and does not require a new runtime GameStatus.
14. The pure transition function remains callable and deterministic regardless of animation state. The ViewModel/controller ignores new player actions while a prior result is being presented.
15. The ViewModel/controller ignores further gameplay input once status is SOLVED.

### 3.5 Transition result and events

Model gameplay as:

~~~text
GameState + Direction -> TransitionResult
~~~

TransitionResult must expose:

~~~text
beforeState
direction
traversedPath
departedCells
newlyCollectedBuds
newBloomCells
afterState
orderedEvents
~~~

Use documented events such as:

~~~text
SHIFT_STARTED
SEED_ENTERED(cell)
BUD_COLLECTED(cell)
BLOOM_CREATED(departedCells)
LEVEL_COMPLETED
STATE_STABLE
INVALID_MOVE
DEAD_STATE
~~~

For a valid move, `traversedPath` is the ordered list of cells entered: it excludes the starting cell and includes the final resting cell. `departedCells` is the ordered list containing the starting cell followed by every entered cell except the final resting cell. On every validated state, `newBloomCells` is exactly `departedCells`; encountering overlap is a state-invariant error, not something to hide by filtering.

Event order is exact. Emit `SHIFT_STARTED`; then, for every cell in `traversedPath`, emit `SEED_ENTERED(cell)` followed immediately by `BUD_COLLECTED(cell)` when that cell held a remaining Bud; then emit one `BLOOM_CREATED(departedCells)` after the final stop; then emit exactly one terminal event: `LEVEL_COMPLETED`, `DEAD_STATE`, or `STATE_STABLE`. An invalid move emits only `INVALID_MOVE`, with empty paths and no state mutation.

The simulation decides the entire result before rendering begins. UI animations consume TransitionResult; animation timing, animation locks, or callbacks must never determine game rules.

### 3.6 Required invariants

- Repeating an identical transition from an identical state produces an identical result.
- Every valid move adds at least one Bloom cell.
- Bloom never disappears during a forward transition; Undo may restore an earlier exact snapshot.
- remainingBudMask only shrinks.
- Stones never change.
- The Seed Spirit never occupies a Stone or Bloom.
- The final resting cell is not Bloom.
- A pre-existing blocker is never crossed.
- The logical state graph is acyclic because every valid unfinished move strictly grows bloomMask.
- Rendering coordinates, floating-point collision, frame time, animation progress, device density, and wall-clock time never enter simulation decisions.

### 3.7 Undo, Restart, and resume

- Undo restores the exact previous state, including Seed position, Bloom, remaining Buds, move count, and status.
- Undo is unlimited and always free.
- Restart restores the pristine initial state immediately and is always free.
- If the state is DEAD, prominently offer Undo and Restart.
- Persist the active attempt so backgrounding or process death does not destroy meaningful progress.
- Restore active state when its stable level ID, exact level-definition checksum, and rules version are compatible. A campaign-manifest version bump that only appends levels 101–2,000 must not reset an unchanged level 1–100 attempt. Otherwise safely restart only that incompatible attempt and explain nothing technical to the player.

---

## 4. Core architecture

### 4.1 Modules for an empty project

Use this minimal graph when bootstrapping:

- :app
  - Android application, Compose UI, navigation, ViewModels, dependency wiring, sharing, audio/haptics, ads, billing, consent, and analytics adapters.
- :game-core
  - Pure Kotlin/JVM models, transition engine, validation, replay, exact solver, fingerprints, generator contracts, and difficulty analysis.
  - No Android, Compose, database, ads, analytics, or wall-clock dependency.
- :data
  - Android persistence, Room/DataStore implementations, migrations, and repository implementations.
  - May depend on :game-core; :game-core must not depend on it.
- :content-tools
  - JVM command-line tooling for generation, solving, analysis, rendering, certification, deduplication, and manifest creation.
  - Never packaged into the app.

Do not create a module per screen, a redundant domain module, or speculative framework layers. In an existing project, preserve its module structure and enforce these boundaries with packages if that is safer.

For an existing repository, map each conceptual responsibility above to the actual module/package that owns it before editing. Do not assume the literal module paths exist, and do not add duplicate modules solely to satisfy the example names. Record the conceptual-to-actual mapping in `docs/IMPLEMENTATION_REPORT.md` and use the mapped modules’ real Gradle tasks for verification.

### 4.2 UI architecture

- For an empty project or an existing Compose project, use Kotlin, Jetpack Compose, and Material 3.
- For an existing Views/XML product that is already intended for MazeBloom, preserve its working UI stack and Material equivalent unless a small isolated Compose adoption is demonstrably the lowest-risk integration. Do not force a broad Compose/toolchain migration merely to match this prompt; keep the same state/event and rules/rendering separation in either UI stack.
- Use the repository’s existing dependency injection solution. If none exists, use constructor injection and one small application composition root; do not add a DI framework for a small object graph.
- Use ViewModels and coroutines. Prefer StateFlow; preserve an existing lifecycle-aware observable convention when replacing it would create unnecessary churn.
- Each screen has an immutable UiState and explicit user actions/events.
- Composables or Views render state and emit actions; they do not contain game rules, solver logic, generator logic, persistence, ad policy, or billing behavior.
- Navigation passes stable IDs, not full boards or arbitrary serialized objects.
- Inject clocks and random sources.
- No global mutable gameplay singleton.
- Keep app startup small; do not initialize optional SDKs on the critical path before consent/configuration is resolved.

### 4.3 Core types

Implement cohesive equivalents of:

~~~text
CellIndex
CellMask
Direction
LevelDefinition
LevelValidationResult
GameState
GameStatus
TransitionResult
GameEvent
MazeBloomRules
Replay
ReplayChecksum
SolverRequest
SolutionReport
DifficultyReport
CertificationReport
GeneratorProfile
CampaignManifest
CampaignCatalog
GardenDescriptor
ChapterDescriptor
CampaignShardDescriptor
UniquenessProfile
GlobalUniquenessReport
~~~

Use a CellMask abstraction. A packed Long implementation is appropriate for 5×5 and 6×6, but do not make external level schemas or public contracts impossible to extend beyond 64 cells later.

### 4.4 Lazy campaign repository

Do not expose the 2,000-level corpus as one eager `List<LevelDefinition>`. Provide cohesive equivalents of:

~~~text
observeCampaignCatalog()
observeGardenSummary(gardenId)
observeChapterSummary(chapterId)
loadLevel(levelId or campaignOrder)
prefetchNextChapter(currentChapterId)
~~~

- Keep catalog/value models in pure core, Android asset I/O/caching in data, and loading/error/navigation state in app.
- Parse shards on an I/O dispatcher, coalesce concurrent requests for the same shard, propagate cancellation, and optionally prefetch only the next Chapter.
- Maintain an LRU of at most three decoded Chapter shards. Runtime-verify a shard against its catalog digest once per cache entry/process before serving it.
- Missing/corrupt assets return a typed error without crashing or deleting progress. Gameplay never downloads a missing shard because all campaign content is bundled offline.
- Cold start may read the small root catalog and current/required Chapter only; it must not open, hash, parse, or instantiate all 100 shards.

---

## 5. Exact solver

### 5.1 BFS state and transition

All actions cost one swipe. Implement exact breadth-first search using the production transition function.

State key:

~~~text
StateKey(seedCell, bloomMask, remainingBudMask)
~~~

Do not include moveCount or derived status in the transposition key. Never omit remainingBudMask.

Requirements:

- Expand directions in one fixed order: UP, RIGHT, DOWN, LEFT.
- Ignore invalid swipes.
- Deduplicate converged states.
- The first solved depth is the exact optimal move count.
- Finish the entire first solved depth when counting optimal solutions.
- Accumulate shortest-path counts in a signed 64-bit counter. Start the root count at 1; on any checked addition above `Long.MAX_VALUE`, store exactly `Long.MAX_VALUE` and set a sticky `overflow=true`. Use the same rule independently for each near-optimal depth.
- Keep one singular lexicographically canonical optimal parent chain using direction order `UP < RIGHT < DOWN < LEFT`; all “canonical replay” metrics/signatures refer to this replay only.
- Return BUDGET_EXCEEDED rather than UNSOLVABLE when a hard cap prevents proof.
- Standard BFS deduplication is correct for shortest distance and optimal-path counts, but it must not be reused to count longer solutions. Count solutions at exact depths `optimal+1` and `optimal+2` with a separately bounded layered dynamic program keyed by `(StateKey, depth)`, using the defined saturating counts and never expanding solved states. If its diagnostic cap is reached, record the explicit deterministic status `NOT_COMPUTED_BUDGET`; do not undercount and do not reject an otherwise certified level solely for this optional metric.

Support:

- SHORTEST: optimal depth, one canonical solution, number of optimal solutions, state expansions, and deterministic budget status.
- AUDIT: full reachable-state analysis within explicit budgets for difficulty and traps.
- FROM_CURRENT_STATE: bounded asynchronous solving for a valid hint after the player has deviated from the stored initial solution.
- SHORTEST_PROBE: generator-only prefilter with the lower section 7 cap. It is never publication proof; every selected level must subsequently pass full SHORTEST and AUDIT. A probe cap hit rejects only that candidate and is not labeled UNSOLVABLE.

Never use the initial stored solution as a hint if the player’s current state no longer lies on it.

The pure core does not read a clock. App/content-tool wrappers may time a solve for non-canonical diagnostics, but elapsed duration is not part of `SolutionReport`, equality, serialization, acceptance, or checksums.

### 5.2 Solver budgets

Initial hard publication budgets:

- Campaign board area: at most 36 cells.
- SHORTEST: at most 250,000 expanded states, 300,000 discovered states, and 200,000 frontier records.
- AUDIT: at most 500,000 expanded states, 550,000 discovered states, 500,000 frontier records, and 2,000,000 retained directed edges. Compute the minimum-Bloom-stop proof over this complete retained graph without a second graph expansion.
- FROM_CURRENT_STATE hint solve: at most 100,000 expanded states, 125,000 discovered states, and 75,000 frontier records; return `BUDGET_EXCEEDED` on the cap and support coroutine cancellation.
- Optional near-optimal layered counting: at most 750,000 distinct `(StateKey, depth)` entries; its `NOT_COMPUTED_BUDGET` status is diagnostic and not a publication rejection.
- No recursion, unbounded queues, or unbounded predecessor retention.
- Content tools run with a maximum 512 MiB heap and at most four concurrent full-certification workers. Generation/probe workers may use bounded additional concurrency only while total configured heap/queue caps remain enforced.
- Reject candidate levels when any required SHORTEST, AUDIT, or minimum-Bloom-stop proof exceeds its cap. Do not conflate this with the optional near-optimal diagnostic cap.
- Expansion, discovered-state, frontier-record, retained-edge, attempt-count, and output-size caps are authoritative. Treat wall-clock measurements as diagnostic because CI machines vary.

Reference targets:

- Gameplay transition p95 below 1 ms on representative low-end hardware.
- Shortest solve p50 below 25 ms and p95 below 150 ms in a release JVM reference run.
- Diagnostic target: no accepted campaign level above 500 ms on a named reference run.
- Diagnostic target: certification of one changed 100-level Garden below 90 seconds and full 2,000-level automatic certification below 15 minutes with bounded parallelism.

These timing values are non-blocking targets unless the repository defines and runs a named, reproducible benchmark environment with fixed hardware/runtime, warm-up, sample count, and threshold policy. CI must fail on authoritative expansion/memory caps, not on ad hoc wall-clock variance. Report timing honestly, including environment and sample method.

Do not run full generation or audit BFS during ordinary gameplay. Runtime hints may use bounded FROM_CURRENT_STATE solving on a background dispatcher with cancellation and a safe failure state.

---

## 6. Human-oriented difficulty analysis

Do not label difficulty from optimal moves or solver expansions alone.

DifficultyReport must retain raw metrics including:

- Optimal move count.
- Bud count.
- Buds collected per optimal move.
- Reachable states.
- Physically dead states/ratio and mobile-but-unsalvageable `doomedStateCount`/ratio.
- Valid-action distribution.
- Meaningful decision count.
- Earliest meaningful branch.
- Forced-move ratio.
- Number of optimal solutions.
- Number of solutions within optimal + 1 and optimal + 2, using bounded counting.
- Earliest wrong-branch depth and wrong-branch penalty distribution.
- Boundary, Stone, and Bloom-assisted stop counts.
- Minimum Bloom-assisted stops required by any solution.
- Canonical-replay Bloom dependency depth: moves between creating a Bloom and later using it as a stopper.
- Canonical-replay maximum chained Bloom dependency.
- Average and maximum slide length.
- Required optimal moves that collect no Bud.
- Greedy traps: moves collecting the most immediate Buds but preventing an optimal result.
- Optional heuristic-agent traces/solve indicators only when their evaluation function, lookahead, tie-breaks, trial set, and denominator are fully versioned.
- Visual load proxies such as occupied ratio, open corridors, remaining Buds, and simultaneous visible choices.

A Bloom-assisted stop means movement stops because the next cell is Bloom created by an earlier move. Compute `minimumBloomAssistedStopsAcrossAnySolution` with an exact bounded 0–1/Dijkstra-style search whose edge cost is one for a Bloom-assisted stop and zero otherwise, or with an equivalently exact constrained proof. A positive minimum proves the mechanic is required; do not infer this from only the canonical solution.

Build the reachable directed graph during bounded AUDIT and determine which states can reach any solved state by reverse reachability. `DEAD` counts active states with zero valid actions. `DOOMED` counts active states with one or more valid actions but no path to a solved state. Keep those metrics distinct.

Some dependency metrics require path history that is intentionally absent from `StateKey`. Compute creator-to-use distance and chained dependency only along the singular canonical optimal replay, where the creation move of each Bloom cell is known, and name the fields accordingly (for example `canonicalReplayMaxCreatorUseDistance`). Do not present them as minima across all solutions unless an exact history-aware search actually proves that result.

Use these exact metric definitions:

- A valid action is a direction producing a non-INVALID transition from an ACTIVE state.
- Collapse directions that produce the same successor `StateKey` before counting choices.
- A forced state is a reachable, solvable, nonterminal state with exactly one distinct valid successor. `forcedMoveRatio = forcedStateCount / reachableSolvableNonterminalStateCount`, or zero when the denominator is zero.
- Precompute exact remaining distance-to-solve for solvable states from the bounded audit graph.
- A meaningful decision is a reachable, solvable, nonterminal state with at least two distinct valid successors where either their exact remaining distances differ or at least one successor is DOOMED. Report both the number of such states and the earliest depth from the start.
- At a meaningful decision, a wrong successor is DOOMED or has an exact remaining distance greater than the minimum among solvable successors. Its penalty is the distance difference or the explicit `DOOMED` sentinel. Earliest wrong-branch depth is the minimum start depth of a state with such a successor.
- A greedy action maximizes Buds collected by the next transition, with the fixed direction order breaking ties. A greedy trap is a state where that chosen successor is DOOMED or has a strictly worse exact remaining distance than another valid successor.
- Qualitative notions such as “plausible,” “believable,” “convincing,” “visual clarity,” and predicted human difficulty are diagnostics/review fields, never automatic certification failures.
- Heuristic-agent fields are optional non-canonical diagnostics and are excluded from hard gates and checksums until an exact policy is added to a later certification-profile version.

### 6.1 Provisional bands

Use versioned, configurable numeric profiles rather than burying weights in UI code. The initial automatic hard gates are:

- Tutorial
  - Optimal moves 1–4 and 1–2 Buds.
  - Forced-move ratio may be up to 1.0.
  - First required Bloom-assisted stop by level 4.
- Easy
  - Optimal moves 3–7, at least one meaningful decision, at least one required Bloom-assisted stop, and forced-move ratio at most 0.85.
- Normal
  - Optimal moves 5–10, at least two meaningful decisions, at least one required Bloom-assisted stop, and forced-move ratio at most 0.75.
- Hard
  - Optimal moves 7–13, at least three meaningful decisions, `minimumBloomAssistedStopsAcrossAnySolution >= 2`, at least one DOOMED or greedy-trap successor, and forced-move ratio at most 0.70.
- Expert
  - Optimal moves 9–16, at least four meaningful decisions, `minimumBloomAssistedStopsAcrossAnySolution >= 2`, canonical replay creator-to-use distance at least 3, and forced-move ratio at most 0.65.
- Master
  - Optimal moves 11–20, at least five meaningful decisions, `minimumBloomAssistedStopsAcrossAnySolution >= 3`, canonical replay creator-to-use distance at least 4, and forced-move ratio at most 0.60.

All numeric fields, denominator rules, and band thresholds belong to a versioned certification profile and are recorded in the manifest. A level must satisfy its assigned profile’s hard numeric gates, but qualitative copy in the chapter descriptions is never a hidden gate. These labels remain provisional predictions. Preserve metrics so future human solve time, attempts, Undo, Restart, hint use, abandonment, and subjective ratings can recalibrate the model. Never call a level human-certified without human data.

---

## 7. Deterministic generator

### 7.1 Stable generation

- Implement SplitMix64 explicitly with 64-bit wraparound and unsigned right shifts: advance by `0x9E3779B97F4A7C15`, mix with xor-shift 30 and multiplier `0xBF58476D1CE4E5B9`, then xor-shift 27 and multiplier `0x94D049BB133111EB`, then xor-shift 31. Pin golden vectors so platform/library RNG changes cannot alter content.
- Persist generator version and seed.
- The same generator version, profile, and seed must produce byte-identical logical output.
- Give each candidate an independently derived seed.
- Results must not change with thread count: evaluate candidates independently, then sort and select deterministically.
- Preserve every Garden 1 seed. Introduce an immutable `generationNamespaceVersion` that changes only when seed-space semantics intentionally change; a catalog append or metadata-only corpus-version bump must not change it. For each nonempty `(gardenOrder, difficulty, boardSize)` stratum in Gardens 2–20, derive `candidateSeed` as the first eight SHA-256 digest bytes interpreted as an unsigned big-endian 64-bit value over length-prefixed canonical UTF-8 fields `"mazebloom-candidate"`, generation-namespace version, generator version, garden order, difficulty, board size, and candidate ordinal. Use that bit pattern to initialize SplitMix64 inside the candidate. This isolates streams and makes adding one stratum incapable of perturbing another.
- Each extension stratum has exactly two candidate windows: ordinals `0..<5000` and `5000..<10000`. Constructive beam search uses width at most 64, history depth at most 24, and at most 4,096 construction-node expansions per raw candidate. Within each window, send at most 200 exact-prefilter survivors to `SHORTEST_PROBE` with caps of 50,000 expansions, 60,000 discovered states, and 30,000 frontier records; send at most 80 survivors to the required board-only AUDIT proof. Thus one stratum retains at most 160 fully audited, unassigned `CandidateBoard` values. A candidate proof contains geometry, seed, solution/difficulty evidence, and fingerprints but no campaign ID/order/Chapter and is not a final level certificate. Daily generation uses its separately versioned namespace and ordinals `0..<500000`.
- With eight nonempty strata per new Garden, the extension’s absolute maxima are 1,520,000 raw candidates, 60,800 probe solves, and 24,320 full candidate audits before final selection. Exceeding or extending these ranges requires an explicit generator/profile version change; never do it silently.
- Build audited candidate pools independently and sort strictly by unsigned candidate ordinal ascending; the derived seed is recorded metadata, never a secondary or alternate ordering key. A single reducer visits campaign slots 101–2,000 in ascending campaign order, assigns the slot's stable ID/Garden/Chapter/order to the first unused board from its stratum that passes the complete higher-priority campaign uniqueness registry, and then creates the final checksum-bearing `LevelDefinition` and final certificate. The lower campaign order always wins. If assignment-time certification or uniqueness fails, reject that board and continue the same ordered pool. Parallelism, cache hits, and worker completion order cannot affect selection.
- One canonical level JSON is capped at 64 KiB, one packaged certification summary at 24 KiB, one nonpackaged detailed diagnostic record at 128 KiB, one Garden candidate-diagnostics report at 256 MiB, and the global manifest/uniqueness index at 64 MiB; exceedance is a named rejection/failure rather than truncation. Never package full reachable graphs or verbose per-state traces for all 2,000 levels.
- Use the fixed solver/attempt/frontier/edge/output budgets in this contract and record them in the versioned profile. Record wall-clock time only in non-canonical diagnostic output.
- Elapsed time, timestamps, worker completion order, host paths, machine identity, locale, and thread count must never affect candidate output, acceptance, ordering, fingerprints, selection, manifests, golden files, or checksums.

### 7.2 Constructive pipeline

Do not use unconstrained random wall placement as the primary generator.

For each candidate:

1. Sample board dimensions, Stone topology, start, and candidate seed.
2. Run a bounded forward beam search through legal swipe histories.
3. Prefer histories with turns, meaningful choices, and later stops against earlier Bloom.
4. Select Bud cells from cells entered across the construction history.
5. Exclude the start and Stones.
6. Distribute Buds across several moves; reject non-tutorial boards where all Buds lie on one trivial line.
7. Replay the construction history to establish at least one candidate solution.
8. Solve the pristine level with exact BFS; never assume the construction history is optimal.
9. Run AUDIT difficulty analysis.
10. Compute geometric, dynamic, and near-duplicate fingerprints.
11. Certify, cluster, select, or reject with a precise reason.
12. Persist seed, versions, full level, optimal replay, metrics, fingerprints, and checksums.

Do not maximize Stone occupancy. Static density must preserve readable open space and choices. Difficulty must come from Bud order and self-created Bloom geometry, not from turning every level into a forced corridor.

### 7.3 Generator reporting

Report at least:

- Candidates attempted.
- Schema-invalid count.
- Unsolvable count.
- Trivial count.
- Required solver/proof-budget rejection count; report optional near-optimal diagnostic-cap hits separately.
- Difficulty-profile rejection count.
- Exact-definition/D4-geometric, complete-dynamic, structural-signature, hard-near, and review-similarity rejection counts separately.
- Global pair/alignment comparisons performed and zero-collision acceptance result.
- Forced-corridor rejection count.
- Certified yield.
- Median and p95 solver expansions, plus separately labeled non-canonical duration diagnostics.
- Accepted distribution by board size, Bud count, difficulty, optimal moves, branching, and Bloom dependency.
- Smallest reproducing candidate ordinal within the configured attempted range, plus its fixed-width hexadecimal derived seed, for every failure class. Do not claim a global minimum outside that range.

---

## 8. Duplicate and similarity rejection

### 8.1 Exact geometric duplicate

Canonicalize the complete tuple across all eight D4 transforms of the supported square board:

~~~text
width
height
static walls
start
buds
~~~

For an `N×N` board, define identity `(x,y)`, clockwise rotation `R(x,y)=(N-1-y,x)`, and vertical-axis mirror `M(x,y)=(N-1-x,y)`; the D4 set is `I, R, R², R³, M, M∘R, M∘R², M∘R³`. Transform every Stone, Bud, start, Seed, and Bloom cell with the same function. Transform directions as vectors: under `R`, `UP→RIGHT→DOWN→LEFT→UP`; under `M`, `LEFT↔RIGHT` while `UP` and `DOWN` remain unchanged; compositions follow in the same order. Unit-test all cells/directions and inverse/round-trip behavior. Use the lexicographically smallest canonical external serialization as the canonical form. An identical canonical fingerprint is a hard rejection.

Do not collapse BFS states under symmetry unless a transform is proven to be an automorphism of that exact level.

### 8.2 Dynamic duplicate

For the singular lexicographically canonical optimal replay, build a structural signature containing per move:

~~~text
direction
slide length
buds collected
stop source: boundary | stone | bloom
bloom creator-to-use distance
new Bloom count
remaining Bud count
~~~

Under each D4 transform, transform the replay's directions with the board and serialize the complete structural record; choose the lexicographically smallest transformed serialization before hashing. Also build a deterministic dynamic fingerprint from the complete bounded AUDIT transition graph: mark the distinguished initial node, sort normalized reachable `StateKey` records and their valid `(direction, successor, event summary)` edges under each D4 transform, choose the lexicographically smallest encoding, and hash it. Exclude elapsed time and traversal/discovery order. An identical complete dynamic fingerprint is a hard rejection even when irrelevant unreachable Stone cells differ.

Also derive `solutionGrammarFingerprint` from the direction sequence transformed under each D4 element plus its invariant ordered pickup-count and stop-source patterns, then choose the lexicographically smallest transformed grammar serialization. Both an identical full structural signature and an identical solution-grammar fingerprint are global hard rejections, regardless of geometric distance.

### 8.3 Strict near-duplicate rejection

For levels of the same board size, evaluate all eight D4 alignments and use the minimum weighted mask distance:

~~~text
maskDistance = popcount(stonesA XOR stonesB)
             + 2 * popcount(budsA XOR budsB)
             + 2 * (startA == startB ? 0 : 1)
~~~

Collect every alignment tied for minimum `maskDistance`; do not choose one arbitrarily. For each alignment, transform level B's complete canonical-optimal direction sequence into level A's coordinate frame before comparison. The pickup pattern is the ordered Bud counts collected per move and the stop pattern is the ordered boundary/Stone/Bloom stop sources; those two patterns are invariant under D4.

A pair is a hard uniqueness collision if any condition holds under any minimum-distance alignment:

- `maskDistance <= 6`, regardless of solution behavior.
- `maskDistance <= 12` and at least one of direction, pickup, or stop pattern is identical.
- `maskDistance <= 18` and at least two of those three patterns are identical.
- The full canonical optimal-replay structural signature or `solutionGrammarFingerprint` from section 8.2 is identical, regardless of mask distance.

A pair is a review-similarity collision when it is not already hard but either `maskDistance <= 12`, or `maskDistance <= 18` with exactly one identical behavior pattern. Campaign and Daily generation run in `strictUniqueness=true`: review-similarity collisions also reject the later candidate automatically. Do not keep them merely because a human might eventually approve them.

The normalized difficulty vector `[optimalMoves/20, budCount/7, min(meaningfulDecisionCount,20)/20, forcedMoveRatio, min(minimumBloomAssistedStopsAcrossAnySolution,5)/5, doomedStateRatio]` is retained for diversity/pacing reports, but similar difficulty alone is desirable and is never treated as duplication.

For reporting ties, choose the alignment with the most matched patterns, then the lexicographically smallest transformed canonical serialization, then fixed transform order `I < R < R² < R³ < M < M∘R < M∘R² < M∘R³`. Emit both IDs, chosen transform, mask components, matched patterns, collision tier, and reason.

### 8.4 Global uniqueness registry

Maintain one versioned uniqueness registry covering all 2,000 campaign levels and every bundled Daily level.

- Priority is immutable campaign levels 1–100, then lower campaign order, then lower Daily pool order. The higher-priority published item always wins; regenerate the later candidate.
- Exact definition, D4 geometric, complete dynamic, solution grammar, canonical replay structural, hard near, and strict review-similarity fingerprints/reasons are release-blocking.
- Compare every unordered campaign pair exhaustively: `C(2000,2) = 1,999,000` pairs and at most 15,992,000 D4 alignments. Do not use probabilistic LSH or sampling for the final uniqueness proof.
- For each Daily candidate, compare against the complete campaign and previously accepted Daily pool using the same rules.
- Hash maps may accelerate exact fingerprints, but confirm canonical payload equality after a hash collision. Near checks still use the exact pairwise rule.
- Produce a `campaignUniquenessRoot` after the 2,000 campaign levels are fixed, covering campaign records only. After the lower-priority Daily pool is regenerated against that fixed campaign, produce a separate `globalUniquenessRoot` covering all accepted campaign and Daily records. A valid extension has zero collision pairs involving any level 101–2,000 or Daily entry. If the newly strengthened profile detects a pair entirely inside the immutable approved 1–100 prefix, record it separately as a grandfathered legacy exception; it may never justify accepting a matching new level and must not be misreported as fixed.
- Each uniqueness root hashes the uniqueness-profile/fingerprint versions, exact expected and performed pair/alignment counts, accepted bundled-item fingerprint records, and accepted-item collision records only. A collision record contains both stable IDs and priority/order keys, chosen transform, mask-distance components, matched behavior patterns, collision tier, and stable reason code; sort by `(leftPriorityKey, rightPriorityKey, collisionTier, transformId, reasonCode)`. On a valid release, the only stored collision records are disclosed legacy-prefix exceptions. Rejected generator-candidate collisions and timing/statistical diagnostics belong in a separate non-canonical generation report and never enter either uniqueness root.
- Any replacement candidate is rechecked against the entire higher-priority registry before filling its slot. Levels 101–2,000 and Daily entries have no collision waiver, similarity allowlist, or manual override. Never resolve a collision by renaming, reordering, rotating, reflecting, changing only decoration, or weakening a threshold.

These rules guarantee uniqueness under every specified geometric, behavioral, structural, and near-similarity test. Subjective “feels different to every human” cannot be mathematically proven, so the separate human sample still records perceived similarity; never misstate that limitation.

### 8.5 Canonical certification checksum

Use lowercase SHA-256 over a documented canonical UTF-8 payload containing only:

- Schema, content, rules, solver, generator, certification-profile, and fingerprint versions.
- Stable level ID, campaign order, globally stable Garden/Chapter IDs and local Chapter order, plus generator seed; for an immutable legacy record, hash its original compatibility fields exactly as published.
- Width, height, sorted Stone indices, start index, and sorted Bud indices.
- Exact optimal move count, canonical direction replay, optimal-path saturated count/overflow flag, each optional near-optimal count or its `NOT_COMPUTED_BUDGET` sentinel, authoritative expansion/entry counts, the certification profile’s explicitly enumerated canonical non-timing metrics, geometric/dynamic/solution-grammar/structural fingerprints, and uniqueness-profile version. Exclude optional heuristic-agent and human-review fields.

Sort campaign manifest entries by campaign order then stable ID. Exclude elapsed time, timestamps, file paths, host/JVM details, thread count, worker completion order, human-review fields, and all non-canonical diagnostics. A checksum change requires the appropriate explicit version bump and regenerated golden manifest.

Separate roots so appending content cannot masquerade as changing a published level:

~~~text
definitionHash = canonical logical level fields
solutionHash = definitionHash + rulesVersion + optimalMoves + canonicalReplay
publishedCertificateHash = definitionHash + solutionHash + the certificate/profile versions and canonical metrics/fingerprints published with that level
auditCertificateHash = definitionHash + solutionHash + current solver/profile/fingerprint/uniqueness versions + current canonical audit metrics/fingerprints
contentShardRoot = shard metadata + 20 ordered runtime definitions + their published solutions/certificates
auditShardRoot = 20 ordered current audit-certificate records, stored outside the immutable runtime content shard
campaignContentRoot = catalog/schema versions + legacyPrefixCompatibilityRoot + ordered 100 contentShardRoots + pacingScheduleHash
campaignUniquenessRoot = campaign-only accepted fingerprint records + campaign comparison counts + grandfathered legacy collision records
campaignAuditRoot = current audit versions + ordered 100 auditShardRoots + campaignUniquenessRoot
campaignRoot = campaignContentRoot + campaignAuditRoot
dailyContentRoot = ordered Daily definitions + published solutions/certificates, excluding campaign/global roots
dailyAuditRoot = ordered current Daily audit-certificate records
globalUniquenessRoot = accepted Campaign+Daily fingerprint records + complete comparison counts + grandfathered legacy collision records
releaseContentRoot = campaignRoot + dailyContentRoot + dailyAuditRoot + globalUniquenessRoot
~~~

Use length-prefixed canonical UTF-8 fields and lowercase SHA-256. Persist `parentCampaignRoot` for an append/update. A new audit/profile version may change audit roots without rewriting definitions, published solutions/certificates, or content shards. A catalog append must not change any unchanged prior `definitionHash`, `solutionHash`, `publishedCertificateHash`, or `contentShardRoot`. There is no hash cycle: campaign selection/rooting never depends on Daily; `releaseContentRoot` is created only after the fixed campaign and replacement Daily pool are complete.

---

## 9. Campaign content — exactly 2,000 levels

### 9.1 Preservation contract and hierarchy

Generate, certify, bundle, and expose exactly 2,000 campaign levels. The approved first 100 are the immutable foundation; this task adds 1,900 levels rather than replacing them.

- If certified levels 1–100 already exist, preserve them byte-for-byte and behavior-for-behavior: IDs, order, boards, Buds, optimal moves, canonical replays, stars, hashes, checksums, difficulty metadata, generator seeds, and certification results must not drift.
- If the repository is empty and levels 1–100 do not yet exist, create them from the Garden 1 baseline in section 9.2, pin their manifest/hashes, and then treat that result as immutable while generating levels 101–2,000.
- Present levels 1–100 as Garden 1 without rewriting an existing authoritative payload merely to add grouping metadata; grouping may live in the new campaign index.
- New candidates are deduplicated against all earlier accepted levels, including the locked first 100. A collision rejects the new candidate, never the baseline level.
- Introduce `campaignCatalogVersion` separately from per-level `contentVersion`. Bump the catalog version for the extension while retaining every unchanged level’s checksum/version.
- Pin `legacyPrefixDefinitionRoot` over the ordered definition hashes, `legacyPrefixSolutionRoot` over ordered published solution hashes/replays, and `legacyPrefixCertificateRoot` over ordered published certificate bytes for levels 1–100. Pin `legacyPrefixCompatibilityRoot` over those three roots plus every ordered frozen ID, campaign order, difficulty/star-threshold metadata, generator version/seed, and other immutable static field promised above. Fail before generation if any root drifts. If the stricter new uniqueness profile finds an internal pair inside that locked prefix, record `GRANDFATHERED_LEGACY_COLLISION` without rewriting it; index both legacy entries so no level 101–2,000 or Daily candidate can match either one.
- Support historical solver/certification-profile versions. A newer audit may add a versioned certificate/fingerprint for legacy comparison, but it must not rewrite the legacy logical definition hash or old certificate.
- Existing completion, stars, best moves, replays, and compatible active attempts for levels 1–100 survive the update. A player who had completed level 100 has level 101 unlocked after migration.
- Do not hand-author 1,900 source constants or separate Kotlin objects. Generate, certify, serialize, shard, index, and bundle the extension through the deterministic content pipeline.

Use this fixed hierarchy:

~~~text
20 Gardens
└── 5 Chapters per Garden
    └── 20 Levels per Chapter

1 Garden   = 100 levels
1 Chapter  = 20 levels
20 Gardens = 100 Chapters = 2,000 levels
~~~

Mapping is exact:

~~~text
gardenOrder             = floor((campaignOrder - 1) / 100) + 1
chapterOrderWithinGarden = floor(((campaignOrder - 1) % 100) / 20) + 1
levelOrderWithinChapter  = ((campaignOrder - 1) % 20) + 1

Garden 1  = levels 1–100
Garden 2  = levels 101–200
...
Garden 20 = levels 1901–2000
~~~

Use stable zero-padded IDs for new content, such as `campaign-0101` through `campaign-2000`; preserve existing IDs if Garden 1 already uses another stable scheme. Decorative Garden names, palettes, and plant motifs are localizable presentation metadata only and never affect rules, solving, difficulty, fingerprints, or checksums.

### 9.2 Garden 1 baseline and onboarding

Garden 1 retains the approved first 100 structure:

- Chapter 1 — Sprout, levels 1–20
  - 5×5.
  - Levels 1–5 are handmade onboarding using Tutorial profile; levels 6–20 use Easy.
- Chapter 2 — Roots, levels 21–40
  - 5×5 using Normal profile.
- Chapter 3 — Branches, levels 41–60
  - 5×5 using Hard profile.
- Chapter 4 — Canopy, levels 61–80
  - Levels 61–70 use Hard; levels 71–80 use Expert.
  - Mixed 5×5/6×6 with at least five of each size.
- Chapter 5 — Full Bloom, levels 81–100
  - 6×6.
  - Levels 81–95 use Expert; levels 96–100 use Master.

Onboarding teaches through play:

1. One visible swipe collects one Bud.
2. One swipe crosses multiple Buds; make the Bloom trail unmistakable.
3. Demonstrate that old Bloom blocks a later route.
4. Require using an earlier Bloom as the stopping wall.
5. Offer a plausible mistake, then highlight free Undo contextually.

Do not add a long tutorial screen, dialogue, or instruction carousel. Show a lightweight gesture cue only while level 1 is idle; permanently remove it after the first valid action. Levels 101–2,000 never replay onboarding or use Tutorial profile.

For an empty repository or a prefix created by this contract, Garden 1’s exact profile totals are `Tutorial=5, Easy=15, Normal=20, Hard=30, Expert=25, Master=5`. Preflight an existing approved prefix against those metadata totals. If its retained certified labels differ, preserve them and treat its actual immutable profile totals as `legacyProfileTotals`; do not relabel levels merely to satisfy this table.

### 9.3 Exact mixed-difficulty schedule for Gardens 2–20

Every new Garden uses exactly this quota:

| Difficulty | Per Garden | Across 19 new Gardens |
| --- | ---: | ---: |
| Easy | 16 | 304 |
| Normal | 28 | 532 |
| Hard | 28 | 532 |
| Expert | 20 | 380 |
| Master | 8 | 152 |
| **Total** | **100** | **1,900** |

When Garden 1 matches the baseline, the complete campaign totals are exactly `Tutorial=5, Easy=319, Normal=552, Hard=562, Expert=405, Master=157`. For a pre-existing approved prefix with different retained labels, compute complete totals as `legacyProfileTotals + (Tutorial=0, Easy=304, Normal=532, Hard=532, Expert=380, Master=152)` and report them; the 1,900-level extension quotas never change.

Use these exact per-Chapter quotas in every Garden 2–20:

| Chapter | Easy | Normal | Hard | Expert | Master | Total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 6 | 7 | 4 | 2 | 1 | 20 |
| 2 | 4 | 7 | 5 | 3 | 1 | 20 |
| 3 | 2 | 6 | 7 | 4 | 1 | 20 |
| 4 | 3 | 4 | 6 | 5 | 2 | 20 |
| 5 | 1 | 4 | 6 | 6 | 3 | 20 |

Use `E=Easy`, `N=Normal`, `H=Hard`, `X=Expert`, and `M=Master`. Apply these exact 20-level pacing templates to every new Garden:

~~~text
Chapter 1: E E N E N H N E N H X E N H N E N H X M
Chapter 2: E N H N E N H X N H E N H X E N N H X M
Chapter 3: E N H N H X N H E N H X H N H X N H X M
Chapter 4: E E N H X M E N H X H N H X H N H X X M
Chapter 5: E N H X M N H X H N H X M N H X H X X M
~~~

Certification must prove the schedule and these pacing invariants:

- Every Chapter ends with a Master level; Garden-local level 100 is the Garden capstone.
- Every non-final Master is immediately followed by Easy or Normal.
- Difficulty may rise by at most one adjacent band per level; it may drop by multiple bands for deliberate relief.
- No difficulty band appears more than twice consecutively and no two Master levels are adjacent.
- Garden-local levels 1 and 61 are Easy breathers; the first three levels of every new Garden are `Easy, Easy, Normal`.
- Each aligned 10-level half-Chapter contains at least one Easy/Normal level and at least one Hard/Expert/Master level.
- Labels come from exact certified profile gates, not desired slot names. If a generated level misses its scheduled profile, reject it and continue that slot’s bounded stream; never relabel or substitute another band silently.
- A Hard/Expert/Master level must earn that band through meaningful decisions and Bloom dependencies, not length, Stone density, or a forced corridor.

### 9.4 Exact board-size mix for Gardens 2–20

Every new Garden contains exactly 30 5×5 and 70 6×6 levels:

| Difficulty | 5×5 | 6×6 | Total |
| --- | ---: | ---: | ---: |
| Easy | 12 | 4 | 16 |
| Normal | 14 | 14 | 28 |
| Hard | 4 | 24 | 28 |
| Expert | 0 | 20 | 20 |
| Master | 0 | 8 | 8 |
| **Total** | **30** | **70** | **100** |

Across levels 101–2,000 this is exactly 570 new 5×5 and 1,330 new 6×6 boards. Within each `(Garden, difficulty)` group, order scheduled occurrences by Chapter then level; with zero-based `rank`, assign 5×5 when `ceilDiv((rank+1)*fiveByFiveQuota,totalQuota) > ceilDiv(rank*fiveByFiveQuota,totalQuota)` and assign the remainder 6×6. Define integer `ceilDiv(a,b)=(a+b-1)/b`. This makes assignment deterministic, evenly spread, and starts each Garden’s first Easy/Normal occurrence on 5×5.

Continue using only Seed Spirit, Floor, Stone, Bud, and permanent Bloom. Do not add new gameplay objects or larger boards merely to manufacture 2,000 levels.

### 9.5 Per-level and whole-campaign certification

Every campaign level must:

- Pass schema validation and exact required solver/proof budgets.
- Store exact optimal moves and the singular canonical replay.
- Reproduce its solution and checksums after serialization round trip.
- Satisfy its scheduled difficulty profile and board size.
- Pass the complete strict uniqueness registry against every higher-priority campaign level. After the 2,000-level campaign is fixed, regenerate/revalidate the bundled Daily pool against it; any conflicting Daily entry loses and is replaced. Exact definition, D4 geometry, complete dynamics, solution grammar, canonical replay structure, hard near, and review-similarity collisions are all rejected.
- Avoid unintended one-swipe/all-Buds-on-one-line solutions outside levels 1–3.
- Require at least one Bloom-assisted stop after level 5.
- Satisfy its profile’s exact forced-move-ratio gate.
- Have no irrelevant open cell: every non-Stone cell is the start or appears in at least one valid transition’s `traversedPath` in the complete AUDIT graph.
- Remain visually distinguishable at high expected Bloom occupancy; treat broader beauty or “misleading decoration” judgments as human review, not a hidden build gate.

Whole-campaign certification must verify:

- Exactly 2,000 unique stable IDs and every campaign order from 1 through 2,000 exactly once.
- Exactly 20 Gardens, 100 Chapters, and 20 levels per Chapter with correct mapping.
- Garden 1’s pinned legacy hashes/checksums have zero drift.
- Every fixed profile, pacing, and board-size quota for Gardens 2–20. Validate Garden 1 against its frozen approved metadata: the section 9.2 baseline when created by this contract, otherwise its pinned `legacyProfileTotals` and retained board-size/chapter metadata. Compute the global totals from that actual prefix plus the fixed extension totals.
- No repeated definition, geometric, complete dynamic, solution-grammar, or singular canonical-replay structural fingerprint involving any level 101–2,000; disclose only a grandfathered pair, if any, wholly inside the immutable first 100.
- Zero collision pairs involving levels 101–2,000 or Daily content, including cross-Garden/cross-shard pairs; separately disclose any grandfathered pair wholly inside levels 1–100.
- A valid sorted campaign collision audit and `campaignUniquenessRoot` covering exhaustive comparison of all 1,999,000 campaign pairs; after Daily is selected, a valid combined audit and `globalUniquenessRoot` cover every campaign-versus-Daily and Daily-versus-Daily pair.
- Pinned level hashes, optimal moves, replays, published and current audit-certificate hashes, content/audit-shard hashes, and root-manifest checksums.

Generation and selection are bounded by versioned streams. If any Garden/profile stream exhausts its range without filling its exact quota, generation and certification fail with the stream identity, rejection counts, smallest reproducing ordinals/seeds, and remaining slots. Never fill the quota with transforms, relax gates, alter Garden 1, treat soft review as approval, or substitute unverified content.

### 9.6 Sharding and runtime content index

Do not parse or retain 2,000 full boards at startup. Bundle:

~~~text
campaign/campaign-manifest.json
campaign/garden-01/chapter-01.json
...
campaign/garden-20/chapter-05.json
~~~

- The root manifest stores lightweight Garden/Chapter/level metadata, order, difficulty, board size, per-shard SHA-256, and aggregate checksum; it does not duplicate complete boards.
- Each of the 100 Chapter shards contains exactly 20 complete level definitions sorted by campaign order plus their compact certification summaries.
- A repository loads a level directly by stable ID or loads one Chapter shard on demand. Keep a bounded LRU cache of at most three decoded Chapter shards and never instantiate unopened boards.
- Production assets exclude verbose audit graphs, generator rejection traces, and human worksheets. Those remain in development reports.
- Decorative Garden visuals are generated/drawn from small theme tokens; do not bundle 2,000 prerendered board images.
- `verifyBundledContent` hashes and certifies these exact source-set assets, and packaging depends on that verification.

### 9.7 Stars, unlocking, and migration

- Unlock levels sequentially within a Chapter.
- Completing Chapter level 20 unlocks the next Chapter; completing Garden level 100 unlocks the next Garden.
- Completing level 100 on an upgraded installation unlocks Garden 2/level 101 without replaying Garden 1.
- Stars never gate progression.
- 1 star: complete.
- 2 stars: complete within optimal + 2 moves.
- 3 stars: complete in the certified optimal move count.
- Preserve best moves and best replay. Never revoke a star because of a later campaign-manifest extension.
- Completing level 2,000 shows a finite campaign-complete state; do not silently add an unverified endless mode.
- `Next Level` from an ordinary completion opens the next global level. At Chapter-local level 20 it completes the Chapter and opens the next Chapter’s level 1; at Garden-local level 100 it completes the Garden and opens the next Garden’s level 1 after the short celebration. Level 2,000 has no `Next Level` action and opens the campaign-complete state.

### 9.8 Human-review truthfulness at 2,000-level scale

Code can automatically solver-certify all 2,000 levels but cannot prove human fun or perceived difficulty.

- Generate per-Garden and aggregate machine reports plus a human-review worksheet with level ID, predicted band, metrics, reviewer, attempts, time, Undo, Restart, hint use, perceived difficulty, duplicate concern, decision, and notes.
- The extension base sample is exactly 10 levels per Garden 2–20—two each from Easy, Normal, Hard, Expert, and Master—for 190 levels total. Within each `(Garden,difficulty)` group choose the two smallest SHA-256 values of `reviewSamplingVersion|levelId`. Add, without replacing the base sample, all levels 1–100 lacking retained real approval, both endpoints of the 20 globally closest accepted non-colliding pairs under the strict similarity ordering, any metric outlier, and every Chapter/Garden capstone. This nearest-pair addition is capped at 40 distinct levels; break distance ties by the ordered pair's SHA-256 so sample size cannot grow accidentally.
- Each sampled extension level receives two independent reviews; disagreement receives a third. Record perceived similarity to the nearest machine-reported neighbors as well as clarity, fun, and difficulty.
- Human release-readiness targets are zero rule/replay discrepancies, at least 90% approval across the extension base sample, at least 80% within every difficulty band, and no unresolved high-severity perceived-duplicate concern. Missing those targets holds production promotion but does not falsify automatic certification.
- Automated certification and human approval are separate fields. Do not fabricate playtests or approval.
- Do not describe the 2,000-level campaign as production difficulty/fun validated until representative review and closed-test evidence actually exist.
- Automatically certified content may be bundled for development/internal/closed testing while human review remains pending, provided that status is explicit.

---

## 10. Content tooling

Provide JVM CLI commands or equivalent Gradle tasks for:

~~~text
solve
analyze
generate
certify
certify-campaign
dedupe
replay
render
generate-campaign
generate-daily-pool
verify-legacy-prefix
generate-garden
certify-garden
certify-campaign-incremental
certify-campaign-full
verify-bundled-content-fast
verify-bundled-content
~~~

Tooling must support:

- Compact text import/export.
- Versioned JSON import/export.
- Human-readable board rendering with coordinates.
- Step-through canonical replay.
- Projection of path, Buds collected, departed cells, and new Bloom.
- BFS state inspection.
- Reachability/dead-state heat maps.
- Generator rejection statistics.
- Difficulty histograms.
- Adjacent-level difficulty-spike report.
- D4 transform viewer.
- Nearest-duplicate comparison.
- Global collision audit and comparison for any two IDs.
- Per-Garden/shard generation, certification, and report regeneration.
- Reproduction by seed.
- Machine-readable JSON/CSV reports for CI.

`certifyCampaignFull` (with `certifyCampaign` as an optional alias) fails on any automatic problem:

- Invalid schema.
- Unsolvable level or required SHORTEST/AUDIT/minimum-Bloom proof budget exceeded.
- Checksum drift without content-version change.
- Missing replay/optimal data.
- Any exact-definition, D4-geometric, complete-dynamic, solution-grammar, structural, hard-near, or strict review-similarity collision involving level 101–2,000 or Daily content. A pair wholly inside the checksum-locked first 100 is reported only as a grandfathered legacy exception.
- Missing required Bloom-assisted stop.
- Broken campaign/Garden/Chapter order, shard range/hash, duplicate ID, fixed quota, pacing template, or board-size schedule.
- Legacy prefix drift.
- Difficulty outside hard acceptance limits.

Hard acceptance limits mean the versioned numeric rules in this contract/profile, not subjective prose. In strict uniqueness mode, both hard and review-similarity collision classes fail; similar difficulty vectors alone do not. Qualitative fun/beauty concerns remain in the human report and do not masquerade as automatic proof.

Cache a current audit certificate only under `SHA-256(definitionHash | rulesVersion | solverVersion | certificationProfileVersion | fingerprintVersion | uniquenessProfileVersion)`. Verify the cached checksum before reuse. Never overwrite the frozen published certificate with that cache entry. Cache presence, file timestamps, and worker order never affect canonical results.

`certifyCampaignIncremental` must:

1. Verify `legacyPrefixCompatibilityRoot` and every previously published unchanged content-shard root.
2. Recompute local certification for only new/changed Chapter shards.
3. Exhaustively compare every changed level with all prior campaign/Daily entries and all peers in the changed set.
4. Rebuild sorted global uniqueness records/roots and check pacing across shard boundaries.
5. Leave every unchanged definition, published solution/certificate, and content shard byte-identical. A newer solver/profile writes separate audit certificates/audit-shard roots and never mutates those published bytes.

When no `parentCampaignRoot` exists, run incremental certification in explicit initialization mode: treat every shard as new, skip only the impossible prior-root comparison, build the baseline roots, and report `INITIAL_CATALOG` rather than pretending an earlier publication existed. When a parent exists, initialization mode is forbidden. Because campaign has higher priority than Daily, a changed campaign level is never rejected in favor of an old Daily entry; instead mark each conflicting Daily entry for deterministic replacement before creating the combined roots.

`certifyCampaignFull` must re-solve all 2,000 levels, validate all 100 content shards, create current versioned audit-shard roots without rewriting published content, and recompute all 1,999,000 campaign pair comparisons plus campaign-versus-Daily and Daily-versus-Daily checks. Run it for this one-go delivery after Daily generation, every release candidate, and whenever rules, solver, fingerprint, difficulty, pacing, or uniqueness-profile versions change.

Create two packaging integrity layers:

- `verifyBundledContentFast` reads the exact source-set assets and validates root/catalog schema, exactly 2,000 campaign levels, Daily minimum, contiguous shard ranges/counts, IDs, file sizes, SHA-256 values, all legacy compatibility roots, and correspondence between runtime records and pinned published-certificate summaries. It does not re-solve every level. Wire it into ordinary debug packaging and local aggregate checks.
- `verifyBundledContent` runs the fast gate and requires a valid full-certification stamp keyed by the exact `releaseContentRoot` and all rules/solver/profile versions. A clean CI/release verification must execute `certifyCampaignFull` after Daily generation to establish that stamp; every release-like bundle task depends on `verifyBundledContent`, so Gradle cannot package it first or use a stale stamp.

All verification reads the exact checked-in assets consumed by the app, never a generator temp directory. Keep `generateCampaign`, `generateGarden`, and `generateDailyPool` as explicit intentional content-update operations; ordinary builds verify/package checked-in content and never regenerate it silently.

---

## 11. Bundled offline daily challenge

Implement Daily Bloom without a backend.

- Generate a bundled, versioned, solver-certified daily pool separate from the campaign.
- A pool of at least 120 distinct certified boards is a hard deliverable; 365 is the aspirational target within the versioned bounded candidate budget. Report the actual count.
- Generate/revalidate Daily only after the 2,000-level campaign registry is fixed. Reject every exact, D4, dynamic, solution-grammar, structural, hard-near, or strict review-similarity collision against the campaign and earlier Daily entries; a Daily candidate always loses to campaign content.
- Persist pool version, seed, full definitions, certification, and checksums.
- Inject the local-date source. For a pool of size `P`, derive one stable offset from `dailyPoolVersion` and use `index = floorMod(localDate.toEpochDay() + offset, P)`. This visits every pool entry once before cycling and is independent of locale, time of day, and wall-clock duration.
- Persist an immutable `DailySelection` containing `challengeKey = dailyPoolVersion + ":" + challengeLocalDate`, challenge local date, pool version, level ID, full versioned level definition/checksum, and first-open timestamp for display only. Enforce one selection and one completion/streak increment per local calendar date in local storage even across pool-version updates.
- Persist `lastAdvancedLocalDate` and the active selection. On first use, create that date’s selection. Thereafter advance only when the newly observed local date is strictly later than `lastAdvancedLocalDate`; if it is equal or earlier because of clock rollback or timezone travel, keep the active selection pinned. A forward timezone/date change may advance once; returning backward must not create or re-award another challenge.
- Reopen the persisted selection after process death or app update even if the current bundled pool version changed. Use its persisted full definition when the old bundle entry is unavailable. New pool versions apply on the next legitimate forward-date advance.
- Never award one local date twice.
- Never delete prior daily results because the clock or timezone changes.
- Daily play, result history, best moves, and streak work in airplane mode.
- Missing a day may reset the current numeric streak but never removes past completion records, best moves, history, or access.
- Unlock Daily Bloom after campaign level 10.
- Do not claim anti-cheat protection or global competition.
- Offline-only state cannot prevent replay after app-data deletion, reinstall, device cloning, or deliberate clock manipulation. Document that limitation rather than claiming otherwise.
- If bounded generation cannot reach the hard minimum of 120, fail the content task honestly. Never fall back to an unverified runtime board. Reuse after the certified pool’s full cycle is allowed and documented.

---

## 12. Local persistence

Use local-only persistence.

Recommended split:

- Room:
  - Campaign progress.
  - Per-level best result/replay.
  - Active attempt.
  - Daily selection and completion.
  - Streak and completion history.
  - Idempotent reward/ad grants if monetization is enabled.
- DataStore:
  - Sound effects and haptics.
  - Reduced motion.
  - High contrast.
  - Direction-button preference.
  - Theme if supported.
  - Tutorial/first-session flags.
  - Consent and ad-policy timestamps where appropriate.

Requirements:

- Repository interfaces allow in-memory test fakes.
- Keep static level definitions in versioned assets, not 2,000 prepopulated Room rows. Persist sparse progress only for touched/completed levels, keyed by stable level ID with indexed campaign order/Chapter ID where useful.
- Persist a small campaign state containing current stable level ID and highest sequentially unlocked campaign order. Completion atomically advances it without scanning 2,000 records. Derive migration unlock state from highest contiguous completion, never merely the numerically highest completed row.
- Maintain Garden/Chapter completion and star aggregates transactionally or query them through indexed sparse progress; do not rebuild a 2,000-level UI model after every write.
- Completion, stars, unlock, and best-result writes are atomic.
- Active-run persistence stores stable level ID/order, rules version, per-level content version, definition checksum, and exact logical state/snapshot so compatible runs survive process death and catalog expansion.
- Room schemas are exported.
- Provide and test explicit migrations for every actual schema-version transition introduced or already required. A new version-1 database needs schema export and a documented future migration policy, not a fake `1→1` migration.
- Add an explicit 100→2,000 campaign-expansion migration fixture proving that levels 1–100 progress, stars, best moves, replays, Daily history, settings, entitlement state, and compatible active attempts survive and that completion of level 100 unlocks level 101.
- Never use destructive migration in release.
- Persist stable primitives and versioned game data, not Compose state, Views, or arbitrary Java serialization.
- Handle corrupt or incompatible active-state data safely without deleting unrelated progress.

---

## 13. Screens and navigation

Implement:

### Home

- Continue.
- Campaign.
- Daily Bloom.
- Collection.
- Settings.
- A lightweight current-Garden progress visual and global `completed / 2000` progress.
- No fake splash delay.

### Campaign

- Show 20 virtualized, catalog-driven Garden cards—never 2,000 level buttons on one surface.
- Each Garden card shows stable Garden name/order, completed count out of 100, earned stars, five Chapter petals, and lock/current/complete state.
- Provide Continue/jump-to-current and auto-scroll to the current Garden using stable IDs and saved scroll state.
- Preserve the original first five Chapter names and progress presentation inside Garden 1.
- Derive summary counts from sparse aggregate progress and root catalog metadata, not loaded board definitions.

### Garden

- Show exactly five Chapter cards loaded from the selected Garden descriptor.
- Each card shows completed count out of 20, stars, decorative growth state, difficulty mix summary, and lock/current/complete state.
- Auto-focus the current Chapter and preserve navigation/scroll state.

### Chapter

- Show one virtualized 20-level grid with stable ID keys.
- Preserve locked, current, completed, and three-star states; optionally show the certified difficulty band with text/shape rather than color alone.
- Load only this Chapter’s 20 lightweight summaries/progress records. Do not decode or compose unopened boards.
- Show explicit loading, missing/corrupt-shard, and retry states without modifying progress or crashing.

### Game

- Garden/Chapter/global-level label and accessible difficulty label.
- Moves.
- Buds remaining.
- Undo.
- Restart.
- Hint.
- Pause/back behavior that preserves state.
- Centered square board.
- Swipe gesture.
- Optional accessible direction buttons.

### Completion

- 1–3 stars.
- Player moves and optimal moves.
- Replay.
- Next Level.
- Back to Garden.
- Retry.
- Local share card.
- Brief Bloom celebration before the sheet.

### Daily Bloom

- Today’s bundled certified board.
- Best moves.
- Current streak and completion history.
- No countdown pressure during play.

### Collection

- Page/filter by Garden and Chapter; never construct 2,000 card models at once.
- Generate completed-level cards and Bloom silhouettes locally on demand rather than storing prerendered images.
- Finished Bloom silhouette, stars, and best moves.
- Do not turn this into a cosmetic shop.

### Settings

- Sound.
- Haptics.
- Reduced motion.
- High contrast.
- Swipe versus optional direction buttons.
- Privacy/ads information if monetization dependencies are present.
- Restore Purchases if billing is implemented.
- Language entry only if the repository already supports an in-app selector; otherwise follow system language.

Use edge-to-edge layouts and correct back navigation. Keep navigation arguments small and stable.

---

## 14. Visual direction and animation

### 14.1 Identity

Avoid:

- Neon space.
- Dark generic sci-fi.
- Glowing gravity ball.
- Portal-heavy maze imagery.
- Photorealistic 3D.
- Visual clutter when the board fills.

Use:

- Warm cream/parchment background.
- Muted garden greens and teal.
- Terracotta/amber accents.
- Raised Stone silhouette.
- Seed Spirit with one recognizable leaf/droplet silhouette.
- Buds differentiated by shape, outline, and state—not color alone.
- Bloom as tactile raised leaves, petals, folded-paper foliage, or compact hedge tiles.
- Soft shadows that communicate collision height.

Create an original adaptive app icon using the Seed/Bloom silhouette. Do not use copyrighted or scraped art.

### 14.2 Board rendering

- Prefer Compose Canvas in a Compose app or a custom-drawn View in an existing Views app when it provides better performance and visual control.
- Keep simulation coordinates separate from pixels.
- Preserve square cells and a readable board on phones and tablets.
- Cache geometry and avoid per-frame allocation.
- Ensure Stone, Bloom, floor, Seed, collected Bud, and uncollected Bud remain distinguishable at high board occupancy.
- Expose equivalent accessibility semantics/virtual nodes even when the board is custom-drawn.

### 14.3 Animation contract

- Animate from TransitionResult only.
- Move the Seed along traversedPath.
- Collect Buds in traversal order.
- Grow Bloom only after the Seed stops, visually following departedCells.
- Cap long-slide duration so play remains fast.
- Lock gameplay actions only for the short presentation interval.
- Blocked swipe receives subtle bump feedback without changing state.
- Completion uses a short garden-wide bloom, then the completion sheet.
- Undo restores logical state immediately and presents a short non-blocking reversal/fade.
- Reduced motion replaces travel/growth sequences with short fades or immediate state.
- Animation interruption, lifecycle pause, or recomposition must settle on the already-decided afterState.

---

## 15. Audio and haptics

Implement a lightweight optional feedback layer:

- Soft directional whoosh.
- A pleasant ascending pentatonic note for each Bud collected during one swipe.
- Organic pop/rustle when Bloom grows.
- Muted bump for an invalid swipe.
- Warm completion chord.
- Light haptic on Bud collection.
- Slightly stronger but brief completion haptic.

Requirements:

- Gameplay never depends on sound or vibration.
- Respect system/device capability and in-game toggles.
- Keep assets small.
- Use original or reproducibly generated local audio, not unlicensed files.
- Avoid a heavy audio engine.
- Release audio cleanly on lifecycle teardown.

---

## 16. Accessibility and responsiveness

- Put all player-facing text in Android string resources.
- Do not hardcode player-visible strings.
- Provide an English fallback and localization-ready resource structure.
- Preserve RTL compatibility.
- Do not invent low-quality bulk translations unless approved translations already exist.
- Minimum 48dp interactive targets.
- Meaningful TalkBack labels for buttons, level state, Seed position, Buds remaining, move count, stars, and completion.
- Announce important collection/completion events without announcing every animation frame.
- Optional cardinal direction buttons for players who cannot swipe reliably.
- High-contrast mode.
- Reduced-motion mode.
- Color-blind-safe semantics using shape, elevation, texture, and outline in addition to color.
- Font scaling without overlapping controls or board.
- No flashing effects.
- Responsive phones, foldables, 7-inch tablets, and 10-inch tablets.
- Portrait-first one-handed reach on phones.

---

## 17. Progression, hints, replay, and sharing

### 17.1 Retention without pressure

- 2,000-level offline campaign organized as 20 Gardens × 5 Chapters × 20 levels.
- Visible chapter growth.
- Stars for mastery but no star gates.
- Daily Bloom after level 10.
- Local daily streak and history.
- Immediate Next, Replay, and Retry.
- No login reward calendar, lives, energy, artificial waiting, or loss punishment.

### 17.2 Hints

- Hints must be valid for the player’s current exact state.
- Use bounded FROM_CURRENT_STATE BFS on a background dispatcher or a cached certified policy.
- First hint highlights the recommended direction.
- A deeper hint may preview the stopping cell and Buds that would be collected.
- Never automatically make a move.
- If exact current-state solving returns `UNSOLVABLE` while legal moves remain, explain in friendly language that this path can no longer reach every Bud and prominently offer Undo and Restart. Do not call it DEAD and do not recommend a guessed direction.
- If the solver returns `BUDGET_EXCEEDED`, show a distinct safe “hint unavailable” state; never mislabel the state unsolvable and never guess.
- At least one basic hint path must remain available without payment or ads during onboarding.
- Undo is never monetized.

### 17.3 Replay

- Store a compact direction sequence plus content/rules version and checksum.
- Replays must reproduce the same final state.
- Allow completion replay at adjustable speed.
- Detect and reject incompatible/corrupt replays safely.

### 17.4 Share card

- Generate locally using Android graphics/Compose capture.
- Include working game name, chapter/level or Daily date, moves, stars, and finished Bloom silhouette.
- Do not reveal the direction sequence or full solution.
- Share through the Android Sharesheet.
- Sharing is optional and never required for rewards.
- Use a FileProvider and clean up temporary files safely.

---

## 18. Monetization, consent, and billing

Gameplay must work completely without ads, network, billing, or consent availability.

### 18.1 Ads abstraction

Place SDK access behind an AdsGateway owned by the app layer.

Always provide:

- No-op implementation for tests and builds with ads disabled.
- A fake gateway for deterministic policy/ViewModel tests.
- A release-safe configuration surface using external Gradle properties, environment variables, or manifest placeholders.

Only when both a compatible approved ad/consent SDK is already present and valid integration configuration is supplied, provide the real adapter. Its debug mode must use official test inventory only. Otherwise do not add a vendor SDK: bind the no-op gateway in every variant and disable ad-dependent UI.

Never hardcode live IDs. If production IDs are unavailable, keep release ads disabled and document configuration rather than inventing values.

### 18.2 Consent

- Resolve required consent before initializing/requesting personalized advertising.
- Provide privacy options entry when required.
- Consent failure must fail closed for personalized ads and fail open for gameplay.
- Do not block offline gameplay on a network consent request.

### 18.3 Rewarded ads

Allowed only after an explicit player action:

- Optional deeper hint after free onboarding help.

Rules:

- Reward only from the verified reward callback.
- Grant once using an idempotent local transaction.
- An unavailable, failed, dismissed, duplicated, or late callback must never corrupt progress.
- Never reward Undo or Restart because they are already free.

### 18.4 Interstitial policy

Centralize policy in one testable class.

Initial guardrails:

- Never during the first-ever session.
- Never during onboarding levels 1–5.
- Eligible only after successful campaign level completion.
- At least three completed eligible levels since the previous interstitial.
- At least 180 seconds since the previous interstitial.
- Never on failure, DEAD state, Undo, Restart, Hint, Daily-result review, Settings, Collection, sharing, app resume, or immediately after tapping Next.
- Never interrupt movement, Bloom growth, or completion celebration.
- Ad failure or unavailability continues directly to gameplay.
- No banner over the board or near controls.
- Do not add App Open ads in this task.

The only initial interstitial trigger is an explicit `Back to Garden`/completion-sheet dismissal that navigates from a completed level back into the Campaign → Garden → Chapter hierarchy: if every guardrail passes, show it after the completion sheet closes and before revealing that destination. Direct `Next Level`, Chapter-complete, Garden-complete, and final-campaign celebrations never trigger one. Do not invent another trigger.

### 18.5 Remove Ads purchase

Always implement a `BillingGateway`, no-op/fake gateways, entitlement state/policy, and deterministic tests. Implement the real one-time non-consumable Remove Ads Play Billing adapter and expose purchase UI only if a compatible billing dependency and external product configuration are available; otherwise keep the production purchase entry disabled and document setup.

- Product ID is externally configurable.
- Handle purchase acknowledgement.
- Restore entitlement.
- Handle pending, cancelled, unavailable, and duplicate callbacks.
- Persist entitlement locally while treating Play Billing as authority when available.
- Remove interstitials; rewarded hints may remain optional only when their benefit is explicitly requested.
- If external Play product setup is unavailable, compile and test through the fake billing gateway, disable the production purchase entry, and document the manual Play Console step. Do not mark real purchase/restore manual QA as passed.

No subscriptions, gameplay currencies, pay-to-win, or purchase-required levels.

---

## 19. Analytics abstraction

Create a vendor-neutral Analytics interface with no-op default and debug logger.

If the repository has no approved analytics SDK/configuration:

- Do not choose or install a vendor.
- Implement typed events, debug logging, and documentation only.

Suggested events:

- app_opened
- onboarding_step
- level_started
- level_completed
- level_abandoned
- level_restarted
- undo_used
- hint_requested
- hint_shown
- daily_started
- daily_completed
- replay_started
- share_requested
- generator_fallback_or_error
- ad_request/result
- purchase_request/result

Parameters may include:

- Stable level ID.
- Garden and Chapter stable IDs/orders.
- Provisional difficulty band.
- Move/attempt/time buckets.
- Stars.
- Hint/Undo/Restart counts.
- Solver/generator version.

Do not send:

- Names, email, contacts, files, media, precise location, raw board state, raw solution sequence, advertising identifiers without consent, or unnecessary device data.

Analytics failure must never affect gameplay. Document every event, purpose, and parameter.

---

## 20. Build variants and configuration

Keep variants minimal:

### Debug

- Debuggable.
- Application ID/version suffix when compatible.
- Debug engine overlays and an app-safe developer entry backed only by packaged app/core APIs and precomputed reports.
- Official test ads or no-op ads only.
- Fake/no-op billing.
- Debug/no-op analytics.
- Solver/generator diagnostic logging without personal data.

### Release

- Non-debuggable.
- R8 and resource shrinking enabled when compatible.
- Debug overlays removed.
- Production signing external.
- Ads disabled unless valid external configuration exists.
- No live test IDs.
- Targeted keep rules only where serialization/SDKs require them.

Do not add speculative dev/qa/staging flavors. Preserve existing variants when present.

In a bootstrapped project, the ordinary release bundle task must be configuration-free and capable of producing an unsigned/non-publishable bundle without live IDs, production services, or signing credentials. In an existing project whose publishing release variant intentionally requires protected credentials or service plugins, do not change or bypass it; add or use one narrowly scoped credential-free CI-release variant/task (for example `bundleMazeBloomCiRelease`) that exercises release code, shrinking, and packaged content without becoming publishable. This purposeful verification variant is allowed and is not a speculative environment flavor. Signed publication remains a separate external release step. Never bundle debug/test ad IDs into any release-like artifact.

---

## 21. Debug and authoring tools

Debug builds should expose a clearly non-production developer panel capable of:

- Load level by stable ID/global order and inspect its Garden/Chapter shard.
- Import/load a versioned development level definition from a bundled or debug-safe source.
- Display cell indices and masks.
- Show projected stop/path/Buds/Bloom for a direction.
- Show StateKey and replay checksum.
- Display optimal solution and DifficultyReport.
- Autoplay canonical replay.
- Adjust animation speed.
- Toggle reduced motion/high contrast.
- Simulate DEAD state and process restoration.
- Use no-op/test ads.
- Export a level/certification report.
- Compare any two level IDs under every strict uniqueness tier and show the winning D4 alignment/distance/reason.
- Display decoded-shard cache entries/evictions and simulate a missing/corrupt shard.

Production builds must not expose these tools.

The Android app must never depend on or load JVM-only `:content-tools`. Put reusable solver/report models in the app-safe pure core and package only bounded runtime capabilities or precomputed certification summaries needed by the developer panel.

Content tooling must make every rejection explainable. Never call a level certified merely because construction found one successful history.

---

## 22. Automated testing

Use existing test conventions. Prefer table-driven and seeded loops over adding a heavy property-test dependency.

### 22.1 Transition tests

- Stop at each boundary.
- Stop before Stone.
- Stop before existing Bloom.
- Blocked-at-origin swipe is a no-op.
- Actions from SOLVED or DEAD states are no-ops.
- One-cell movement is valid.
- Starting and intermediate departed cells become Bloom.
- Final resting cell never becomes Bloom.
- Bloom created now does not block the current swipe.
- Collect Bud on intermediate cells.
- Collect Bud on resting cell.
- Collect multiple Buds in one swipe.
- Crossing the final Bud does not stop early.
- Invalid swipe collects nothing and creates nothing.
- Solve only after full movement/Bloom commit.
- Correct DEAD detection.
- Undo exact restoration.
- Restart exact restoration.
- Exact `traversedPath`, `departedCells`, per-cell event order, and terminal event order.

### 22.2 Invariant and fuzz tests

Across thousands of deterministic seeded legal states:

- Masks stay in bounds.
- Stones stay unchanged.
- Bloom is monotonic.
- remainingBudMask is monotonic decreasing.
- Collected Buds equal entered-path intersection with prior remaining Buds.
- Every valid move adds at least one Bloom.
- Final cell is not Bloom.
- Seed never crosses a prior blocker.
- Identical state/action gives identical output.
- Replay serialization/deserialization is equivalent.

### 22.3 Solver tests

- Tiny handmade boards with known optimal answers.
- Unsolvable boards.
- DEAD start.
- Multiple optimal solutions.
- Exact optimal+1/+2 counts on a fixture where ordinary BFS state deduplication would undercount longer solutions.
- Converging histories requiring transposition deduplication.
- Exact BFS checked against exhaustive enumeration on tiny boards.
- Stable canonical direction ordering.
- Stable replay checksum.
- BUDGET_EXCEEDED distinct from UNSOLVABLE.
- FROM_CURRENT_STATE hint after deviation.

### 22.4 Generator and duplicate tests

- Same seed/version produces byte-identical output.
- Changing one `(Garden, difficulty, boardSize)` stream does not perturb another stream or any locked Garden 1 output.
- Serial and bounded-parallel corpus selection match.
- Every emitted accepted level passes exact certification.
- Every rotation/reflection has the same geometric fingerprint.
- Every square D4 transform has the specified coordinate and direction mapping, inverse, and round trip.
- Exact-definition, dynamic-graph, solution-grammar, and canonical-replay structural duplicate fixtures.
- Near-duplicate boundary fixtures at mask distances 6/7, 12/13, and 18/19 with every behavior-pattern combination and tied-D4 alignment.
- Strict review-similarity collisions reject the later candidate; lower campaign order wins deterministically.
- Replacement candidates are checked against the full registry, and Daily conflicts always lose to campaign content.
- Full and incremental global uniqueness roots agree.
- All-Buds-on-one-line rejection outside tutorial.
- Forced-corridor rejection.
- Failure records the smallest reproducing attempted ordinal and its derived seed.

### 22.5 Campaign regression tests

Pin:

- Exactly 2,000 IDs and campaign orders.
- The independent immutable legacy definition, solution, certificate, and compatibility roots plus all level 1–100 hashes/replays/published certificates.
- Exactly 20 Gardens, 100 Chapters, 20 levels per Chapter, and 100 content-shard roots plus current versioned audit-shard roots.
- Exact extension/per-Garden/per-Chapter difficulty quotas, derived complete-campaign totals, pacing templates, board-size quotas, `campaignUniquenessRoot`, and combined `globalUniquenessRoot`.
- Level hashes.
- Optimal move counts.
- Canonical replays.
- Difficulty labels/metrics version.
- Certification checksums.

Test shard ranges for gaps/overlap, globally unique Chapter IDs, random direct loads for levels 1, 100, 101, 1,000, and 2,000, initialization-mode certification, full-versus-incremental audit equality, and unchanged prior content-shard bytes after an append or recertification. Require an explicit `campaignCatalogVersion` bump and regenerated root manifest for intentional catalog changes; unchanged levels retain their per-level content version/hash and published certificate.

### 22.6 Persistence tests

- Campaign progress CRUD.
- Atomic completion/stars/unlock.
- Best result and replay.
- Active-run restoration.
- Room migrations.
- Sparse progress with no 2,000-row prepopulation.
- 100→2,000 expansion preserves all old progress and unlocks level 101 after contiguous completion through level 100.
- Highest contiguous completion, rather than highest arbitrary completed order, determines sequential unlock state.
- Corrupt/incompatible active-state fallback.
- Daily mapping stability.
- Same-date reopen, strict forward-date advance, timezone-forward advance once, timezone/clock rollback pinning, pool-version update on an active date, and full-pool cycle behavior.
- Streak without duplicate completion increment.
- Idempotent rewarded-ad grant.
- Remove Ads entitlement restore through fake billing.

### 22.7 Ad-policy tests

- First session suppression.
- Onboarding suppression.
- Level interval and time cooldown.
- Failure/restart/Undo/hint/daily/settings/share suppression.
- Ad unavailable/failure continues gameplay.
- Duplicate rewarded callback grants once.
- Screen recreation and late callback safety.
- Remove Ads disables interstitial.

### 22.8 ViewModel and UI tests

- First-run onboarding.
- Virtualized Campaign → Garden → Chapter navigation, jump-to-current, stable scroll/selection restoration, and Garden/Chapter completion transitions.
- Opening/scrolling campaign summaries does not decode 2,000 boards; only the selected Chapter shard is loaded.
- Missing/corrupt shard produces a recoverable error without progress loss.
- Swipe play.
- Direction-button play.
- Undo/Restart.
- Hint from current state.
- Completion and Next.
- Player input ignored by the controller while a transition is being presented, while direct pure-engine calls remain deterministic.
- Background/resume and state restoration.
- Daily play offline.
- Settings persistence.
- Large font/reduced motion/high contrast critical flows.
- TalkBack semantics for important controls and status.

---

## 23. Performance and quality

- No network requirement for gameplay.
- No heavy engine.
- No per-frame object churn in board rendering.
- No main-thread BFS, generation, database I/O, ad request, or file rendering.
- Bound every solver/generator loop.
- Cancel obsolete hint work.
- Do not leak Activity, View/Compose, audio, ad, or billing references.
- Handle process recreation.
- Support airplane mode and ad/consent unavailability.
- Keep download size modest.
- Root catalog is at most 2 MiB; one uncompressed Chapter shard is at most 2 MiB; total uncompressed campaign runtime assets are at most 64 MiB and compressed campaign assets at most 16 MiB. Treat smaller generated-baseline targets as optimization goals, but these are hard ceilings.
- Cold start reads only root catalog metadata and at most the current Chapter shard. Direct loads of levels 1, 100, 101, 1,000, and 2,000 each resolve through one shard; decoded-shard cache never exceeds three.
- Record catalog parse time, shard-load p50/p95, loaded-shard count, compressed/uncompressed campaign size, full-certification peak heap, and certification expansions. Timing values are diagnostic; count/size/heap caps are authoritative.
- Use only necessary manifest permissions.
- Do not request broad storage access.
- Use Android Photo/Media APIs only if actually required; a share card should use cache + FileProvider.

---

## 24. Documentation

Update existing documents rather than duplicating them when possible. Ensure equivalent repository documentation exists:

- README.md
  - Setup, JDK/SDK, build/run, empty/existing project decisions, external configuration.
- docs/ARCHITECTURE.md
  - Modules, dependency direction, state flow, lazy catalog/shard repository, sparse persistence, animation separation.
- docs/GAME_RULES.md
  - Exact deterministic rules and edge cases.
- docs/LEVEL_SCHEMA.md
  - Versioned level format, CampaignCatalog/Garden/Chapter/shard descriptors, direct ID index, validation, and separate catalog/per-level versions.
- docs/SOLVER_GENERATOR.md
  - BFS, namespaced extension generator, budgets, strict global uniqueness rules, fingerprints/roots, difficulty metrics, CLI.
- docs/CONTENT_PIPELINE.md
  - Locked first-100 prefix, 2,000-level quotas/pacing, Garden generation, 100 Chapter shards, incremental/full certification, exact packaged-content gates, golden manifests, versioning.
- docs/DIFFICULTY_AND_HUMAN_REVIEW.md
  - Automatic model, mixed-difficulty schedule, 2,000-level stratified review policy, limitations, worksheet, recalibration plan.
- docs/DAILY_CHALLENGE.md
  - Offline mapping, pool versioning, timezone/rollback behavior, limitations.
- docs/MONETIZATION_ANALYTICS.md
  - Placements, cooldown, consent, test IDs, external release config, event catalog.
- docs/ACCESSIBILITY_VISUAL_AUDIO.md
  - Semantics, contrast, reduced motion, haptics, audio assets.
- docs/QA_RELEASE_CHECKLIST.md
  - Legacy-prefix, 2,000-level count/quota/global-uniqueness, fast/full content gates, automated/manual gates, Play-signing/configuration blockers.
- docs/IMPLEMENTATION_REPORT.md
  - Actual final evidence from this execution.

Create machine-readable campaign certification reports and a human-review worksheet in an appropriate reports/docs location. Do not write aspirational documentation for features that were not implemented.

---

## 25. Required verification

Use the repository’s Gradle wrapper. Discover actual task names and run all applicable checks.

For the default empty-repository module layout, at minimum attempt:

~~~bash
./gradlew --version
./gradlew projects
./gradlew :game-core:test
./gradlew :data:testDebugUnitTest
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
./gradlew check
~~~

In an existing repository, the literal example module paths may not exist. Use the conceptual-to-actual mapping from section 4.1, discover task names with `./gradlew projects`/`./gradlew tasks`, and run the equivalent core tests, data tests, app unit tests, lint, debug assembly, credential-free CI-release bundle, and aggregate checks. Preserve a credential-protected publishing variant. Do not report a nonexistent example task as a product failure; report the real mapped command and result.

Also run configured quality tasks such as ktlintCheck or detekt.

Run the actual content tasks, for example:

~~~bash
./gradlew :content-tools:test
./gradlew generateCampaign
./gradlew verifyLegacyPrefix
./gradlew certifyCampaignIncremental
./gradlew generateDailyPool
./gradlew certifyCampaignFull
./gradlew verifyBundledContentFast
./gradlew verifyBundledContent
./gradlew :app:bundleRelease
~~~

Use the task names actually implemented. `generateCampaign` must preflight an existing legacy prefix before changing content; on an empty repository it first creates and pins Garden 1, verifies the newly established compatibility roots, and then generates the extension. The incremental task uses the explicit initialization behavior from section 10 when there is no parent. Never run a release-like bundle before full certification and `verifyBundledContent`; enforce this with Gradle task dependencies as well as command order.

Run:

~~~bash
./gradlew :app:connectedDebugAndroidTest
~~~

only when a compatible emulator/device is available. If none is available, mark it SKIPPED with the exact reason. Do not claim it passed.

The release verification target is the bootstrapped project’s unsigned bundle or the existing project’s dedicated non-publishable CI-release bundle. It contains no live credentials or test inventory. Production signing and Play upload are external checks and must be listed separately.

Do not hide failures. Fix failures caused by this work. If a failure is demonstrably pre-existing, show the evidence and keep it separate from new failures.

### 25.1 Manual smoke verification

When an emulator/device is available, verify:

- Fresh install.
- Levels 1–5 onboarding.
- Garden → Chapter drill-down, jump-to-current, and virtualized progress through all 20 Gardens.
- Representative Easy, Normal, Hard, Expert, and Master levels, including debug direct-load checks at global levels 100, 101, 1,000, and 2,000.
- Chapter completion, Garden completion, level 100→101 unlock, and final level 2,000 campaign-complete state.
- A later Bloom-assisted level.
- Undo, Restart, Hint, completion, replay, Next.
- Background/resume and process recreation.
- Daily challenge in airplane mode.
- Share card.
- Consent unavailable/declined.
- Ads unavailable.
- Rewarded fake/test callback.
- Remove Ads fake billing entitlement; test the real Play purchase/restore flow only when valid external product configuration and a compatible test environment are available.
- Phone and tablet layouts.
- Large font.
- Reduced motion.
- High contrast.
- Direction buttons.
- TalkBack labels.
- Launch a release-like APK only when a locally installable artifact signed with a non-production test key is available; an unsigned AAB is not installable, so otherwise mark this check SKIPPED with that exact reason.

---

## 26. Definition of done

The work is complete only when:

- The app launches to a functional MazeBloom experience.
- The deterministic transition engine implements every exact rule.
- Undo, Restart, save/resume, completion, and DEAD recovery work.
- Exact BFS and FROM_CURRENT_STATE hint solving work.
- Generator output is deterministic and bounded.
- `legacyPrefixCompatibilityRoot` plus its definition, solution, and certificate component roots prove every promised frozen field of levels 1–100 did not drift.
- Exactly 2,000 campaign levels are automatically certified and bundled as 20 Gardens, 100 Chapters, and 100 verified content shards with corresponding current audit roots.
- Exact mixed-difficulty, pacing, and board-size quotas for Gardens 2–20 match section 9 with genuinely certified Easy through Master levels; Garden 1 matches its immutable approved metadata and complete totals are derived correctly.
- Exhaustive strict uniqueness passes with zero exact-definition, D4-geometric, complete-dynamic, solution-grammar, structural, hard-near, or review-similarity collisions involving any new level 101–2,000 or Daily entry; the sorted campaign/combined audits, `campaignUniquenessRoot`, and `globalUniquenessRoot` are pinned, and any pair wholly inside immutable levels 1–100 is explicitly grandfathered/reported.
- At least 120 daily levels are automatically certified, bundled, and work offline; report whether the 365-level target was reached.
- Lazy Campaign → Garden → Chapter navigation, sparse progression, 100→2,000 migration, stars, Daily, paged Collection, Settings, replay, and share card work.
- Visual identity is warm garden/paper, not neon gravity.
- Accessibility and reduced-motion controls work.
- Ads/billing/analytics are safely abstracted and cannot break gameplay.
- Debug uses test/no-op monetization only.
- No credentials or live IDs were added.
- Every new or affected unit, UI, lint, content-certification, and packaging gate passes. The aggregate repository check either passes or any demonstrably unrelated pre-existing failure is reproduced against the untouched baseline, remains unmodified, and is reported as a release blocker.
- `verifyBundledContentFast`, `certifyCampaignFull`, and `verifyBundledContent` pass against the exact assets wired into packaging.
- Debug APK builds.
- The bootstrapped unsigned release bundle or existing-project credential-free CI-release bundle builds without live IDs or production signing credentials; the protected signed publishing build remains an explicitly reported external step.
- Documentation matches the implemented system.
- Human difficulty/playtest validation is explicitly reported as pending unless actual evidence exists.

---

## 27. Final response

Finish with a concise, evidence-backed report containing:

1. What was implemented.
2. Final product rule and deliberate non-goals.
3. Module/dependency structure.
4. Campaign/Daily counts; 20-Garden/100-Chapter structure; per-band and board-size distributions.
5. Legacy compatibility-root result, full certification results, all 1,999,000 campaign-pair plus campaign/Daily and Daily/Daily comparisons, collision/rejection counts by uniqueness tier, difficulty/pacing results, authoritative expansion/entry maxima, diagnostic solver p50/p95 with benchmark environment, content/audit-shard roots, `campaignUniquenessRoot`, `globalUniquenessRoot`, `campaignRoot`, and `releaseContentRoot`.
6. Persistence and offline behavior.
7. Ads, billing, consent, and analytics behavior.
8. Accessibility, animation, audio, haptics, and responsive behavior.
9. Key files created/changed.
10. Every verification command with PASS, FAIL, or SKIPPED.
11. Manual checks performed.
12. Remaining external configuration: package/name clearance, signing, production ad IDs, billing product, consent/privacy setup, approved analytics, and Play Console work.
13. Human-review and closed-test work still required.
14. Final git status --short.
15. A concise diff summary.

Do not claim:

- Production readiness when mandatory checks failed or were skipped.
- Human difficulty validation without human data.
- 10M-install likelihood.
- Unique/first-ever legal status.
- Store-name or trademark clearance.

Do not commit or push. Leave all changes available for review.

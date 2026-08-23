# CODEX ONE-GO IMPLEMENTATION PROMPT — MAZEBLOOM PREMIUM UI + ENDLESS GARDEN

You are Codex working inside the existing MazeBloom Android repository. Implement this follow-up phase completely in one continuous execution. Make local repository changes only. Do not commit, push, create branches or pull requests, change Git identity/remotes, upload builds, add production credentials, or wait for routine approval.

Inspect the repository before editing, preserve unrelated and uncommitted work, establish the current build/test baseline, implement the change, run all available verification, fix failures caused by your work, and finish with an evidence-based report. Do not stop after analysis or return only a plan.

---

## 1. Authoritative starting point

The previous implementation reported all of the following. Verify them against the repository and treat the repository as the final source of truth:

- The first five Campaign levels are tutorials with animated finger guidance.
- Every completion has an emoji/confetti celebration.
- A first completion grants exactly 10 Coins.
- A hint costs 30 Coins; an insufficient balance exposes the rewarded-ad path.
- Skip requires a rewarded ad and its result persists across relaunch.
- An interstitial becomes due after every fifth first completion, subject to the existing policy and lifecycle guards.
- Campaign completion unlocks a certified 100-level Auto Progressive pack.
- That pack contains exactly 16 Easy, 28 Normal, 28 Hard, 20 Expert, and 8 Master levels.
- The current 2,220 accepted levels were solved and exhaustively compared across 2,463,090 unordered pairs with no new uniqueness collision.
- Room schema v2 and a non-destructive migration exist.
- The previous verification reported 28 unit tests passing, lint passing with 18 advisory warnings, debug APK/aggregate check/R8/release AAB passing, and device testing skipped because no emulator/device was connected.
- Debug uses deterministic fake ads. Release fails safely when a live ad/consent provider is unavailable.

Before changing code:

1. Read every applicable `AGENTS.md` and project instruction file.
2. Inspect `git status`, relevant diffs, modules, architecture, navigation, theme, current assets, Room schemas/migrations, generator/solver/certifier, uniqueness registry, monetization state machines, and tests.
3. Run the repository's existing fast unit/content checks and a debug build to establish a baseline.
4. Locate the authoritative manifests, hashes, certificates, and uniqueness roots for all 2,220 accepted levels.
5. Record the conceptual-to-actual class/module/task mapping in the implementation report. Do not invent duplicate subsystems when working ones already exist.

This follow-up is keyed to the reported 2,220-item baseline. If the authoritative constituent manifests do not total exactly 2,220 or their pinned root/evidence is unavailable, stop before mutation and report `BASELINE_MISMATCH` with the actual composition. Do not silently recompute this prompt's roots/pair constants or adapt generation to an unknown corpus.

If an older specification says MazeBloom has no Coins or no runtime-generated levels, update that stale documentation. The verified repository behavior and this follow-up contract supersede those statements.

---

## 2. Required outcome

Deliver both changes together:

1. Redesign the player-facing UI into a cohesive, attractive, premium garden puzzle experience implemented with the repository's existing Jetpack Compose/Material architecture.
2. Replace the finite “100-level Auto Progressive pack” concept with an offline Endless Garden mode:
   - The already certified Auto Progressive levels 1–100 remain an immutable starter prefix.
   - Auto Progressive level 101 onward is generated deterministically on demand.
   - A generated level is never shown until it is exactly solved, difficulty-certified, serialized/round-tripped, and checked for every existing uniqueness tier against the complete bundled baseline and retained local Auto Progressive history.
   - Completion, Skip, process death, relaunch, and app updates never cause a retained board to be selected as a new Endless ordinal/Next candidate. An explicit Replay of the same stable level ID remains allowed and never grants first-completion rewards again.

Use **Endless Garden** as the player-facing name. Preserve existing `AutoProgressive` route keys, database identifiers, analytics identifiers, and internal names when changing them would break compatibility; migrate presentation strings rather than rewriting stable data.

Do not change the core swipe/Bloom rules, the first five tutorial rules, the fixed Campaign/Daily definitions, the certified Auto Progressive 1–100 definitions, Coin amounts, hint price, rewarded Skip requirement, interstitial cadence, ad safety, or existing player progress except where a compatible persistence extension is necessary.

### 2.1 Truthful meaning of “infinite non-repeatable”

Finite 5×5/6×6 rules produce a finite mathematical puzzle space, and an offline app cannot preserve history after the user clears storage or uninstalls without a backend/restore source. Therefore implement and describe the feature truthfully as:

> A practically unbounded Endless Garden that creates new certified puzzles on the device and never repeats any puzzle in the retained local history.

Guarantee no repeats across process death, relaunch, normal app updates, and non-destructive database migrations. Do not claim a mathematical infinity proof, cross-device uniqueness, or continuity after Clear Data/uninstall. Player-facing copy should prefer “New puzzles as you grow” and “No repeats in your saved history,” not “infinitely many mathematically unique boards.”

No backend, account, network dependency, downloaded board, or server-generated seed is allowed for gameplay content, generation, solving, certification, or uniqueness. Preserve the existing optional network behavior of approved ad/consent adapters; ads never supply level content or block ordinary certified gameplay.

---

## 3. Preserve existing behavior and data

The UI redesign and Endless extension must be behavior-preserving around the existing game:

- The Campaign, Daily, and Auto Progressive 1–100 logical definitions, IDs, orders, seeds, replays, certificates, hashes, and uniqueness records remain byte-for-byte stable unless the existing format explicitly stores presentation-only metadata separately.
- Existing Campaign/Daily/Auto progress, stars, Coins, best moves, replays, current attempt, tutorial completion, Skip results, ad counters, settings, entitlement state, and completion unlock survive migration.
- The first-completion reward remains exactly +10 Coins and is transactional/idempotent in every mode.
- A hint still costs exactly 30 Coins. Never deduct twice after recomposition, retry, process restoration, or duplicate callbacks.
- The insufficient-Coin rewarded affordance remains an explicit opt-in path; never auto-launch an ad. Inspect and preserve the repository's actual rewarded outcome. Label it accurately and accessibly—for example, `Watch ad for hint` only if the reward directly grants a hint, or `Watch ad to earn Coins` if it grants Coins. Do not rely on a video icon alone or change the reward amount/action.
- Rewarded Skip remains explicit, persists, advances once, grants no completion Coins, and permanently marks that accepted level as seen so it cannot return later.
- Preserve the existing “every fifth first completion” interstitial due counter and all cooldown, lifecycle, consent, Remove Ads, navigation, and duplicate-callback guards already implemented. UI animations, navigation recomposition, background generation, and cached-level creation must never count as a completion or trigger an ad.
- A generated level's first completion emits the same existing first-completion domain event exactly once. Whether Auto Progressive participates in the global every-fifth counter follows the repository's current source policy; do not silently broaden or exclude the mode.
- Do not add a Coin store, lives, energy, subscriptions, online services, new gameplay objects, or larger board sizes in this task.
- Never add live ad IDs, signing material, API keys, or privacy claims. Keep fake/no-op ads in debug and the current safe release behavior.

Create regression fixtures for the complete retained baseline before altering schemas or presentation code.

---

## 4. Premium visual direction — “A living paper garden”

The game must look authored, calm, tactile, and recognizable rather than like a generic Material sample, neon gravity game, casino screen, or asset-heavy cartoon. Keep the board visually dominant.

Use Compose drawing, shapes, paths, gradients, and vector assets. Do not add PNG, WebP, JPG, Lottie, remote images, downloadable fonts, or one bitmap per level. Reuse an existing local brand asset only when removing it would cause needless regression. Retain the existing result-sensitive emoji as a small celebration accent, but replace any platform-dependent emoji-as-centerpiece treatment with a branded flower/petal composition.

### 4.1 Color system

Implement semantic theme roles rather than scattering raw colors. Use this art direction as the starting palette, adjusting only when automated contrast validation requires it:

| Role | Light | Dark |
| --- | --- | --- |
| Background / parchment | `#F7F3E8` | `#101813` |
| Surface / paper | `#FFFDF7` | `#17231C` |
| Primary / fern | `#2F6B4F` | `#8ED09B` |
| Primary container | `#DCEBD8` | `#244D36` |
| Secondary / terracotta | `#C86B4A` | `#F0A184` |
| Secondary container | `#F6DED2` | `#4C2C25` |
| Tertiary / warm gold | `#E6B85C` | `#F2CB72` |
| Main ink | `#1E2B24` | `#EDF4EC` |
| Muted ink | `#657168` | `#B7C5BA` |
| Stone | `#58645B` | `#87948B` |
| Bloom trail | `#79B96A` | `#8FD17D` |
| Bud | `#F6C95B` | `#F5D477` |
| Error | `#B84A4A` | `#FFB4AB` |

Requirements:

- Normal text contrast is at least 4.5:1; large text and essential non-text controls are at least 3:1.
- High-contrast mode remains distinct from dark mode and makes Stones, Buds, Seed, Bloom, route preview, focus, and disabled states unambiguous without relying only on hue.
- Color-blind-safe differentiation uses shape, outline, texture, or icon changes in addition to color.
- No unreadable text over gradients or decorative imagery.

### 4.2 Typography and shape

- Use the existing bundled/system typeface unless a suitable local brand font already exists; do not add a network font dependency.
- Establish one clear display style, headline/title hierarchy, readable body/label styles, tabular numerals for Coin/level counters where supported, and a minimum 12sp text size for non-decorative labels.
- Use 4dp spacing multiples, generous rounded paper cards (16–24dp), pill status chips, rounded action buttons, and subtle asymmetric leaf/petal details. Avoid applying the same oversized radius to every element.
- Keep elevations restrained. Prefer tonal separation, 1dp outlines, and soft ambient shadows over heavy floating cards.
- Centralize dimensions, typography, colors, motion, and component defaults in theme/design-token files.

### 4.3 Reusable Compose components

Create or refine reusable, previewable components appropriate to the actual architecture, including equivalents of:

- `BloomScaffold`
- `MazeBloomTopBar`
- `CoinBalanceChip`
- `GardenProgressCard`
- `ChapterPetalRow`
- `LevelNode`
- `MazeBoard`
- `GameplayActionDock`
- `RewardedActionButton`
- `FingerSwipeGuide`
- `CelebrationOverlay`
- `CompletionSheet`
- `EndlessGardenCard`
- `DifficultyBadge`
- `LoadingGarden`
- `InlineErrorCard`

Components consume immutable UI state and emit events. They never grant/consume Coins, decide ad eligibility, unlock content, navigate directly, generate/solve levels, or access Room.

---

## 5. Screen-by-screen redesign

Adapt this information architecture to the existing navigation rather than rebuilding working navigation solely for naming parity.

### 5.1 Home

- Use a calm parchment background with a very subtle code-drawn botanical pattern that cannot compete with text.
- Top bar: MazeBloom mark/title, compact Coin pill, and Settings action.
- Hero: `Continue Growing`, current Garden/Chapter, meaningful progress, a living plant whose growth corresponds to actual Campaign completion, and one clear CTA.
- Make the hero mode-aware. Resume a compatible persisted active attempt first; otherwise resume the explicitly stored last-played eligible mode; otherwise use incomplete Campaign, then unlocked Endless. Daily becomes the hero only when it is the persisted active/last-played destination and never silently replaces another attempt. Label and metadata must match the actual route (`Continue Campaign`, `Continue Daily`, or `Continue Endless`) rather than always showing Garden text.
- Mode cards:
  - Campaign: current level and `completed / 2000`.
  - Daily Bloom: today status/streak using existing data.
  - Collection: existing collection summary.
  - Endless Garden: locked before Campaign completion; afterward, current ordinal, certified difficulty, completed count, and cached-ready state.
- Locked copy: `Complete the Campaign to unlock an endless stream of new mazes.` It cannot be unlocked by an ad or payment.
- Use progressive disclosure. Do not show technical hashes, all economy rules, or all 2,000 level buttons on Home.
- Preserve fast cold start: Home must not parse all campaign boards or start expensive generation while the mode is locked.
- Do not add a fake splash delay, autoplay carousel, persistent bottom navigation, or large empty hero region.

### 5.2 Campaign hierarchy

- Present 20 Gardens as a virtualized vertical winding garden path using stable keys and saved scroll position.
- Each Garden card shows name/order, completed count out of 100, stars, five Chapter petals, and locked/current/complete state.
- Give the current Garden a subtle breathing leaf outline; completed Gardens appear fully bloomed.
- Garden detail presents five Chapter cards as a flower/petal progression.
- Chapter uses an adaptive level grid with at least 56dp nodes. Each node communicates number, difficulty, stars, and locked/current/completed/skipped state using icon, outline, elevation, and text—not color alone.
- Preserve lazy shard loading, retry/error states, current-level focus, scroll restoration, and all existing unlock rules.
- On tablets, use bounded max-width/two-pane arrangements rather than stretching content edge to edge.

### 5.3 Game screen

- The board is the visual center and receives the largest stable square area available with at least 16dp breathing room.
- Top area: back/pause, Garden–Chapter–Level or Endless ordinal, accessible difficulty badge, compact moves/Buds HUD, and Coin balance.
- Bottom action dock: Undo, Restart, Hint, and clearly labelled existing Skip access in one-handed reach.
- At 320dp width and/or 200% font scale, reflow the header into two rows or move secondary metadata into an accessible overflow/details surface. Never shrink essential text below its token, clip location/difficulty/economy state, or compress the board to preserve a one-row header.
- Hint always exposes its 30-Coin price when payable. With insufficient Coins, show accessible copy matching the existing direct-hint or Coin-reward outcome; Skip shows `Watch ad to skip`. Preserve loading, cancellation, verified reward, unavailable, and error states. Never imply reward success before the verified callback.
- Board art: recessed parchment floor, raised outlined Stones, closed flower Buds, an original seed/droplet Spirit with one leaf, and compact raised Bloom/hedge cells visibly distinct from every other object.
- Keep collision occupancy unmistakable. Decorative foliage, textures, shadows, or particles must not hide a traversable/blocking cell.
- Cache geometry with `drawWithCache` or the repository equivalent; avoid allocations during animation.
- Keep simulation/rendering separate. Consume already-resolved transitions; never let animation timing affect rules.
- Prevent double input during a committed move through the existing state/controller, not arbitrary delays.

Tutorial levels 1–5:

- Draw guidance programmatically using a fingertip circle, fading trail, arrowhead, and `Animatable`.
- Preserve the existing verified tutorial direction script, step order, and progression exactly. Redesign only its visual rendering and logical-cell-to-screen mapping; do not choose a different merely-valid move.
- Never intercept touch, obscure Buds, or expose decorative guidance to TalkBack.
- Stop after the expected valid action and advance from actual state.
- Respect persisted tutorial state, reduced motion, optional direction controls, configuration changes, and process recreation.
- Never appear after Campaign level 5 or in Daily/Endless.
- Always expose the same four directional accessibility actions through semantics; when TalkBack/touch exploration is enabled, automatically provide an operable visible control surface even if optional direction buttons are otherwise disabled.

### 5.4 Completion

- Animate the already-decided board completion, then present a bounded 700–1,000ms Compose celebration with a branded flower opening, 24–36 petal/leaf particles, and the existing result-sensitive emoji as a smaller accent.
- Reduced motion shows a static petal halo and result accent without particle travel.
- Show stars, moves versus optimum, first-completion reward, Next, Replay/Retry, Back to Garden, and existing Share behavior.
- Make destinations mode-aware: Campaign returns to its Chapter/Garden hierarchy, Daily returns to Daily/Home, and Endless returns to Endless Garden. Do not label a non-Campaign destination `Back to Garden` when it routes elsewhere.
- Animate `+10 Coins` and the balance exactly once only when the transactional reward was granted. Replay/relaunch never shows or grants it again.
- Never let an interstitial interrupt Seed movement, Bloom growth, completion result, reward visibility, or the campaign-unlock moment. Preserve the existing safe display boundary after the completion experience.
- Chapter, Garden, final Campaign, Daily, and Endless completions use related but appropriately scaled treatments.
- At Campaign level 2,000, replace ordinary Next with `Enter Endless Garden`. Persist unlock before emitting the navigation/celebration effect.
- Final Campaign completion introduces a premium `Endless Garden Unlocked` card. Ad availability is never part of unlocking.
- If level 2,000 also makes an interstitial due, persist that due state but defer it through the entire unlock experience. Consume it once at the next boundary already eligible under existing policy; do not lose it or show it during unlock.

### 5.5 Endless Garden

- Before unlock: show the exact Campaign-completion requirement and actual progress, without a fake active CTA.
- After unlock: distinguish `Certified Starter • 1–100` from `Endless Garden • 101+` while presenting one continuous ordinal sequence.
- Show Continue, current ordinal, total completed, certified difficulty, current 100-level cycle progress, and next-maze readiness.
- While generating, show a calm seed-sprouting state with `Growing and checking a new maze…`; do not show fake percentage progress when total work is unknown.
- Prefer seamless pre-generation. If generation is interrupted or exhausts its bounded search, show Retry/Return Home and a typed recoverable state; never substitute a repeated or unverified board.
- Use friendly trust copy: `Created offline • Solvable • No repeats in saved history`. Keep hashes/certificate details in debug only.

### 5.6 Settings and supporting surfaces

- Preserve all existing settings and entitlement behavior.
- Group Appearance, Controls, Sound & Haptics, Accessibility, Privacy/Ads, and About into clear sections.
- Support System/Light/Dark, reduced motion, high contrast, sound, haptics, optional direction controls, and existing language settings.
- Put all player-visible text in string resources and retain RTL compatibility.
- Dialogs/sheets remain responsive, edge-safe, keyboard-safe where relevant, semantic, and dismissible without losing progress.

---

## 6. Motion, accessibility, and responsive rendering

- Motion explains Seed travel, Bud collection, Bloom creation, blocker contact, invalid input, completion, unlock, and new-level readiness.
- Use a small motion-token set: Seed travel approximately 80ms per traversed cell capped at 420ms; Bud pulse 120ms; Bloom stagger approximately 25ms per departed cell capped at 300ms; invalid bump 120ms; card/state transitions 180–260ms; completion at most 1,000ms.
- Do not delay persistence/navigation merely to finish decoration. Interruption/backgrounding/recomposition must settle on authoritative logical state.
- Reduced motion replaces path/particle choreography with short fades and immediate final state; honor system animator scale.
- Keep sound/haptics lightweight and respect system/user settings. Never vibrate or play a reward sound twice after restoration.
- Use stable keys, immutable models, cached paths/brushes, and allocation-conscious particle drawing. Profile recomposition.
- Generator/solver/database work never runs on the main thread or degrades active board animation.
- Provide TalkBack names/roles/states for level nodes, difficulty, reward costs, ad requirements, moves, Buds, stars, unlock progress, and actions. Expose Canvas board state through stable semantics/semantic overlays.
- Announce meaningful collection, invalid input, hint, reward, completion, and unlock events without narrating frames. Decorative guidance remains hidden from accessibility.
- Support optional cardinal-direction controls through the exact swipe transition path.
- Use at least 48dp touch targets, logical traversal, 200% font scale, non-color state cues, high contrast, and safe insets.
- Compact phone: single column and one-handed controls. Medium/tablet portrait: wider board and two-column summaries. Expanded/landscape: board left, HUD/actions right, content capped near 1,200dp. Preserve a square bounded board and state/scroll/focus across size changes.
- Verify at minimum 320×568, 360×800, 600×960, and 1,280×800 layouts plus split-screen/foldable-safe behavior supported by the project.

---

## 7. Endless content model

### 7.1 Stable sequence and immutable prefix

- Endless ordinals 1–100 map to the existing certified Auto Progressive definitions and never change.
- Use one-based `Long` ordinals throughout persistence, navigation arguments, UI keys, block math, and reports; never truncate them to `Int`.
- Arithmetic, persistence types, route arguments, UI keys, and ID formatting remain correct through `100 + 2^48 = 281,474,976,710,756` without pre-creating rows or models. Actual generation may terminate earlier through storage, finite content space, solver budget, or uniqueness exhaustion. Return a typed state rather than wrapping.
- Use a stable generated ID such as `auto-v<generation-segment>-<15-digit-ordinal>`; preserve existing IDs for 1–100.
- Introduce a generation-segment record containing at least `startOrdinal`, generator/profile/fingerprint/uniqueness versions, and namespace version. A future generator upgrade starts a new segment at the next not-yet-accepted ordinal; it never changes an accepted ID or regenerates accepted history under new rules.
- Store a fixed, versioned generation namespace. Do not use time, locale, device identity, thread count, completion time, Coin balance, ad state, or mutable catalog version in generation.
- Completing or previously having completed starter level 100 unlocks generated level 101. Preserve sequential unlock and active-attempt behavior.

### 7.2 Exact difficulty mix

Preserve the existing first 100 labels. For every complete generated block of 100 levels after the prefix, enforce exactly:

| Difficulty | Count |
| --- | ---: |
| Easy | 16 |
| Normal | 28 |
| Hard | 28 |
| Expert | 20 |
| Master | 8 |
| **Total** | **100** |

Use deterministic, versioned pacing templates that:

- Start each 100-level block with brief Easy/Normal relief.
- Rise by at most one adjacent band at a time; larger downward relief is allowed.
- Never place more than two identical bands consecutively.
- Never place adjacent Master levels.
- Follow every non-final Master with Easy or Normal.
- Spread Easy/Normal and Hard/Expert/Master through both halves of every 20-level chapter-sized span.
- Use exact certified profile gates. If a candidate misses its scheduled band, reject it; never relabel it merely to fill a slot.

Difficulty schedule repetition is allowed; board geometry, complete dynamics, solution grammar, and near similarity are not.

Use these exact five 20-level templates for every generated block, with `E=Easy`, `N=Normal`, `H=Hard`, `X=Expert`, and `M=Master`:

~~~text
Chapter 1: E E N E N H N E N H X E N H N E N H X M
Chapter 2: E N H N E N H X N H E N H X E N N H X M
Chapter 3: E N H N H X N H E N H X H N H X N H X M
Chapter 4: E E N H X M E N H X H N H X H N H X X M
Chapter 5: E N H X M N H X H N H X M N H X H X X M
~~~

Use the deterministic per-block board-size mix already validated for the Campaign extension:

| Difficulty | 5×5 | 6×6 | Total |
| --- | ---: | ---: | ---: |
| Easy | 12 | 4 | 16 |
| Normal | 14 | 14 | 28 |
| Hard | 4 | 24 | 28 |
| Expert | 0 | 20 | 20 |
| Master | 0 | 8 | 8 |
| **Total** | **30** | **70** | **100** |

Within each `(100-level block, difficulty)` group, order occurrences by ordinal. For zero-based rank `r`, total occurrences `t`, and 5×5 quota `q`, assign 5×5 exactly when `ceilDiv((r+1)*q,t) > ceilDiv(r*q,t)`; otherwise assign 6×6. Define `ceilDiv(a,b)=(a+b-1)/b` using checked `Long` arithmetic.

### 7.3 Candidate identity and seed derivation

For generated ordinal `o >= 101` and candidate attempt `a` in `0..65,535`, define with checked unsigned arithmetic:

~~~text
slotZero        = o - 101
candidateCounter = (slotZero << 16) OR a
~~~

Require `slotZero < 2^48`. The injective `CandidateKey` is the complete canonical tuple:

~~~text
"mazebloom-auto-progressive"
endlessBaselineRoot
generationNamespaceVersion
generatorVersion
certificationProfileVersion
fingerprintVersion
uniquenessProfileVersion
generationSegment
ordinal
candidateAttempt
scheduledDifficulty
scheduledBoardSize
~~~

`CandidateKey`, not a truncated digest, is the authoritative identity. Define the segment base exactly:

~~~text
namespaceBase = first8UnsignedBigEndian(
  SHA-256(lengthPrefixedUtf8(
    "mazebloom-auto-seed",
    endlessBaselineRoot,
    generationNamespaceVersion,
    generatorVersion,
    certificationProfileVersion,
    fingerprintVersion,
    uniquenessProfileVersion,
    generationSegmentId,
    generationSegmentStartOrdinal
  ))
)
~~~

Exclude ordinal, attempt, scheduled band/size, time, and mutable state from `namespaceBase`; ordinal/attempt are represented injectively by `candidateCounter`, while the complete CandidateKey records all identity fields. Derive a collision-free seed mapping inside the namespace:

~~~text
candidateSeed = namespaceBase
              + candidateCounter * 0x9E3779B97F4A7C15
              modulo 2^64
~~~

The multiplier is odd, so all candidate counters map bijectively to 64-bit seed patterns within one namespace. On a new namespace, reject a raw seed already present in Auto history. Persist the full CandidateKey plus the fixed-width lowercase hexadecimal seed. A unique key/seed is never treated as proof of a unique board; every resulting definition still passes complete duplicate rejection.

Reuse the existing constructive generator, exact transition engine, solver, difficulty analyzer, D4 canonicalization, fingerprints, and uniqueness rules. Do not create a weaker runtime implementation. Continue using only existing 5×5/6×6 objects and rules.

### 7.4 Bounded, resumable, ordered generation

- Generate only the next required ordinal and a small look-ahead cache; never attempt to materialize an infinite list.
- Visit attempts exactly `0..65,535` in 256-attempt deterministic windows. Slot-wide—not per-window—take the first 1,024 cheap-prefilter survivors by attempt ordinal into `SHORTEST_PROBE`; among their passes, take the first 128 by attempt ordinal into full SHORTEST/AUDIT. Later attempts are not promoted after either canonical slot quota is exhausted. Construction remains bounded by the existing maximum of 4,096 construction expansions per raw candidate. Window boundaries, caps, solver budgets, rejection rules, and versions are canonical.
- Persist a resumable checkpoint at window boundaries with baseline root, generation segment, target ordinal, and `historyRootAtStart`. Resume only when all equal current state; otherwise discard it and restart that ordinal at attempt 0. Restarting a killed worker cannot select a different winner or continue against an older registry.
- At most one generation coordinator owns an ordinal. Coalesce duplicate requests and use process-safe transaction/lease ownership.
- Parallel proof work is allowed only when a single ordered reducer accepts the lowest candidate attempt after every earlier attempt is resolved. A simpler bounded single-worker implementation is acceptable.
- Limit full-certification concurrency to one on typical devices unless measured repository evidence justifies another hard bound. Keep the existing solver's hard heap/state caps.
- If all windows exhaust, return typed `CONTENT_SPACE_EXHAUSTED`/`GENERATION_EXHAUSTED`. Do not advance, relax uniqueness/difficulty, rotate an old board, fetch content, or serve an uncertified fallback.
- Wall-clock time is non-canonical diagnostic data and never affects seeds, acceptance, order, hashes, or content identity.

### 7.5 Look-ahead policy

- Keep the immutable 1–100 prefix instantly available.
- Do not start post-prefix generation immediately on Campaign unlock. Begin when starter ordinal 97 becomes unlocked/reached, or immediately when migration finds starter level 100 already completed. Thereafter keep at most three certified future generated levels cached near the playhead.
- After a completion or rewarded Skip commits, atomically advance and request bounded replenishment.
- Do not generate while locked, during first-run onboarding, because Home recomposed, or in an uncontrolled periodic loop.
- Use the repository's current coroutine/application-scope/background-work conventions. Do not add WorkManager or another framework solely for this feature when existing architecture safely supports resumable work.
- If background execution is interrupted, continue deterministically when appropriate. No network is required.

---

## 8. Exact certification and non-repeat guarantee

A generated candidate may be accepted only after all of these pass:

1. Schema/board validation.
2. Deterministic construction replay.
3. Exact pristine-level solution under the mandatory solver budget.
4. Full required difficulty/AUDIT proofs and scheduled profile.
5. Singular canonical solution/replay selection.
6. Serialization round trip with identical definition/hashes.
7. Exact definition and all eight D4 rotations/reflections.
8. Complete dynamic fingerprint.
9. Canonical replay structural signature.
10. Solution-grammar fingerprint.
11. Existing hard near-duplicate and strict review-similarity rules.
12. Comparison against every bundled Campaign, Daily, and immutable Auto Progressive 1–100 entry plus every retained atomically committed ready look-ahead, accepted, or skipped generated Endless entry. Staged/in-flight candidates never influence another candidate's acceptance.

Create and verify the authoritative `endlessBaselineRoot` from the actual ordered 2,220 accepted records, canonical definitions/certificates/fingerprints, uniqueness versions, exact expected/performed counts, and canonical collision records—or derive it from existing canonical content/uniqueness roots plus the immutable Auto-prefix root. Never hash a free-form comparison report, timestamp, duration, path, host detail, or environment data. Package/reuse a compact immutable baseline uniqueness-index asset verified by that root; do not prepopulate 2,220 mutable Room progress rows. Query all baseline records without altering content roots. A baseline count/root mismatch disables generation with `BASELINE_MISMATCH`; never compare only against the latest block/history window.

Let `n=1` represent generated Endless ordinal 101. Accepting the `n`th generated level adds exactly `2,220 + (n - 1)` new prior-record pair obligations. Collision-complete indexes cover every obligation, although the full comparator need run only for exact-index hits and losslessly selected near candidates. After accepting `m` generated levels, the corpus has `2,220 + m` items and `C(2220 + m, 2)` unordered pairs. Use `BigInteger` in tools and canonical decimal strings in reports because future pair counts exceed signed 64-bit range.

Use exact hash maps for definition, canonical D4 geometry, complete dynamics, solution grammar, and replay structure. Confirm canonical payload equality when a digest matches. Maintain a separate collision-complete metric index per board size for the existing weighted mask distance; store/query all eight D4 transforms, return every candidate within radius 18, group by level, and run the authoritative comparator across every tied minimum-distance alignment. A persisted BK-tree or equivalent exact metric index is acceptable only when proven to have no false negatives.

Approximate nearest-neighbor/LSH may produce diagnostics but cannot decide acceptance. Safe pruning may use board size, Stone/Bud popcount lower bounds, and exact D4 mask bounds only when losslessness is proven. Add optimized-index versus brute-force equivalence tests over randomized and adversarial fixtures.

Use non-unique indexed digest columns and collision buckets for exact/D4/dynamic/grammar/structural fingerprints. On a digest match, compare canonical payloads or recompute/stream the canonical dynamic payload. Apply uniqueness constraints only to true identities such as stable level ID, ordinal, CandidateKey, and `(levelId, transformId)`; a digest alone is never authoritative.

When a candidate passes, one atomic Room transaction must:

- Store stable ID/ordinal, CandidateKey/seed, segment/versions, complete canonical serialized definition, canonical solution replay, full certificate payload, fingerprints/signatures, and checksums. Seeds are diagnostic/reproduction inputs, never the only authoritative storage.
- Insert its complete uniqueness/transform records.
- Append the next `historyRoot`.
- Mark it ready.
- Advance next-generation state once.
- Delete the consumed checkpoint.

If a derived index lives outside Room, block further generation until the committed row becomes index-visible; on failure enter `INDEX_REBUILD_REQUIRED`.

A crash before commit accepts nothing; a crash after commit finds the accepted row and cannot generate or advance it twice. Skipped levels remain in the uniqueness registry forever for retained history: they are seen, not reusable.

Maintain an append-only integrity chain:

~~~text
historyRoot[0] = SHA-256("mazebloom-auto-history-v1" | endlessBaselineRoot)
historyRoot[n] = SHA-256(historyRoot[n-1] | canonicalAcceptedRecord[n])
~~~

The accepted record contains stable ID/ordinal, CandidateKey/seed, logical definition/hash, solution/certificate/hash, difficulty/size, every canonical fingerprint/signature, and all relevant versions. Exclude timestamps, elapsed duration, paths, locale, device details, and worker order. Rejected candidates enter only bounded non-canonical summaries.

Indexes are derived data; baseline and committed definitions are authoritative. If an index is missing/corrupt, disable generation with `INDEX_REBUILD_REQUIRED` and rebuild exactly. If an authoritative committed row fails its hash/history chain, stop with `HISTORY_CORRUPT`; never silently reset or bypass it.

Reports distinguish theoretical pair coverage, exact index queries, and full authoritative comparator calls. Do not falsely report all theoretically covered pairs as linearly executed comparisons when exact indexes eliminated impossible collisions.

### 8.1 Guarantee scope

Reject repeats under:

- Exact definition.
- Rotation/reflection.
- Cosmetic/minor mask mutation caught by existing thresholds.
- Equivalent complete reachable dynamics.
- Canonical solution grammar.
- Structural replay signature.
- Hard-near and strict review-similarity rules.

Similar difficulty alone is not duplication. Subjective human perception cannot be mathematically guaranteed; retain nearest-neighbor diagnostics and honest human-review language.

### 8.2 Failure and version states

Model at least:

~~~text
READY
GENERATING
PAUSED
STORAGE_BLOCKED
INDEX_REBUILD_REQUIRED
BASELINE_MISMATCH
HISTORY_CORRUPT
GENERATION_EXHAUSTED
INDEX_SPACE_EXHAUSTED
UNSUPPORTED_PROFILE
~~~

Cancellation/process death may resume. Exhausting all 65,536 attempts for one ordinal is terminal for that generation segment: persist it, keep earlier levels playable, disable Next with honest Retry-after-update/Return Home handling, and require an explicit versioned generator/profile update. Never retry the same exhausted range indefinitely.

Previously accepted generated levels remain immutable and playable after updates. A new generation segment declares its namespace/version, effective starting ordinal, prior history root, and reason. It never resets history. If rules/fingerprint/uniqueness semantics change, re-certify the baseline and local accepted definitions before enabling generation. If exact cross-version comparison is impossible, use `UNSUPPORTED_PROFILE` rather than silently comparing incompatible fingerprints.

The current 2,220-item baseline is fixed for this namespace. Future bundled-content replacement/addition requires an explicit baseline migration and collision handling against retained local history; it is outside this task and must not happen accidentally.

---

## 9. Persistence and migration

Extend persistence through the existing architecture. Remain on v2 only when the byte-identical existing Room schema already supports every required state without a DDL/entity/index change. Any added Room table, column, index, or constraint requires schema v3, an explicit non-destructive v2→v3 migration, full v1→v2→v3 testing where v1 remains supported, and an exported schema.

Use cohesive equivalents of:

~~~text
AutoProgressiveState
GenerationSegment
GeneratedLevelRecord
GeneratedLevelProgress
UniquenessRecord
GenerationCheckpoint
~~~

Persist at least:

- Unlock state, `endlessBaselineRoot`, append-only `historyRoot`, next playable ordinal, accepted generated count, and next generation ordinal.
- Current generation segment/namespace/solver/profile/fingerprint/uniqueness versions.
- Accepted ID/ordinal/CandidateKey/seed, complete canonical serialized definition, canonical replay, full certificate and hashes, difficulty, board size, and lifecycle state.
- Completion/Skip/stars/best moves/replay and first-reward idempotency using current progress conventions.
- Compact exact/D4 transforms/dynamic/grammar/structural/near data required to prevent repeats and rebuild indexes.
- Deterministic window checkpoint, history root at checkpoint start, rejection counters, and typed terminal failure, if any.

Storage grows with accepted history, not rejected attempts:

- Keep bounded rejection summaries; never persist every failed board or reachable graph.
- Keep uniqueness records for every accepted or skipped level.
- Keep at most three future levels and a bounded number of decoded nearby boards.
- Do not copy bundled starter 1–100 into generated rows.
- Do not prepopulate unbounded rows or load complete generated history at startup.
- Retain every authoritative compact accepted definition, certificate, and uniqueness record. Only evict decoded/rendered caches; do not make future no-repeat behavior depend on successfully reconstructing data that was deleted.
- Never delete uniqueness history as automatic cleanup.
- Measure and report database growth per 1,000 generated levels.

If the app already offers `Reset progress`, it may reset visible scores/stars according to current policy, but it must preserve Endless unlock, accepted/skipped definitions, uniqueness/history roots, next unseen ordinal, generation segment/checkpoint compatibility, and first-reward/ad idempotency records. Continue from the first unseen Endless ordinal; never rewind or re-grant Coins. Any action that truly erases Endless history must be separate, explicitly warned, and added only if such destructive reset is already supported; state that previously seen generated puzzles may later reappear.

Migration from the real exported v2 schema must preserve all Campaign/Daily/Auto progress, active attempts, tutorials, Coins, rewards, skips, stars, replays, settings, entitlement, ad idempotency/counters, unlock state, and every existing level hash. Never use destructive migration in production.

---

## 10. Architecture and state flow

Respect current module boundaries. Intended dependency direction:

~~~text
Compose UI
  -> ViewModel / immutable UI state / one-shot effects
  -> use cases and repositories
  -> generator + exact solver + certifier + uniqueness registry
  -> Room/DataStore and versioned content assets
~~~

- UI never directly runs generation, solver, Room, ads, navigation, or Coin mutation.
- Generator/certifier remains pure deterministic Kotlin wherever possible and JVM-testable.
- Expose observable generation states such as `LOCKED`, `READY`, `GENERATING`, `EXHAUSTED`, and recoverable `ERROR`.
- Model navigation, celebration, ad request, snackbar, and unlock as one-shot effects so recomposition cannot repeat them.
- Persist authoritative progress before emitting navigation/celebration.
- Never open a level before certificate/uniqueness transaction commit.
- Do not retain all Campaign/generated definitions in memory.
- Use stable Compose keys, immutable models, narrow state subscriptions, and lifecycle-aware collection.

---

## 11. Debug and authoring support

Extend existing debug tools and keep them out of release:

- Direct-load Endless ordinal.
- Show generation segment, scheduled band/size, attempt/window, seed, rejection counts, solver metrics, and hashes.
- Reproduce a generated level from versions/ordinal/attempt.
- Compare any two bundled/generated IDs through every uniqueness tier and show the chosen D4 alignment/reason.
- Inspect baseline/local uniqueness counts and roots.
- Simulate process death during generation and deterministic resume.
- Simulate exhaustion, database error, ad unavailable, reduced motion, high contrast, and locked/unlocked mode.
- Preview redesigned screens/components in light/dark/high contrast, phone, 7-inch tablet, 10-inch tablet, landscape, and large font configurations.

Debug controls cannot be reached in release or alter production generation namespaces.

---

## 12. Automated tests and release gates

Add focused coverage without deleting, weakening, or replacing existing tests.

### 12.1 Baseline regression

- Verify the real constituent manifests sum to exactly 2,220. Expected composition is 2,000 Campaign + actual bundled Daily pool + 100 Auto Progressive prefix; do not double-count the prefix. Fail clearly if the repository differs.
- Pin `endlessBaselineRoot` over ordered accepted canonical records, certificates/fingerprints, uniqueness versions, exact expected/performed counts, and canonical collision records only; exclude all timing/path/host diagnostics.
- All existing definitions/IDs/seeds/hashes/replays/certificates remain unchanged.
- The baseline remains exactly `C(2220,2) = 2,463,090` unordered pairs with its existing zero-new-collision result.
- Auto Progressive 1–100 and the 16/28/28/20/8 distribution remain unchanged.
- Existing Campaign/Daily/Auto progress, economy, ads, tutorials, and unlock behavior pass.

### 12.2 Endless generation

- Golden CandidateKey/seed vectors for versions, ordinals, attempts, and unsigned 64-bit edges.
- Boundary cases at `Int.MAX_VALUE`, `Int.MAX_VALUE + 1`, and the maximum supported ordinal for Room bindings/queries, route parsing, saved state, Compose keys, ID formatting, schedule math, and UI display.
- Same inputs produce byte-identical candidate selection, certificate, and persisted record across retries, locale/timezone, thread counts, process recreation, checkpoint boundaries, and worker order.
- Checked ordinal/candidate arithmetic cannot overflow or wrap.
- Every generated 100-level block has exact band/board-size quotas and pacing.
- Candidates missing scheduled profile/proofs are rejected, never relabeled.
- Every candidate covers all 2,220 baseline records and complete accepted, atomically committed ready-look-ahead, and skipped local history; staged/in-flight candidates do not participate.
- Exact, D4, dynamic, grammar, structural, hard-near, and review-similarity fixtures reject the later candidate, including thresholds 6/7, 12/13, 18/19 and tied D4 minima.
- Optimized exact index results equal brute force over deterministic randomized and adversarial corpora.
- Missing/corrupt derived index disables generation, rebuilds from authoritative records, and never bypasses checks.
- Duplicate generation requests coalesce; one ordinal accepts one candidate.
- Crash/cancellation immediately before, during, and after atomic commit is idempotent.
- Skipped levels remain unique-history entries after relaunch.
- Window exhaustion returns the typed terminal state without an infinite retry or relaxed rule.
- Generator/profile cutover preserves history and begins a new explicit segment only at the next unaccepted ordinal.
- No generation network request and no main-thread BFS/database scan.

### 12.3 Persistence, economy, and ads

- Migrate the actual exported Room v2 fixture to v3 when v3 is required, and test full v1→v2→v3 where v1 remains supported.
- Preserve every prior progress/economy/ad/settings field and active attempt.
- A v2 user who already completed starter level 100 migrates with generated level 101 unlocked, without replaying level 100 or re-granting its reward.
- Do not prepopulate unbounded generated rows or duplicate 2,220 static definitions as mutable progress.
- +10 Coins is granted once on a generated level's first completion; replay grants nothing.
- Hint deducts 30 Coins once; insufficient balance exposes but never auto-starts rewarded flow. Assert the repository's actual rewarded outcome and amount without changing it.
- Rewarded Skip advances once, grants no Coins, persists, and remains in uniqueness history.
- Duplicate/late rewarded callbacks mutate once.
- Generation/recomposition/celebration do not alter first-completion/interstitial counters.
- Rewarded/interstitial never overlap; existing consent/cooldown/lifecycle/Remove Ads rules remain intact.
- If Campaign level 2,000 creates an interstitial-due state, it remains pending through unlock and is consumed exactly once at the next existing eligible boundary.
- Measure database growth per 1,000 accepted levels and test low-storage failure without deleting history.

### 12.4 UI and accessibility

- Tutorial guidance appears only on Campaign 1–5, follows state, and never intercepts input.
- Home and Endless locked/unlocked/starter/generated/loading/exhausted/retry states.
- Campaign/Garden/Chapter progress, lazy loading, stable keys, focus, and scroll restoration.
- Game ready/moving/hint/Skip/ad-loading/ad-unavailable/completed states.
- Reward appears only when actually granted; Campaign unlock/navigation emits once.
- Rapid tapping/swiping, rotation, background/foreground, and process recreation do not duplicate reward, ad, navigation, generation, or progress.
- No clipping/inaccessible action at required phone/tablet sizes and 100%, 130%, and 200% font scale.
- TalkBack traversal/semantics, optional direction controls, RTL, reduced motion, high contrast, light/dark, and non-color cues.
- No new bitmap/Lottie/network-art dependency.
- Board remains responsive and generation never runs on the main thread.
- System Back, pause, and process restoration resume the exact active Campaign, Daily, or Endless attempt and cannot generate, advance, reward, or count an interstitial merely through restoration.

### 12.5 Deterministic 1,000-level horizon

Future content cannot be exhaustively pre-certified. Add a desktop/JVM release gate using the exact production runtime algorithm and versions:

- Generate and fully certify the first 1,000 levels after immutable Auto Progressive 1–100.
- Validate ten complete blocks, each with exact difficulty and board-size quotas.
- The horizon introduces exactly `2,220,000 + 499,500 = 2,719,500` new baseline/horizon and horizon-internal pair obligations.
- An independent slow verifier checks the entire resulting 3,220-item corpus across exactly `C(3220,2) = 5,182,590` unordered pairs under every strict uniqueness tier, with zero new collision.
- Emit deterministic horizon/history roots, accepted/rejection counts by cause, theoretical pair coverage, exact index-query counts, actual full-comparator counts, solver metrics, peak heap, and non-canonical p50/p95 duration.
- Do not bundle the 1,000 horizon boards as another finite pack. Runtime still certifies every subsequently accepted level before display.
- Make the horizon task checkpointed/resumable without making checkpoint timing canonical. Its valid verification stamp is keyed by exact `endlessBaselineRoot`, rules, generation namespace/generator, solver, certification profile, fingerprint, uniqueness, schedule/tooling versions, and relevant content-tool code hash.

A smaller fast horizon may run during ordinary development, but this one-go delivery and release-like verification require the full independent 1,000-level gate. `bundleRelease` verifies a current exact-key stamp rather than regenerating the full horizon on every invocation; a stale/missing stamp fails closed.

### 12.6 Human difficulty truthfulness

Automatic profile certification proves machine metrics, not perceived challenge or fun. If existing project evidence shows weak assigned-versus-perceived difficulty correlation, preserve it in reports and do not claim Expert/Master human validation.

- Generate a deterministic review worksheet sampling each band across the 1,000-level horizon and nearest non-colliding neighbors.
- Do not fabricate playtest results.
- Representative human playtesting of Hard/Expert/Master and on-device latency/heat/battery remain production-promotion blockers until actually completed.

---

## 13. Performance and storage safety

- Zero generator/solver/database work on the main thread.
- No eager unbounded list, bulk future generation, or unbounded Room prepopulation.
- Keep only current/nearby decoded boards and at most three certified future levels in memory.
- Maintain authoritative compact definitions/certificates and uniqueness records for all accepted/skipped levels. Evict decoded caches and rejected diagnostics, never accepted uniqueness history.
- Treat 128 MiB as the initial hard in-memory generator/index working-set ceiling, not a lifetime database cap. Persistent history grows linearly while safe storage is available. Before each commit, perform deterministic payload-size accounting and a separate operational free-space check; when storage is unsafe, enter `STORAGE_BLOCKED`, preserve all prior levels, and resume the same candidate after space becomes available. Never choose a different candidate based on storage or delete history automatically.
- Preserve existing solver state/heap caps and one full mobile certification worker. Add fixed-work yielding/cancellation without affecting candidate order.
- Keep generation off cold-start critical path and active gameplay frames. Start post-prefix preparation only when starter ordinal 97 is reached/unlocked, or immediately on migration when starter level 100 is already complete, matching section 7.5.
- Report peak heap, raw/probe/full attempts, rejection reasons, database growth, and non-canonical latency. Timing never affects identity.
- Release artifacts exclude verbose traces, reachable graphs, horizon boards, debug tools, and preview-only resources.

Before production claims, benchmark at least one representative low-memory, one mid-range, and one high-end Android device for UI smoothness, generation latency, memory, heat, and battery. This task may report those checks as pending when hardware is unavailable; it may not invent results.

---

## 14. Documentation

Create/update actual project documentation:

- `docs/UI_DESIGN_SYSTEM.md`: tokens, components, screens, responsive/accessibility/motion behavior, and preview references.
- `docs/AUTO_PROGRESSIVE_ENDLESS.md`: truthful guarantee, baseline/prefix, sequence/seed/version rules, certification, indexes, history roots, persistence, exhaustion/storage/recovery, and Clear Data/uninstall limitation.
- `docs/ARCHITECTURE.md`: actual modules/types/state flow.
- `docs/CONTENT_PIPELINE.md`: runtime generation and fast/full horizon tasks with reproduction commands.
- `docs/MONETIZATION_POLICY.md`: confirm unchanged Coin/hint/Skip/interstitial/ad-safety behavior.
- `docs/IMPLEMENTATION_REPORT.md`: baseline, changed files, migration, UI results, generator/certification evidence, commands/results, warnings/skips, performance/storage measurements, and external release needs.

Document what was implemented and verified, not aspirations. Preserve older reports rather than silently rewriting historical evidence.

---

## 15. Required verification

Discover and run the repository's real module/task names. At minimum run equivalents of:

~~~bash
./gradlew --version
./gradlew projects
./gradlew tasks
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
./gradlew check
./gradlew verifyBundledContentFast
./gradlew certifyCampaignFull
./gradlew certifyAutoProgressiveHorizon
./gradlew verifyBundledContent
./gradlew minifyReleaseWithR8
./gradlew bundleRelease
~~~

Run migration tests from the actual exported v2 schema plus relevant generator/core/data/ViewModel/Compose/monetization/accessibility tests. A release-like bundle must depend on the exact baseline/horizon verification stamp so it cannot package stale evidence.

For this UI/runtime phase, `certifyCampaignFull` runs in verification-only mode against the frozen 2,220 assets. It may refresh derived non-content caches/stamps but must not rewrite definitions, published certificates, manifests, or canonical content/uniqueness roots.

Run connected/device tests only with a compatible emulator/device. Otherwise report `SKIPPED` with the exact reason; previews/host tests are not physical-device evidence.

Do not hide failures. Fix failures introduced by this work. Separate reproducible pre-existing failures. Keep advisory warnings visible, do not call them errors, and do not add avoidable new warnings.

---

## 16. Definition of done

This phase is complete only when all applicable items are true:

- Home, Campaign hierarchy, Game, Completion, Endless Garden, and Settings share a cohesive living-paper-garden design.
- Light/dark/high-contrast/reduced-motion and phone/tablet/large-font behavior are verified as far as available tooling permits.
- Completion uses a branded flower/petal treatment while retaining the existing result emoji as a restrained accent and preserving reward/ad semantics.
- Existing 2,220 definitions, roots, certificates, and 2,463,090-pair baseline evidence have no drift.
- Auto Progressive 1–100 remains the immutable certified prefix.
- Campaign completion unlocks Endless Garden exactly once; starter level 100 advances to generated level 101.
- Level 101 onward is generated deterministically, offline, sequentially, and just in time with at most three ready levels.
- Every generated level is exactly solved, scheduled-band certified, round-tripped, and checked against the complete bundled/retained uniqueness registry before display.
- No exact, D4, dynamic, grammar, structural, hard-near, or strict review-similarity repeat is accepted.
- Accepted and skipped generated levels remain non-repeatable across process death, relaunch, normal updates, and non-destructive migrations while history is retained.
- Every generated 100-level block has exactly 16 Easy, 28 Normal, 28 Hard, 20 Expert, and 8 Master plus the required board-size/pacing rules.
- Generation is atomic, resumable, bounded, ordered, off-main-thread, storage-aware, and fail-closed.
- Room v2 data survives; v3 is added only when needed with tested non-destructive migration paths.
- Coin, hint, rewarded Skip, interstitial, consent, entitlement, and fake/no-op ad behavior remain correct.
- The 1,000-level horizon passes all 2,719,500 new obligations and the independent 5,182,590-pair full-corpus verifier with zero new collision; it is not shipped as a pack.
- Relevant unit/content/UI/migration/lint/check/debug/R8/release bundle verification passes, or any external/pre-existing blocker is reported precisely.
- No production IDs, secrets, backend, online dependency, commit, or push was introduced.

---

## 17. Final response

Finish with a concise evidence-based report containing:

1. Player-visible UI improvements.
2. Actual module/type/schema changes.
3. Preservation result for the 2,220 baseline and Auto Progressive 1–100.
4. Endless generation, difficulty, certification, persistence, and non-repeat behavior.
5. Horizon attempts/acceptances/rejections; 2,719,500 new obligations; all 5,182,590 independent pair checks; roots; heap/storage/timing evidence.
6. Economy, rewarded Skip, and interstitial regressions.
7. Exact commands and pass/fail/skip outcomes, unit-test and warning counts.
8. Debug APK, release AAB, reports, and documentation paths.
9. Device/emulator status plus remaining manual visual, accessibility, performance, difficulty, and battery QA.
10. Remaining external ad/consent/privacy/signing/Play Console requirements.

Never claim that all future levels were pre-certified, finite boards provide literal mathematical infinity, automatic bands prove human difficulty, device QA ran when it did not, reinstall preserves history without restore, or live ads are production-ready without external configuration.

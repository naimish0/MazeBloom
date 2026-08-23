# Endless Garden generation and integrity

## Truthful guarantee

Endless Garden is practically unbounded, not mathematically infinite. It creates and certifies puzzles offline and rejects repeats against the fixed 2,220-level baseline plus every retained generated Ready, Completed, or Skipped definition. This guarantee lasts only while local app data is retained; Android Clear Data or uninstall erases local generated history and a previously seen generated puzzle may later reappear.

Ordinals 1–100 are the byte-stable certified `progressive-*` prefix. Generated ordinals begin at 101 and use one-based `Long` values through `100 + 2^48`. Generated IDs are `auto-v<segment>-<15-digit-ordinal>`. The schedule repeats five pinned 20-level templates and gives every complete 100 block exactly 16 Easy, 28 Normal, 28 Hard, 20 Expert, 8 Master, with 26 5×5 and 74 6×6 boards.

## Candidate identity

The authoritative CandidateKey stores the full namespace, pinned baseline root, all generator/certifier/fingerprint/uniqueness versions, segment, ordinal, attempt, scheduled band, and board size. The seed namespace is the first unsigned big-endian 64 bits of the specified length-prefixed SHA-256 digest. Candidate counters are `(ordinal - 101) << 16 | attempt`; multiplication by `0x9E3779B97F4A7C15` is modulo 2^64. Tests pin unsigned boundary vectors through ordinal `281474976710756`.

Attempts run in deterministic 256-attempt windows. Construction is bounded and uses three metric-aware deterministic variants within the 4,096-expansion ceiling. Unconditional metric-radius collisions are rejected before the probe. Canonical probe replays feed lossless structural, grammar, and strict-near checks before the single full solver/AUDIT worker. A segment exhausts rather than weakening a profile. Bundled versioned generator upgrades start a new recorded segment at the same unaccepted ordinal, keep prior history, and use a new namespace.

## Certification and no-repeat registry

An accepted level passes schema validation, deterministic construction, SHORTEST_PROBE, full SHORTEST/AUDIT, scheduled difficulty, complete open-cell coverage, canonical serialization round-trip, exact definition/D4 geometry, complete dynamics, replay structure, solution grammar, and both hard and review similarity rules. It is compared with all Campaign, Daily, starter, and retained generated history. A seed or digest is never treated as proof of board identity.

`endless-baseline.json` pins 2,220 records, 2,463,090 unordered baseline pairs, 11,691,120 D4 alignment comparisons, the constituent roots, and `endlessBaselineRoot`. A mismatch disables generation.

## Room v6 and recovery

Room v3 added `auto_progressive_state`, `generation_segments`, `generated_levels`, `generated_uniqueness`, and `generation_checkpoint`; v6 rebases installs with no accepted generated boards onto the new profile-v3 baseline while preserving completion, skip, and coin data. Active attempts for replaced Campaign 6–2,000 and Progressive definitions are cleared; Tutorial attempts remain. An existing generated history is never silently rewritten: it enters `BASELINE_MISMATCH` because its uniqueness proof references the retired baseline. Starter definitions are not copied into Room. Accepted definition/certificate/fingerprint data and all eight transforms commit atomically with the next append-only history root and ready state.

Only the current level and at most three future generated levels are requested. Preparation starts at starter ordinal 97 or when migration finds starter 100 already complete. Skipped definitions remain uniqueness history. Typed failures preserve earlier playable levels and never serve an uncertified fallback.

Checkpoints persist only canonical 256-attempt boundaries and resume with the same survivor quotas and rejection totals. The retained serialized registry is conservatively capped at 21 MiB to keep decoded definitions, fingerprints, and eight-way geometry indexes below the 128 MiB working-set ceiling. A certified row is limited to 1 MiB, and commit requires that payload plus an 8 MiB operational reserve. `STORAGE_BLOCKED` retains the checkpoint and history; freeing space retries the same deterministic candidate. The app never deletes accepted uniqueness history automatically.

## Release horizon evidence

The profile-v3 release gate accepted ordinals 101–1,100 in 290,031 candidate attempts in a single generation segment. All ten 100-level blocks met the exact difficulty and 26/74 board-size quotas. It checked all 2,719,500 new obligations and independently compared all 5,182,590 pairs in the combined 3,220-item corpus with zero new collision.

The pinned horizon root is `1736d76e3b71ba85e02fecb57fda7bba3577204b9d06bdfcb5778933eaabd85c`; the resulting history root is `fc40c7fa694d58fe7ba6b09e7a55070c359f6b5cb3890cb54b43974e25229cd3`. These boards are evidence only and are not shipped as a finite pack. Runtime still certifies each actual accepted level before display.

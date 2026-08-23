# Content pipeline

The application packages the exact files under `app/src/main/assets/content`:

- `campaign.jsonl`: the historical first-100 source archive; only Tutorial records 1–5 remain authoritative and byte-preserved.
- `campaign/campaign-manifest.json`: the lightweight 20-Garden/100-Chapter catalog and direct level index.
- `campaign/garden-XX/chapter-YY.json`: 100 independently hashed shards of 20 levels each.
- `daily.jsonl`: 120 separately selected offline levels from pool version 2.
- `progressive.jsonl`: the versioned 100-level Auto Progressive pack unlocked after Campaign completion.
- `manifest.json` and `certification-stamp.json`: versions, counts, roots, attempts, rejection totals, and the release certification result.

The mutable Campaign from level 6 through 2,000 has 1,995 levels: 319 Easy, 552 Normal, 562 Hard, 405 Expert, and 157 Master; 559 are 5×5 and 1,436 are 6×6. The repeating extension from level 101 uses 494 5×5 and 1,406 6×6 boards; scheduled Hard boards after level 100 are all 6×6. Together with the five immutable Tutorials, Campaign contains exactly 2,000 levels across 20 Gardens and 100 Chapters. Daily contains 30 levels in each of Easy, Normal, Hard, and Expert; 40 are 5×5 and 80 are 6×6. Auto Progressive repeats 16 Easy, 28 Normal, 28 Hard, 20 Expert, and 8 Master with 26 5×5 and 74 6×6 boards.

`generateBundledContent` is the only intentional update path. It preserves the prefix, deterministically generates the Campaign extension, Daily pool, and Progressive pool, then writes assets plus `reports/content/certification-summary.json`, the collision audit, and a blank human-review worksheet. Each candidate is exactly solved and profiled before strict geometric, dynamic, structural, solution-grammar, hard-near, and review-similarity acceptance. Progressive generation starts only after the complete Campaign and Daily registries exist, so acceptance is globally unique rather than merely unique within the new pack.

`verifyBundledContentFast` verifies the exact package inputs, schema and hierarchy, counts, quotas, shard checksums, ordered roots, five-Tutorial compatibility roots, and certification stamp. `certifyCampaignFull` additionally re-solves all 2,220 definitions and exhaustively compares 2,463,090 unordered pairs across 11,691,120 minimum D4 alignments. Only a disclosed collision wholly inside Tutorials 1–5 may be grandfathered; a collision involving Campaign 6–2,000, Daily, or Progressive fails certification.

Debug packaging and `check` depend on the fast gate. Release bundling depends on full certification. Neither path regenerates content. A checksum change requires an intentional generation/version review. Human approval is a separate blank field and is never inferred from automatic certification.

The immutable Auto Progressive 1–100 asset is the Endless Garden starter prefix, not the end of the mode. Ordinal 101 onward is constructed and certified just in time on-device against the exact 2,220 baseline plus retained local generated history. Finite 5×5/6×6 puzzle space means this is truthfully “practically unbounded,” not literal infinity; typed exhaustion, integrity, index, and storage states fail closed.
# Endless verification commands

`node tools/verify-endless-baseline.mjs` derives and checks the canonical 2,220-record baseline root. `./gradlew :app:certifyAutoProgressiveHorizon` runs the checkpointed production-algorithm 1,000-level horizon and the independent 3,220-item verifier; `--count=100 --probe-namespace` is a non-release diagnostic. `./gradlew :app:verifyEndlessHorizonStamp` fails if any exact key, relevant tooling hash, segment namespace, count, root, or pair obligation is stale. `bundleRelease` depends on that stamp verifier but does not regenerate the horizon.

Horizon checkpoints live under `build/endless-horizon`. The only packaged horizon file is the small verification stamp; the 1,000 boards are never copied into app assets. Machine evidence is written to `reports/content/endless-horizon-1000.json`, and `reports/content/endless-human-review-worksheet.csv` samples every band with blank human-review fields.

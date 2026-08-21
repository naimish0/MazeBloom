# Content pipeline

The application packages the exact files under `app/src/main/assets/content`:

- `campaign.jsonl`: exactly 100 levels in five 20-level chapters.
- `daily.jsonl`: 120 separately selected offline levels.
- `manifest.json`: versions, counts, aggregate hashes, attempts, and rejection totals.

Campaign quotas are 70 5×5 and 30 6×6 boards. Difficulty distribution is 5 Tutorial, 15 Easy, 20 Normal, 30 Hard, 25 Expert, and 5 Master. Daily contains 80 5×5 and 40 6×6 boards.

`generateBundledContent` is the only intentional update path. It writes assets, `reports/content/certification-summary.json`, and the human-review worksheet. `verifyBundledContent` reads the exact package inputs and checks schemas, counts, canonical arrays, record/aggregate hashes, and profile metrics. Android unit tests independently replay exact production rules/BFS and enforce D4/hard-near uniqueness across all 220 boards. Packaging and `check` depend on verification.

A checksum change requires an intentional content/version review. Human approval is a separate blank field and is never inferred from automatic certification.

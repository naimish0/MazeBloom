# Solver and generator

The production solver performs exact BFS over `(seedCell, bloomMask, remainingBudMask)` using the production transition function. Directions expand in `UP, RIGHT, DOWN, LEFT` order. The first complete solved depth yields the exact optimum and lexicographically canonical replay. Counts saturate at `Long.MAX_VALUE`; budget exhaustion is distinct from unsolvability. Exact-depth layered counting handles optimal+1/+2 without incorrectly reusing ordinary BFS deduplication.

Budgets are explicit: shortest 250k expanded/300k discovered/200k frontier; audit/minimum-Bloom proof 500k/550k/500k; current-state hints 100k/125k/75k. Hints execute off the main thread and never guess after an unsolvable or budget-exceeded result.

Generation uses the pinned SplitMix64 algorithm, fixed 16-digit base seeds, ordinal candidate seeds, bounded width-128 forward beam construction, exact pristine BFS, full reachable-graph analysis, exact minimum Bloom-stop proof, profile gates, D4 geometry, and hard near-duplicate rejection. Accepted order depends only on candidate ordinal, not time or worker completion.

`tools/content-cli.mjs` provides generate, solve, analyze, certify, dedupe, replay, render, and verify commands. `tools/generate-content.mjs` caps each pool at 150,000 attempted ordinals in this implementation, below the contract maxima. Reports never include timing in canonical acceptance/checksums.

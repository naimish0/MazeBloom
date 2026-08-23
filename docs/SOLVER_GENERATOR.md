# Solver and generator

The production solver performs exact BFS over `(seedCell, bloomMask, remainingBudMask)` using the production transition function. Directions expand in `UP, RIGHT, DOWN, LEFT` order. The first complete solved depth yields the exact optimum and lexicographically canonical replay. Counts saturate at `Long.MAX_VALUE`; budget exhaustion is distinct from unsolvability. Exact-depth layered counting handles optimal+1/+2 without incorrectly reusing ordinary BFS deduplication.

Budgets are explicit: shortest 250k expanded/300k discovered/200k frontier; audit/minimum-Bloom proof 500k/550k/500k; current-state hints 100k/125k/75k. Hints execute off the main thread and never guess after an unsolvable or budget-exceeded result.

Generation uses pinned SplitMix64 vectors and SHA-256-derived, length-prefixed namespace seeds isolated by Garden/difficulty/board-size. Profile-v3 Campaign and Progressive strata consume ordinals `0..<100000` per difficulty/size stream. A width-64 forward beam constructs legal histories; exact pristine BFS, full reachable-graph analysis, exact minimum Bloom-stop proof, coverage, profile, and strict uniqueness gates then decide acceptance. Accepted order depends only on slot and candidate ordinal, never time or worker completion.

Uniqueness covers exact D4 geometry, complete dynamic graph, canonical structural replay, solution grammar, and all tied minimum D4 alignments at mask distances 6/12/18. Review-similarity is also a hard generation rejection. The final verifier exhaustively compares all 1,999,000 Campaign pairs, Campaign/Daily, Daily/Daily, existing/Progressive, and Progressive/Progressive pairs: 2,463,090 total. Only collisions wholly inside the five checksum-locked Tutorials are grandfathered and disclosed.

`tools/content-cli.mjs` provides generate, solve, analyze, full/fast certify, compare, dedupe, replay, render, and verify commands. Generation/certification run with a 512 MiB Node heap through Gradle. Reports never include timing in canonical acceptance/checksums.

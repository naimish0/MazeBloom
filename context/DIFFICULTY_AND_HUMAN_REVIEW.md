# Difficulty and human review

Difficulty labels are provisional predictions. Certification profile version 3 raises both the shortest-solution floor and the required decision structure for every band. Only Campaign Tutorials 1–5 remain under their checksum-locked v1 contract. Campaign levels 6–2,000, Daily, Progressive, and future Endless definitions are certified under v3.

| Band | Published v1/v2 moves | v3 moves | Decisions | Bloom stops | Doomed states | Dependency | Max forced ratio |
|---|---:|---:|---:|---:|---:|---:|---:|
| Tutorial | 1–4 | 2–5 | 1 | 1 | — | — | 0.90 |
| Easy | 3–7 | 4–8 | 2 | 1 | — | — | 0.80 |
| Normal | 5–10 | 6–11 | 3 | 1 | 2 | — | 0.70 |
| Hard | 7–13 | 8–14 | 4 | 2 | 2 | 3 | 0.65 |
| Expert | 9–16 | 10–17 | 5 | 2 | 2 | 4 | 0.60 |
| Master | 11–20 | 12–20 | 6 | 3 | 3 | 5 | 0.55 |

The profile retains optimal moves and path count, reachable and doomed states, branch-depth and wrong-branch evidence, forced-move ratio, branching histogram, greedy traps, exact minimum Bloom-assisted stops, canonical creator-to-use distance, and optimal+1/+2 near-path counts with saturation flags. These machine gates test consistency and pacing proxies; clarity, relief, delight, and perceived repetition remain human-review questions.

The bundled automated report is [certification-summary.json](../reports/content/certification-summary.json). The [human-review-worksheet.csv](../reports/content/human-review-worksheet.csv) contains 503 review rows: a 283-level stratified Campaign sample plus every Daily and starter-prefix definition. [endless-human-review-worksheet.csv](../reports/content/endless-human-review-worksheet.csv) adds deterministic Easy/Normal/Hard/Expert/Master horizon samples and their nearest non-colliding neighbors. Reviewer, attempts, time, Undo/Restart/Hint usage, perceived difficulty, duplicate concern, decision, and notes fields remain intentionally blank.

No human playtest data has been entered. Automated certification passes, but the campaign is not human-validated or production-release validated. Recalibration should preserve raw metrics and use representative players rather than changing labels from solver depth alone.

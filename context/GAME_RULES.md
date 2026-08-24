# Game rules

- Coordinates start at top-left. `cellIndex = y * width + x`; packed bits use `1L shl cellIndex`.
- A level is a square 5×5 or 6×6 board containing Floor, permanent Stone, 1–7 Buds, and one mover. The engine retains the stable `seedCell` name; after 10 Campaign completions the player may present that mover as an owned Animal Companion.
- One action is `UP`, `RIGHT`, `DOWN`, or `LEFT`.
- The mover slides until the next cell is outside the board, Stone, or Bloom that existed before the action.
- Every entered cell is recorded in order. Buds are collected when entered, including intermediate cells.
- After the complete slide, every departed cell becomes Bloom atomically. The resting cell never becomes Bloom, and newly created Bloom cannot stop its own swipe.
- A blocked direction is an exact no-op and emits only `INVALID_MOVE`.
- Collecting the final Bud does not stop the slide early. Completion is marked only after the final stop and Bloom commit.
- If Buds remain and no adjacent direction can move, status is `DEAD`. A mobile but unsolvable position is `DOOMED` analysis, not runtime `DEAD`.
- Undo restores the exact previous immutable snapshot. Restart restores the pristine state. Terminal states reject movement.

Valid event order is `SHIFT_STARTED`, per-cell `SEED_ENTERED` and optional `BUD_COLLECTED`, one `BLOOM_CREATED`, then exactly one of `LEVEL_COMPLETED`, `DEAD_STATE`, or `STATE_STABLE`.

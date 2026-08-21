# Daily Bloom

Daily Bloom is fully offline and unlocks after ten campaign completions. Pool version 1 contains 120 certified boards.

For pool size `P`, selection uses a stable pool-version offset and `floorMod(epochDay + offset, P)`. The stored selection includes challenge key, local date, epoch day, pool version, full level definition, and display-only first-open time.

The repository advances only when the observed local date is strictly later. Same-date reopen and clock/timezone rollback keep the active selection pinned; forward movement advances once. Completion keys are idempotent, so one date cannot increment history/streak twice. A missed day resets the numeric streak without deleting history or best progress.

The pool cycles after 120 forward dates. Offline data cannot prevent deliberate clock manipulation, reinstall/app-data deletion, device cloning, or replay after reset; MazeBloom makes no anti-cheat or global-competition claim.

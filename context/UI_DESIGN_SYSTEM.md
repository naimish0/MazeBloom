# MazeBloom UI design system

MazeBloom uses a “living paper garden” system implemented entirely with Compose drawing, shapes, paths, and Material 3 primitives. No network art, per-level bitmaps, Lottie files, or downloadable fonts are used.

## Semantic tokens

The light palette uses parchment `#F7F3E8`, paper `#FFFDF7`, fern `#2F6B4F`, fern container `#DCEBD8`, terracotta `#C86B4A`, warm gold `#E6B85C`, ink `#1E2B24`, and muted ink `#657168`. Dark mode uses garden `#101813`, paper `#17231C`, fern `#8ED09B`, fern container `#244D36`, terracotta `#F0A184`, and ink `#EDF4EC`. Board-only roles live in `LocalBloomRoleColors`: Stone, Bloom, Bud, Seed, and board paper. High contrast is a separate palette, not an alias for dark mode.

Spacing follows 4dp increments. Primary touch targets are at least 48dp. Paper cards use 16–24dp radii with tonal separation and restrained elevation. The board remains square and capped at 560dp; layouts switch to board-left/action-right beyond 700dp.

## Components and screens

The Home hero uses a programmatically drawn six-petal brand mark. Campaign, Garden, and Chapter screens retain stable lazy-list/grid keys and show progress using shape plus text, not color alone. Game keeps the board visually dominant, with its HUD and action dock adapting between compact and wide layouts. The first five Campaign levels show a programmatically drawn moving finger guide that never handles pointer input. Completion uses a bounded Compose confetti layer, a small result-sensitive emoji accent, and a bottom result sheet.

`LivingGardenLightPreview` and `LivingGardenDarkPreview` provide IDE-renderable token/brand specimens. Static board floor and Stone geometry use a `drawWithCache` layer; animated Seed, Bud, Bloom, and hint state draw separately so frame updates do not rebuild the static geometry.

Endless Garden is always discoverable on Home. Its screen has distinct locked, starter 1–100, generated 101+, generating, ready, and terminal-integrity states. Player-facing text says “Endless Garden”; stable `progressive-*` IDs and internal compatibility names remain unchanged.

## Accessibility and motion

Board state, direction actions, reward/ad requirements, level state, and tutorial guidance have stable descriptions. Direction controls use the exact swipe transition path. Controls remain usable at large font scales and use text/shape/outline cues in addition to hue.

Seed/Bloom motion is logical-state-driven and never delays persistence. Reduced motion replaces tutorial and result choreography with settled states or short fades. System/Light/Dark, reduced motion, high contrast, sound, haptics, and optional direction controls are persisted preferences.

Host lint, resource compilation, debug/release Compose compilation, and R8 verification pass. A previously connected Samsung device ran the then-current three-test instrumentation suite successfully, but it was unavailable for the final v1.1 run; final manual screenshots, TalkBack traversal, 200% font-scale clipping, RTL, rotation/process restoration, and low/mid/high-device motion/thermal checks remain explicit production-promotion work.

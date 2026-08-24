# MazeBloom UI design system

MazeBloom uses a “living paper garden” system implemented entirely with Compose drawing, shapes, paths, and Material 3 primitives. No network art, per-level bitmaps, Lottie files, or downloadable fonts are used.

## Semantic tokens

Living Garden is the default parchment, fern, terracotta, and amber palette. Rose Garden adds plum, coral, and lime; Moonlit Pond uses blue, indigo, and teal; Golden Meadow uses ochre, apricot, and meadow green. Each theme has coordinated light and dark Material 3 schemes. Board-only roles live in `LocalBloomRoleColors`: Stone, Bloom, Bud, Seed, and board paper, with matching values for every theme and brightness. High contrast remains a separate override, not an alias for dark mode or a color theme.

Spacing follows 4dp increments. Primary touch targets are at least 48dp. Paper cards use 16–24dp radii with tonal separation and restrained elevation. The board remains square and capped at 560dp; layouts switch to board-left/action-right beyond 700dp.

## Components and screens

The Home hero uses a programmatically drawn six-petal brand mark. Campaign, Garden, and Chapter screens retain stable lazy-list/grid keys and show progress using shape plus text, not color alone. Game keeps the board visually dominant, with its HUD and action dock adapting between compact and wide layouts. The first five Campaign levels show a programmatically drawn moving finger guide that never handles pointer input. Completion uses a bounded Compose confetti layer, a visible animated flower/leaf/sparkle emoji layer, fixed flower emojis, the selected Companion after its level-10 unlock, and a bottom result sheet. Reduced motion settles those same visual cues without removing the celebration.

Animal Companions use deterministic Canvas paths and shapes rather than platform emoji or remote sprite files. The 100-entry catalog has separate mammal, bird, insect, garden/reptile, and aquatic renderers, then traits such as ears, tails, wool, masks, antlers, wings, crests, beaks, antennae, shells, fins, tusks, gills, and tentacles create recognizable variants. Motion profiles cover bounce, hop, prowl, waddle, flap, hover, scuttle, crawl, swim, and pulse. The adaptive Companion grid supports family filters, permanent ownership and selection states, dynamic pricing, large animated previews, and stable item keys. Locked cards keep Coin purchase and rewarded-ad actions visually separate, show persistent scoped credit as `earned / price`, reduce the displayed Coin remainder, and disable the ad action when ad services are unavailable.

“Continue growing” launches the next playable level with Home as its explicit return target; Back therefore returns directly to Home. Share waits for pressed-state cleanup, captures the complete settled celebration screen as a PNG, and sends it with the package-specific Play Store URL through Android's chooser.

`LivingGardenLightPreview` and `LivingGardenDarkPreview` provide IDE-renderable token/brand specimens. Static board floor and Stone geometry use a `drawWithCache` layer; animated mover, Bud, Bloom, and hint state draw separately so frame updates do not rebuild the static geometry. Before 10 Campaign completions the original Seed Spirit remains visible; afterward the selected owned Companion is a cosmetic renderer over the unchanged `seedCell` engine state.

Endless Garden is always discoverable on Home. Its screen has distinct locked, starter 1–100, generated 101+, generating, ready, and terminal-integrity states. Player-facing text says “Endless Garden”; stable `progressive-*` IDs and internal compatibility names remain unchanged.

## Accessibility and motion

Board state, direction actions, reward/ad requirements, level state, and tutorial guidance have stable descriptions. Direction controls use the exact swipe transition path. Controls remain usable at large font scales and use text/shape/outline cues in addition to hue.

Mover/Bloom motion is logical-state-driven and never delays persistence. Companion animation also respects Reduced motion, which replaces travel, idle, tutorial, and result choreography with settled states or short fades. Living Garden/Rose Garden/Moonlit Pond/Golden Meadow, System/Light/Dark appearance, reduced motion, high contrast, sound, haptics, and optional direction controls are persisted preferences.

Host lint, resource compilation, debug/release Compose compilation, and R8 verification pass. A previously connected Samsung device ran the then-current three-test instrumentation suite successfully, but it was unavailable for the final v1.1 run; final manual screenshots, TalkBack traversal, 200% font-scale clipping, RTL, rotation/process restoration, and low/mid/high-device motion/thermal checks remain explicit production-promotion work.

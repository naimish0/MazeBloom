# Accessibility, visual, animation, and audio

The visual system uses warm parchment, garden greens, terracotta/amber, raised Stones, outlined petal Buds, a leaf-topped Seed, and tactile paired-leaf Bloom. Shape, outline, and elevation supplement color. The original adaptive icon uses the same Seed/Bloom language.

The board remains square up to 560dp and switches to a side-by-side tablet layout above 700dp. Controls have 48dp+ targets, text uses resources, RTL is enabled, layouts scroll where necessary, and no flashing effects are used. Board semantics announce Seed coordinates, Buds, and moves; level/control/completion semantics expose lock, star, and direction state. Direction buttons provide a swipe alternative.

Transition presentation consumes `TransitionResult`: Seed travel occurs first, entered Buds disappear in order, then departed cells grow into Bloom. Input is briefly locked by the controller. Reduced motion makes the logical result immediate. Undo restores logic immediately.

High contrast is an explicit setting. Lightweight `ToneGenerator` cues and system haptics cover invalid moves, collection, Bloom/slide, and completion, respect toggles, and never affect rules. The feedback controller releases audio resources with composition teardown.

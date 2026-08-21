# Monetization, consent, and analytics

Gameplay is configuration-free and offline. `AdsGateway`, `BillingGateway`, `ConsentGateway`, and `Analytics` isolate optional services. No-op defaults, deterministic fakes, and policy tests are implemented. No vendor SDK, live/test ad inventory, production ID, billing product, or credential was added.

Interstitial policy allows only successful post-level “Back to garden” transitions after onboarding, never the first session, requires three eligible completions and 180 seconds, and respects Remove Ads. Undo, Restart, Hint, failure, Daily, Settings, Collection, share, resume, animation, and Next Level are ineligible. Reward callbacks are idempotent; unavailable services cannot block gameplay.

Typed analytics cover app open, onboarding, level/daily lifecycle, Undo/Restart/hints, replay/share, generator errors, ads, and purchases. Allowed parameters are stable IDs and coarse gameplay metadata. Names, contacts, media, precise location, raw boards/solutions, and unconsented advertising identifiers are prohibited.

To ship monetization, select approved compatible vendors, supply external IDs/product configuration, complete consent/privacy disclosures, implement and test the real adapters, acknowledge/restore the Play purchase, and validate through Play test accounts. Until then, purchase/ad UI stays disabled.

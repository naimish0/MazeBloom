# Play Data safety working sheet

This is a release-operator worksheet, not a substitute for reviewing the current Play Console form and each SDK provider's current disclosure. Complete the form for the exact artifact uploaded; do not combine the two build rows.

## Offline `release` build

- **Does the app collect or share required user-data types?** No developer or third-party SDK collection is implemented.
- **Account creation:** None.
- **Data encrypted in transit:** Not applicable; the app has no network permission or app-operated network transfer. User-initiated sharing is performed by Android's share sheet to the destination the user chooses.
- **Deletion request mechanism:** No cloud account or server data exists. Users delete all app data through Android Clear storage or uninstall.
- **Independent security review:** Declare only if one has actually been completed and qualifies under Play's current wording.

Local game progress, settings, generated puzzles, replay history, coin state, and idempotency identifiers stay in private app storage. Android cloud backup and device-transfer backup are disabled. Local-only processing is not declared as collection when it never leaves the device under Play's current definition, but the operator must recheck the definition at submission time.

## Ad-supported `production` build

In addition to the local behavior above, Google Mobile Ads SDK 25.4.0 states that it automatically collects and shares the following. Verify the current Google disclosure again immediately before submission.

| Play data type | Typical form purpose(s) | Handling |
| --- | --- | --- |
| Approximate location, inferred from IP address | Advertising or marketing; analytics; fraud prevention, security and compliance | Collected/shared by Google; encrypted in transit |
| App interactions | Advertising or marketing; analytics; fraud prevention, security and compliance | Collected/shared by Google; encrypted in transit |
| Crash logs / diagnostics / other app performance data, as mapped by the current form | Analytics; fraud prevention, security and compliance | Collected/shared by Google; encrypted in transit |
| Device or other IDs, including advertising ID and app set ID where available | Advertising or marketing; analytics; fraud prevention, security and compliance | Collected/shared by Google; encrypted in transit |

Also confirm in Play Console:

- the app **contains ads**;
- UMP privacy messages are published for every legally applicable region;
- a privacy-options entry point is configured and visible when required;
- the selected target audience excludes child age groups unless the implementation and all ad requests have first been made Families-compliant;
- the public policy URL and in-app link resolve to `docs/index.html` content;
- no mediation partner, Firebase product, optional AdMob experiment, billing SDK, analytics SDK, or crash SDK has been added without updating this worksheet and the privacy policy; and
- answers distinguish data collection from data sharing using Play's current service-provider and user-initiated-transfer definitions.

## Source references

- Google Play Data safety instructions: <https://support.google.com/googleplay/android-developer/answer/10787469>
- Google Play User Data policy: <https://support.google.com/googleplay/android-developer/answer/10144311>
- Google Mobile Ads SDK disclosure: <https://developers.google.com/admob/android/privacy/play-data-disclosure>
- Google UMP setup and consent flow: <https://developers.google.com/admob/android/privacy>

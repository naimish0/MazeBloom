# Level schema

Bundled definitions use canonical UTF-8 JSON objects with stable field order and no insignificant whitespace. Cells are ascending integer arrays; signed packed masks are never external data. Seeds are exactly 16 lowercase hexadecimal digits.

Schema v2 fields include `id`, independent schema/content/rules versions, dimensions, `stones`, `start`, `buds`, global `gardenId`, global `chapterId`, Chapter-local order, Campaign order, optional Progressive order, generator/solver/profile/fingerprint/uniqueness versions, 16-digit seed, provisional difficulty, optimal move count, canonical replay, saturated optimal count as a decimal string plus overflow flag, fingerprints, hashes, certification metrics, and a SHA-256 `certificationChecksum`. Progressive records use stable `progressive-NNNN` IDs, `campaignOrder = 0`, and the `progressive` Garden/Chapter identity. Only immutable schema-v1 Tutorials 1–5 retain their original payload and source checksums; Campaign 6–2,000 uses content version 2 and certification profile 3.

`content/campaign/campaign-manifest.json` contains only Garden/Chapter descriptors and lightweight summaries/direct IDs. Each of 100 Chapter files contains exactly 20 definitions. Per-shard SHA-256, content/audit shard roots, legacy component roots, campaign/global uniqueness roots, and release root are independent so a newer audit need not rewrite a definition.

Active Daily selections persist the full versioned definition so a selection survives a later pool update. Runtime attempts restore only when stable ID, content/rules versions, and exact definition checksum remain compatible. The Progressive pool has independent version/count/content/audit/uniqueness roots in the root manifest.

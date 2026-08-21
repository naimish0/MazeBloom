# Level schema

Bundled content uses canonical UTF-8 JSON Lines: one object per line, stable field order, no insignificant whitespace. Cells are ascending integer arrays; signed packed masks are never external data. Seeds are exactly 16 lowercase hexadecimal digits.

Required v1 fields include `id`, schema/content/rules versions, dimensions, `stones`, `start`, `buds`, chapter/order, generator/solver/profile/fingerprint versions, generator seed, provisional difficulty, optimal move count, canonical replay, certification metrics, and a 64-character SHA-256 `certificationChecksum`.

The per-record checksum hashes the canonical logical object before the checksum field is appended. The manifest separately pins SHA-256 over the exact campaign and daily JSONL byte streams. Unknown required enum values, non-square/unsupported boards, unsorted or duplicate cells, out-of-range cells, overlaps, invalid Bud count, bad seed encoding, or checksum drift are rejected.

Active daily selections persist the full versioned definition so a selection survives a later pool update. Runtime attempts are restored only when level ID, content version, and rules version remain compatible.

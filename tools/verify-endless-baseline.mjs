import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "..");
const content = resolve(root, "app/src/main/assets/content");
const manifest = JSON.parse(readFileSync(resolve(content, "manifest.json"), "utf8"));
const baseline = JSON.parse(readFileSync(resolve(content, "endless-baseline.json"), "utf8"));
const fields = [
  "endless-baseline-v1",
  manifest.campaignRoot,
  manifest.dailyContentRoot,
  manifest.dailyAuditRoot,
  manifest.progressiveContentRoot,
  manifest.progressiveAuditRoot,
  manifest.progressiveUniquenessRoot,
  manifest.globalUniquenessRoot,
  manifest.certificationProfileVersion,
  manifest.fingerprintVersion,
  manifest.uniquenessProfileVersion,
  "2220",
  String(manifest.globalPairComparisons),
  String(manifest.globalAlignmentComparisons),
  "zero-new-collisions",
];
const payload = fields.map((field) => {
  const value = String(field);
  return `${Buffer.byteLength(value, "utf8")}:${value}`;
}).join("");
const derived = createHash("sha256").update(payload, "utf8").digest("hex");

const fail = (message) => { throw new Error(`ENDLESS_BASELINE_MISMATCH: ${message}`); };
if (manifest.campaignCount !== 2000 || manifest.dailyCount !== 120 || manifest.progressiveCount !== 100) fail("constituent counts are not 2,000 + 120 + 100");
if (baseline.recordCount !== 2220) fail(`declared count ${baseline.recordCount}`);
if (manifest.globalPairComparisons !== 2463090) fail(`pair count ${manifest.globalPairComparisons}`);
if (baseline.endlessBaselineRoot !== derived) fail(`root ${baseline.endlessBaselineRoot}, derived ${derived}`);
if (baseline.expectedPairComparisons !== "2463090" || baseline.collisionResult !== "zero-new-collisions") fail("canonical collision evidence changed");

console.log(`Endless baseline PASS: ${baseline.recordCount} records, ${baseline.expectedPairComparisons} pairs, root ${derived}`);

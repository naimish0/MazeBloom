import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const content = resolve(root, "app/src/main/assets/content");
const stamp = JSON.parse(readFileSync(resolve(content, "endless-horizon-stamp.json"), "utf8"));
const baseline = JSON.parse(readFileSync(resolve(content, "endless-baseline.json"), "utf8"));
const sha256 = (value) => createHash("sha256").update(value).digest("hex");
const fail = (message) => { throw new Error(`ENDLESS_HORIZON_STAMP_INVALID: ${message}`); };
const requireEqual = (actual, expected, field) => {
  if (String(actual) !== String(expected)) fail(`${field}: expected ${expected}, found ${actual}`);
};
const requireHash = (value, field) => {
  if (!/^[0-9a-f]{64}$/.test(value ?? "")) fail(`${field} is not a lowercase SHA-256 value`);
};
const expectedSegments = [
  [1, 101, 15],
].map(([id, start, generator]) => ({ id, start, generator }));
const lengthPrefixed = (...fields) => fields.map((field) => {
  const value = String(field);
  return `${Buffer.byteLength(value, "utf8")}:${value}`;
}).join("");

requireEqual(stamp.status, "PASS", "status");
requireEqual(stamp.generatedCount, 1000, "generatedCount");
requireEqual(stamp.baselineCount, 2220, "baselineCount");
requireEqual(stamp.corpusCount, 3220, "corpusCount");
requireEqual(stamp.endlessBaselineRoot, baseline.endlessBaselineRoot, "endlessBaselineRoot");
requireEqual(stamp.versions?.namespace, 1, "versions.namespace");
requireEqual(stamp.versions?.certification, 3, "versions.certification");
requireEqual(stamp.versions?.fingerprint, 2, "versions.fingerprint");
requireEqual(stamp.versions?.uniqueness, 2, "versions.uniqueness");
requireEqual(stamp.versions?.segmentPolicy, "bounded-generator-upgrade-v1", "versions.segmentPolicy");
requireEqual(stamp.versions?.maximumGeneratorVersion, 40, "versions.maximumGeneratorVersion");
requireEqual(stamp.releaseKeyVersions?.rules, 1, "releaseKeyVersions.rules");
requireEqual(stamp.releaseKeyVersions?.solver, 2, "releaseKeyVersions.solver");
requireEqual(stamp.releaseKeyVersions?.schedule, 1, "releaseKeyVersions.schedule");
requireEqual(stamp.releaseKeyVersions?.candidateKey, 1, "releaseKeyVersions.candidateKey");
requireEqual(stamp.releaseKeyVersions?.seedNamespace, 1, "releaseKeyVersions.seedNamespace");
requireEqual(stamp.releaseKeyVersions?.history, 1, "releaseKeyVersions.history");
requireEqual(stamp.theoreticalNewPairObligations, "2719500", "theoreticalNewPairObligations");
requireEqual(stamp.independentFullCorpusPairChecks, "5182590", "independentFullCorpusPairChecks");
requireEqual(stamp.expectedFullCorpusPairChecks, "5182590", "expectedFullCorpusPairChecks");
requireEqual(stamp.newCollisions, 0, "newCollisions");
requireEqual(stamp.horizonBoardsBundled, false, "horizonBoardsBundled");
requireEqual(stamp.attempts, 290031, "attempts");
requireEqual(stamp.candidateAccounting?.accountedAttempts, 290031, "candidateAccounting.accountedAttempts");
requireEqual(stamp.candidateAccounting?.accepted, 1000, "candidateAccounting.accepted");
requireEqual(stamp.candidateAccounting?.segmentExhaustions, 0, "candidateAccounting.segmentExhaustions");
requireEqual(stamp.actualFullComparatorCalls, "5182590", "actualFullComparatorCalls");
requireHash(stamp.endlessBaselineRoot, "endlessBaselineRoot");
requireHash(stamp.horizonRoot, "horizonRoot");
requireHash(stamp.historyRoot, "historyRoot");
requireHash(stamp.toolingHash, "toolingHash");
requireEqual(stamp.horizonRoot, "1736d76e3b71ba85e02fecb57fda7bba3577204b9d06bdfcb5778933eaabd85c", "horizonRoot");
requireEqual(stamp.historyRoot, "fc40c7fa694d58fe7ba6b09e7a55070c359f6b5cb3890cb54b43974e25229cd3", "historyRoot");

if (JSON.stringify(stamp.segments) !== JSON.stringify(expectedSegments)) fail("generation segments do not match the certified horizon");
let previousStart = 100;
stamp.segments.forEach((segment, index) => {
  requireEqual(segment.id, index + 1, `segments[${index}].id`);
  requireEqual(segment.generator, 15 + index, `segments[${index}].generator`);
  if (!Number.isSafeInteger(segment.start) || segment.start < 101 || segment.start < previousStart || segment.start > 1100) {
    fail(`segments[${index}].start is outside the certified horizon or out of order`);
  }
  previousStart = segment.start;
  const digest = createHash("sha256").update(lengthPrefixed(
    "mazebloom-auto-seed", stamp.endlessBaselineRoot, stamp.versions.namespace, segment.generator,
    stamp.versions.certification, stamp.versions.fingerprint, stamp.versions.uniqueness, segment.id, segment.start,
  )).digest("hex").slice(0, 16);
  requireEqual(stamp.namespaceBases?.[segment.id], digest, `namespaceBases.${segment.id}`);
});
if (stamp.segments.at(-1).generator > stamp.versions.maximumGeneratorVersion) fail("segment generator exceeds the pinned maximum");
if (!Array.isArray(stamp.scheduleBlocks) || stamp.scheduleBlocks.length !== 10) fail("ten schedule blocks are required");
stamp.scheduleBlocks.forEach((block, index) => {
  requireEqual(block.block, index + 1, `scheduleBlocks[${index}].block`);
  requireEqual(block.count, 100, `scheduleBlocks[${index}].count`);
  [["EASY", 16], ["NORMAL", 28], ["HARD", 28], ["EXPERT", 20], ["MASTER", 8]].forEach(([band, count]) =>
    requireEqual(block.difficulty?.[band], count, `scheduleBlocks[${index}].difficulty.${band}`));
  requireEqual(block.boardSize?.[5], 26, `scheduleBlocks[${index}].boardSize.5`);
  requireEqual(block.boardSize?.[6], 74, `scheduleBlocks[${index}].boardSize.6`);
});

const currentToolingHash = sha256(
  readFileSync(resolve(root, "tools/generate-content.mjs"), "utf8") +
  readFileSync(resolve(root, "tools/certify-endless-horizon.mjs"), "utf8"),
);
requireEqual(stamp.toolingHash, currentToolingHash, "toolingHash");

console.log(JSON.stringify({
  status: "PASS",
  generatedCount: stamp.generatedCount,
  pairChecks: stamp.independentFullCorpusPairChecks,
  newCollisions: stamp.newCollisions,
  horizonRoot: stamp.horizonRoot,
  toolingHash: stamp.toolingHash,
}));

#!/usr/bin/env node
import { createHash } from "node:crypto";
import { readFileSync, statSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
  audit, compareSimilarity, difficultyRange, dynamicFingerprint, geometricEncoding, meetsProfile, prepareItem,
  replayStructure, solve,
} from "./generate-content.mjs";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const content = resolve(root, "app/src/main/assets/content");
const campaignDirectory = resolve(content, "campaign");
const sha256 = (text) => createHash("sha256").update(text, "utf8").digest("hex");
const hashFields = (...fields) => sha256(fields.map((field) => {
  const value = String(field);
  return `${Buffer.byteLength(value, "utf8")}:${value}`;
}).join(""));
const bit = (cell) => 1n << BigInt(cell);
const internal = (record) => ({
  size: record.width,
  walls: record.stones.reduce((mask, cell) => mask | bit(cell), 0n),
  start: record.start,
  buds: record.buds.reduce((mask, cell) => mask | bit(cell), 0n),
});
const DIRECTIONS = ["UP", "RIGHT", "DOWN", "LEFT"];
const PACING = [
  "EASY EASY NORMAL EASY NORMAL HARD NORMAL EASY NORMAL HARD EXPERT EASY NORMAL HARD NORMAL EASY NORMAL HARD EXPERT MASTER",
  "EASY NORMAL HARD NORMAL EASY NORMAL HARD EXPERT NORMAL HARD EASY NORMAL HARD EXPERT EASY NORMAL NORMAL HARD EXPERT MASTER",
  "EASY NORMAL HARD NORMAL HARD EXPERT NORMAL HARD EASY NORMAL HARD EXPERT HARD NORMAL HARD EXPERT NORMAL HARD EXPERT MASTER",
  "EASY EASY NORMAL HARD EXPERT MASTER EASY NORMAL HARD EXPERT HARD NORMAL HARD EXPERT HARD NORMAL HARD EXPERT EXPERT MASTER",
  "EASY NORMAL HARD EXPERT MASTER NORMAL HARD EXPERT HARD NORMAL HARD EXPERT MASTER NORMAL HARD EXPERT HARD EXPERT EXPERT MASTER",
].map((value) => value.split(" "));
const FIVE_BY_FIVE = { EASY: 12, NORMAL: 14, HARD: 0, EXPERT: 0, MASTER: 0 };
const TOTAL_BY_BAND = { EASY: 16, NORMAL: 28, HARD: 28, EXPERT: 20, MASTER: 8 };

function expectedBand(order) {
  if (order <= 5) return "TUTORIAL";
  if (order <= 20) return "EASY";
  if (order <= 40) return "NORMAL";
  if (order <= 70) return "HARD";
  if (order <= 95) return "EXPERT";
  if (order <= 100) return "MASTER";
  const local = (order - 1) % 100;
  return PACING[Math.floor(local / 20)][local % 20];
}

function definitionHash(record) {
  const gardenId = record.gardenId ?? (record.campaignOrder ? `garden-${String(Math.floor((record.campaignOrder - 1) / 100) + 1).padStart(2, "0")}` : "daily");
  const chapterId = record.chapterId ?? (record.campaignOrder ? `${gardenId}/chapter-${String(record.chapter).padStart(2, "0")}` : "daily");
  return hashFields(
    "definition-v2", record.schemaVersion, record.contentVersion, record.id, record.campaignOrder,
    gardenId, chapterId, record.chapterOrderWithinGarden ?? record.chapter, record.width, record.height,
    record.stones.join(","), record.start, record.buds.join(","), record.generatorVersion, record.generatorSeed,
  );
}

function validateRecord(record, type) {
  const checksum = record.certificationChecksum;
  if (!/^[0-9a-f]{64}$/.test(checksum)) throw new Error(`${record.id}: invalid certification checksum`);
  const logical = { ...record };
  delete logical.certificationChecksum;
  if (sha256(JSON.stringify(logical)) !== checksum) throw new Error(`${record.id}: source checksum mismatch`);
  if (record.width !== record.height || ![5, 6].includes(record.width)) throw new Error(`${record.id}: invalid dimensions`);
  if (!Number.isInteger(record.start) || record.start < 0 || record.start >= record.width ** 2) throw new Error(`${record.id}: invalid start`);
  for (const field of ["stones", "buds"]) {
    const values = record[field];
    if (!Array.isArray(values) || values.some((value, index) => !Number.isInteger(value) || value < 0 || value >= record.width ** 2 || (index > 0 && value <= values[index - 1]))) {
      throw new Error(`${record.id}: ${field} is not canonical`);
    }
  }
  if (record.buds.length < 1 || record.buds.length > 7 || record.stones.includes(record.start) || record.buds.includes(record.start) || record.buds.some((cell) => record.stones.includes(cell))) {
    throw new Error(`${record.id}: invalid board occupancy`);
  }
  const range = difficultyRange(record.difficulty, record.certificationProfileVersion);
  if (!/^[0-9a-f]{16}$/.test(record.generatorSeed) || !range || record.optimalMoves < range[0] || record.optimalMoves > range[1]) {
    throw new Error(`${record.id}: invalid versioned certification fields`);
  }
  if (!Array.isArray(record.canonicalReplay) || record.canonicalReplay.length !== record.optimalMoves || record.canonicalReplay.some((direction) => !DIRECTIONS.includes(direction))) {
    throw new Error(`${record.id}: invalid canonical replay`);
  }
  if (type === "campaign" && record.campaignOrder < 1) throw new Error(`${record.id}: missing campaign order`);
  if (record.schemaVersion >= 2 && record.definitionHash !== definitionHash(record)) throw new Error(`${record.id}: definition hash drift`);
}

function expectedRepeatingSize(order) {
  const local = (order - 1) % 100;
  const band = PACING[Math.floor(local / 20)][local % 20];
  let rank = 0;
  for (let index = 0; index < local; index += 1) if (PACING[Math.floor(index / 20)][index % 20] === band) rank += 1;
  const ceilDiv = (a, b) => Math.floor((a + b - 1) / b);
  return ceilDiv((rank + 1) * FIVE_BY_FIVE[band], TOTAL_BY_BAND[band]) > ceilDiv(rank * FIVE_BY_FIVE[band], TOTAL_BY_BAND[band]) ? 5 : 6;
}

function expectedSize(order) {
  if (order <= 70) return 5;
  if (order <= 100) return 6;
  return expectedRepeatingSize(order);
}

function profilePass(record, metrics) {
  return meetsProfile(record.difficulty, metrics, record.campaignOrder, record.certificationProfileVersion);
}

function allBudsOnOneLine(record) {
  return new Set(record.buds.map((cell) => cell % record.width)).size === 1 ||
    new Set(record.buds.map((cell) => Math.floor(cell / record.width))).size === 1;
}

const manifest = JSON.parse(readFileSync(resolve(content, "manifest.json"), "utf8"));
const catalogText = readFileSync(resolve(campaignDirectory, "campaign-manifest.json"), "utf8");
const catalog = JSON.parse(catalogText);
if (manifest.campaignCount !== 2000 || catalog.campaignCount !== 2000 || catalog.gardens.length !== 20) throw new Error("Campaign count/hierarchy drift");
if (manifest.campaignCatalogVersion !== 3 || catalog.campaignCatalogVersion !== 3) throw new Error("Campaign catalog version drift");
if (catalog.gardens.some((garden) => garden.chapters.length !== 5) || catalog.gardens.flatMap((garden) => garden.chapters).length !== 100) throw new Error("Garden/Chapter hierarchy drift");
if (Buffer.byteLength(catalogText) > 2 * 1024 * 1024) throw new Error("Campaign catalog exceeds 2 MiB");

const legacyText = readFileSync(resolve(content, "campaign.jsonl"), "utf8");
const historicalLines = legacyText.split("\n").filter(Boolean);
if (historicalLines.length !== 100) throw new Error(`Expected 100 historical source records; found ${historicalLines.length}`);
const legacyLines = historicalLines.slice(0, 5);
const legacyRecords = legacyLines.map(JSON.parse);
legacyRecords.forEach((record) => validateRecord(record, "campaign"));
const legacyDefinitionRoot = hashFields(...legacyRecords.map(definitionHash));
const legacySolutionRoot = hashFields(...legacyRecords.map((record) => hashFields(definitionHash(record), record.rulesVersion, record.optimalMoves, record.canonicalReplay.join(","))));
const legacyCertificateRoot = hashFields(...legacyRecords.map((record) => record.certificationChecksum));
const legacyCompatibilityRoot = hashFields(legacyDefinitionRoot, legacySolutionRoot, legacyCertificateRoot, ...legacyRecords.map((record) => `${record.id}|${record.campaignOrder}|${record.difficulty}|${record.generatorVersion}|${record.generatorSeed}`));
if (legacyDefinitionRoot !== manifest.legacyDefinitionRoot || legacySolutionRoot !== manifest.legacySolutionRoot || legacyCertificateRoot !== manifest.legacyCertificateRoot || legacyCompatibilityRoot !== manifest.legacyPrefixCompatibilityRoot) {
  throw new Error("Legacy prefix compatibility root drift");
}

const chapters = catalog.gardens.flatMap((garden) => garden.chapters);
const campaign = [];
const shardRoots = [];
let totalShardBytes = 0;
for (const chapter of chapters) {
  const assetRelative = chapter.shardPath.replace(/^content\//, "");
  const shardPath = resolve(content, assetRelative);
  const shardText = readFileSync(shardPath, "utf8");
  totalShardBytes += statSync(shardPath).size;
  if (statSync(shardPath).size > 2 * 1024 * 1024) throw new Error(`${chapter.id}: shard exceeds 2 MiB`);
  if (sha256(shardText) !== chapter.shardSha256) throw new Error(`${chapter.id}: shard SHA-256 drift`);
  shardRoots.push(chapter.shardSha256);
  const shard = JSON.parse(shardText);
  if (shard.campaignCatalogVersion !== 3 || shard.levels.length !== 20 || shard.chapterId !== chapter.id) throw new Error(`${chapter.id}: invalid shard metadata`);
  shard.levels.forEach((record, index) => {
    validateRecord(record, "campaign");
    const order = chapter.firstCampaignOrder + index;
    if (record.campaignOrder !== order || chapter.levels[index].id !== record.id || chapter.levels[index].campaignOrder !== order) throw new Error(`${chapter.id}: summary/definition drift`);
    campaign.push(record);
  });
}
if (campaign.length !== 2000 || campaign.map((record) => record.campaignOrder).join(",") !== Array.from({ length: 2000 }, (_, index) => index + 1).join(",")) throw new Error("Campaign order gap or overlap");
if (new Set(campaign.map((record) => record.id)).size !== 2000) throw new Error("Duplicate campaign ID");
if (!campaign.slice(0, 5).every((record) => record.difficulty === "TUTORIAL") || campaign.slice(5).some((record) => record.difficulty === "TUTORIAL")) throw new Error("Campaign levels 1-5 must be the complete Tutorial band");
for (let index = 0; index < 5; index += 1) if (JSON.stringify(campaign[index]) !== legacyLines[index]) throw new Error(`Tutorial shard byte payload drift: ${campaign[index].id}`);

for (const record of campaign.slice(5)) {
  const local = (record.campaignOrder - 1) % 100;
  if (record.difficulty !== expectedBand(record.campaignOrder) || record.width !== expectedSize(record.campaignOrder)) throw new Error(`${record.id}: campaign pacing/size schedule mismatch`);
  const gardenOrder = Math.floor((record.campaignOrder - 1) / 100) + 1;
  const chapterOrder = Math.floor(local / 20) + 1;
  const gardenId = `garden-${String(gardenOrder).padStart(2, "0")}`;
  if (record.gardenId !== gardenId || record.chapterId !== `${gardenId}/chapter-${String(chapterOrder).padStart(2, "0")}` || record.chapterOrderWithinGarden !== chapterOrder) throw new Error(`${record.id}: hierarchy identity mismatch`);
}
const extension = campaign.slice(100);
const count = (values, predicate) => values.filter(predicate).length;
const expectedBands = { EASY: 304, NORMAL: 532, HARD: 532, EXPERT: 380, MASTER: 152 };
for (const [band, expected] of Object.entries(expectedBands)) if (count(extension, (record) => record.difficulty === band) !== expected) throw new Error(`${band}: extension quota drift`);
if (count(extension, (record) => record.width === 5) !== 494 || count(extension, (record) => record.width === 6) !== 1406) throw new Error("Extension board-size quota drift");

const pacingHash = hashFields(...PACING.flat());
const campaignContentRoot = hashFields("campaign-content-v2", legacyCompatibilityRoot, ...shardRoots, pacingHash);
const campaignAuditRoot = hashFields("campaign-audit-v2", ...chapters.map((chapter) => chapter.auditShardRoot), manifest.campaignUniquenessRoot);
const campaignRoot = hashFields(campaignContentRoot, campaignAuditRoot);
if (campaignContentRoot !== manifest.campaignContentRoot || campaignAuditRoot !== manifest.campaignAuditRoot || campaignRoot !== manifest.campaignRoot) throw new Error("Campaign aggregate root drift");
if (manifest.campaignPairComparisons !== 1_999_000 || catalog.campaignPairComparisons !== 1_999_000) throw new Error("Campaign exhaustive-pair count drift");

const dailyText = readFileSync(resolve(content, "daily.jsonl"), "utf8");
const dailyLines = dailyText.split("\n").filter(Boolean);
const daily = dailyLines.map(JSON.parse);
if (daily.length < 120 || daily.length !== manifest.dailyCount) throw new Error("Daily pool count drift");
daily.forEach((record) => validateRecord(record, "daily"));
if (new Set([...campaign, ...daily].map((record) => record.id)).size !== campaign.length + daily.length) throw new Error("Cross-pool duplicate ID");
const dailyContentRoot = hashFields(...dailyLines);
const dailyAuditRoot = hashFields(...daily.map((record) => `${record.dynamicFingerprint}|${record.solutionGrammarFingerprint}|${record.structuralFingerprint}`));
if (dailyContentRoot !== manifest.dailyContentRoot || dailyAuditRoot !== manifest.dailyAuditRoot) throw new Error("Daily root drift");
const progressiveText = readFileSync(resolve(content, "progressive.jsonl"), "utf8");
const progressiveLines = progressiveText.split("\n").filter(Boolean);
const progressive = progressiveLines.map(JSON.parse);
if (progressive.length !== 100 || progressive.length !== manifest.progressiveCount || manifest.progressivePoolVersion !== 1) throw new Error("Progressive pool count/version drift");
progressive.forEach((record, index) => {
  validateRecord(record, "progressive");
  if (record.id !== `progressive-${String(index + 1).padStart(4, "0")}` || record.progressiveOrder !== index + 1 || record.campaignOrder !== 0 || record.gardenId !== "progressive" || record.chapterId !== "progressive") throw new Error(`${record.id}: progressive identity drift`);
  if (record.difficulty !== PACING[Math.floor(index / 20)][index % 20] || record.width !== expectedRepeatingSize(index + 1)) throw new Error(`${record.id}: progressive pacing/size drift`);
});
if (new Set([...campaign, ...daily, ...progressive].map((record) => record.id)).size !== campaign.length + daily.length + progressive.length) throw new Error("Global duplicate ID");
const progressiveContentRoot = hashFields(...progressiveLines);
const progressiveAuditRoot = hashFields(...progressive.map((record) => `${record.dynamicFingerprint}|${record.solutionGrammarFingerprint}|${record.structuralFingerprint}`));
if (progressiveContentRoot !== manifest.progressiveContentRoot || progressiveAuditRoot !== manifest.progressiveAuditRoot) throw new Error("Progressive root drift");
const progressiveAlignmentComparisons = progressive.reduce((total, record, left) => total + progressive.slice(left + 1).filter((other) => other.width === record.width).length * 8, 0);
const progressiveUniquenessRoot = hashFields(
  "progressive-uniqueness-v1", manifest.progressivePoolVersion, 212_000, 4_950, progressiveAlignmentComparisons,
  ...progressive.map((record) => `${record.id}|${geometricEncoding(internal(record))}|${record.dynamicFingerprint}|${record.solutionGrammarFingerprint}|${record.structuralFingerprint}`),
);
if (progressiveUniquenessRoot !== manifest.progressiveUniquenessRoot) throw new Error("Progressive uniqueness root drift");
if (manifest.globalPairComparisons !== 2_463_090) throw new Error("Global exhaustive-pair count drift");
const releaseContentRoot = hashFields(
  manifest.campaignRoot, manifest.dailyContentRoot, manifest.dailyAuditRoot,
  manifest.progressiveContentRoot, manifest.progressiveAuditRoot,
  manifest.progressiveUniquenessRoot, manifest.globalUniquenessRoot,
);
if (releaseContentRoot !== manifest.releaseContentRoot) throw new Error("Release content root drift");
const stamp = JSON.parse(readFileSync(resolve(content, "certification-stamp.json"), "utf8"));
if (stamp.status !== "PASS" || stamp.releaseContentRoot !== releaseContentRoot || stamp.rulesVersion !== manifest.rulesVersion || stamp.solverVersion !== manifest.solverVersion || stamp.certificationProfileVersion !== manifest.certificationProfileVersion || stamp.fingerprintVersion !== manifest.fingerprintVersion || stamp.uniquenessProfileVersion !== manifest.uniquenessProfileVersion) {
  throw new Error("Full-certification stamp is missing or stale");
}
if (totalShardBytes > 64 * 1024 * 1024) throw new Error("Campaign shards exceed 64 MiB uncompressed");

if (process.argv.includes("--full")) {
  const all = [...campaign, ...daily, ...progressive];
  const items = [];
  const started = process.hrtime.bigint();
  let maximumShortestExpanded = 0;
  let maximumShortestDiscovered = 0;
  let maximumAuditReachable = 0;
  for (const record of all) {
    const level = internal(record);
    const solution = solve(level, 250_000);
    if (!solution || solution.depth !== record.optimalMoves || solution.replay.map((direction) => DIRECTIONS[direction]).join(",") !== record.canonicalReplay.join(",")) throw new Error(`${record.id}: full solve/replay certification failed`);
    if (record.optimalSolutionCount !== undefined && (String(solution.optimalCount) !== record.optimalSolutionCount || Boolean(solution.optimalOverflow) !== record.optimalCountOverflow)) throw new Error(`${record.id}: optimal-path count drift`);
    const metrics = audit(level, solution);
    if (!metrics) throw new Error(`${record.id}: audit budget exceeded`);
    const canonicalMetrics = {
      reachableStates: metrics.reachable,
      doomedStateCount: metrics.doomed,
      meaningfulDecisionCount: metrics.meaningful,
      earliestMeaningfulBranch: metrics.earliestMeaningful,
      forcedMoveRatio: Number(metrics.forcedRatio.toFixed(6)),
      minimumBloomAssistedStops: metrics.minimumBloomStops,
      canonicalReplayMaxCreatorUseDistance: metrics.maximumDependency,
    };
    if (JSON.stringify(canonicalMetrics) !== JSON.stringify(record.metrics)) throw new Error(`${record.id}: certification metric drift`);
    if (!profilePass(record, metrics)) throw new Error(`${record.id}: difficulty profile drift`);
    if ((record.schemaVersion >= 2 || record.campaignOrder === 0) && metrics.minimumBloomStops < 1) throw new Error(`${record.id}: missing required Bloom-assisted stop`);
    if ((record.schemaVersion >= 2 || record.campaignOrder === 0) && allBudsOnOneLine(record)) throw new Error(`${record.id}: one-line Bud layout`);
    if (record.schemaVersion >= 2 || record.campaignOrder === 0) {
      for (let cell = 0; cell < level.size ** 2; cell += 1) if (!(level.walls & bit(cell)) && !metrics.traversedCells.has(cell)) throw new Error(`${record.id}: irrelevant open cell ${cell}`);
    }
    const structure = replayStructure(level, solution);
    const dynamic = dynamicFingerprint(level);
    if (record.schemaVersion >= 2 && sha256(geometricEncoding(level)) !== record.geometricFingerprint) throw new Error(`${record.id}: geometric fingerprint drift`);
    if (dynamic !== record.dynamicFingerprint) throw new Error(`${record.id}: dynamic fingerprint drift`);
    if (record.schemaVersion >= 2 && (structure.structuralFingerprint !== record.structuralFingerprint || structure.grammarFingerprint !== record.solutionGrammarFingerprint)) throw new Error(`${record.id}: replay fingerprint drift`);
    maximumShortestExpanded = Math.max(maximumShortestExpanded, solution.expanded);
    maximumShortestDiscovered = Math.max(maximumShortestDiscovered, solution.discovered);
    maximumAuditReachable = Math.max(maximumAuditReachable, metrics.reachable);
    items.push(prepareItem({ id: record.id, campaignOrder: record.campaignOrder, level, solution, metrics, dynamic, structure }));
  }
  let pairs = 0;
  let alignments = 0;
  for (let left = 0; left < items.length; left += 1) {
    for (let right = left + 1; right < items.length; right += 1) {
      pairs += 1;
      if (items[left].level.size === items[right].level.size) alignments += 8;
      const collision = compareSimilarity(items[left], items[right]);
      const grandfathered = items[left].campaignOrder > 0 && items[left].campaignOrder <= 5 &&
        items[right].campaignOrder > 0 && items[right].campaignOrder <= 5;
      if (collision.tier !== "NONE" && !grandfathered) throw new Error(`Full uniqueness collision: ${items[left].id}/${items[right].id}`);
    }
  }
  const durationMs = Number(process.hrtime.bigint() - started) / 1_000_000;
  console.log(`Full certification solved ${all.length} levels and compared ${pairs} pairs (${alignments} D4 alignments) in ${durationMs.toFixed(1)}ms.`);
  console.log(`Authoritative maxima: shortest expanded=${maximumShortestExpanded}, shortest discovered=${maximumShortestDiscovered}, audit expanded/reachable=${maximumAuditReachable}.`);
}

console.log(`Verified ${campaign.length} campaign levels in 20 Gardens/100 Chapters, ${daily.length} Daily levels, and ${progressive.length} Auto Progressive levels.`);
console.log(`Campaign root ${manifest.campaignRoot}`);
console.log(`Release content root ${manifest.releaseContentRoot}`);

import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import {
  candidateFor, campaignBand, campaignSize, certifyRawCandidate, compareSimilarity, difficultyRange,
  hashFields, legacyItem, prepareItem, rawCandidateFor, register, replayStructure, solve, uniquenessRejection,
} from "./generate-content.mjs";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const content = resolve(root, "app/src/main/assets/content");
const reports = resolve(root, "reports/content");
const build = resolve(root, "build/endless-horizon");
const baselineManifest = JSON.parse(readFileSync(resolve(content, "endless-baseline.json"), "utf8"));
const target = Number(process.argv.find((arg) => arg.startsWith("--count="))?.split("=")[1] ?? "1000");
const probeNamespace = process.argv.includes("--probe-namespace");
if (!Number.isInteger(target) || target < 1 || target > 1000) throw new Error("--count must be 1..1000");
const runStartedAt = process.hrtime.bigint();
let sampledPeakHeapBytes = 0;
const sampleHeap = () => { sampledPeakHeapBytes = Math.max(sampledPeakHeapBytes, process.memoryUsage().heapUsed); };
const elapsedMillis = (startedAt) => Number(process.hrtime.bigint() - startedAt) / 1_000_000;

const MASK_64 = (1n << 64n) - 1n;
const GAMMA = 0x9e3779b97f4a7c15n;
const initialGeneratorVersion = Number(process.argv.find((arg) => arg.startsWith("--generator="))?.split("=")[1] ?? "15");
let segments = [{ id: 1, start: 101, generator: initialGeneratorVersion }];
const versions = {
  namespace: 1, certification: 3, fingerprint: 2, uniqueness: 2,
  segmentPolicy: "bounded-generator-upgrade-v1", maximumGeneratorVersion: 40,
};
const releaseKeyVersions = {
  rules: 1, solver: 2, schedule: 1, candidateKey: 1, seedNamespace: 1, history: 1,
};
const lengthPrefixed = (...fields) => fields.map((field) => {
  const value = String(field);
  return `${Buffer.byteLength(value, "utf8")}:${value}`;
}).join("");
const sha256 = (value) => createHash("sha256").update(value, "utf8").digest("hex");
function segmentFor(ordinal) { return [...segments].reverse().find((segment) => ordinal >= segment.start); }
function namespaceBaseFor(segment) {
  const digest = createHash("sha256").update(lengthPrefixed(
    "mazebloom-auto-seed", baselineManifest.endlessBaselineRoot, versions.namespace, segment.generator,
    versions.certification, versions.fingerprint, versions.uniqueness, segment.id, segment.start,
  )).digest();
  let value = 0n;
  for (let index = 0; index < 8; index += 1) value = (value << 8n) | BigInt(digest[index]);
  return value;
}
const seedFor = (segment, ordinal, attempt) => (namespaceBaseFor(segment) + ((((BigInt(ordinal) - 101n) << 16n) | BigInt(attempt)) * GAMMA)) & MASK_64;

function loadBaseline() {
  const catalog = JSON.parse(readFileSync(resolve(content, "campaign/campaign-manifest.json"), "utf8"));
  const campaign = catalog.gardens.flatMap((garden) => garden.chapters).flatMap((chapter) => {
    const shard = JSON.parse(readFileSync(resolve(root, "app/src/main/assets", chapter.shardPath), "utf8"));
    return shard.levels;
  });
  const dailyLines = readFileSync(resolve(content, "daily.jsonl"), "utf8").trim().split("\n");
  const progressiveLines = readFileSync(resolve(content, "progressive.jsonl"), "utf8").trim().split("\n");
  const records = [
    ...campaign.map((record) => [record, JSON.stringify(record)]),
    ...dailyLines.map((line) => [JSON.parse(line), line]),
    ...progressiveLines.map((line) => [JSON.parse(line), line]),
  ];
  if (records.length !== 2220) throw new Error(`BASELINE_MISMATCH: ${records.length}`);
  return records.map(([record, line]) => legacyItem(record, line));
}

function emptyRejections() {
  return {
    construction: 0, unsolvableOrRequiredBudget: 0, trivialOrOneLine: 0, profile: 0,
    irrelevantOpenCell: 0, exactDefinitionOrGeometric: 0, dynamic: 0, structural: 0,
    solutionGrammar: 0, hardNear: 0, reviewSimilarity: 0, metricPrefilter: 0, shortestProbe: 0,
  };
}

const popcount = (value) => {
  let count = 0;
  while (value) { value &= value - 1n; count += 1; }
  return count;
};
function outsideMetricRadius(candidate, existingItems) {
  for (const existing of existingItems) {
    if (existing.level.size !== candidate.level.size) continue;
    for (const geometry of existing.geometry) {
      const distance = popcount(candidate.level.walls ^ geometry.walls) +
        2 * popcount(candidate.level.buds ^ geometry.buds) +
        (candidate.level.start === geometry.start ? 0 : 2);
      if (distance <= 12) return false;
    }
  }
  return true;
}

function minimumMetricDistance(candidate, existingItems) {
  let minimum = Number.MAX_SAFE_INTEGER;
  for (const existing of existingItems) {
    if (existing.level.size !== candidate.level.size) continue;
    for (const geometry of existing.geometry) {
      const distance = popcount(candidate.level.walls ^ geometry.walls) +
        2 * popcount(candidate.level.buds ^ geometry.buds) +
        (candidate.level.start === geometry.start ? 0 : 2);
      minimum = Math.min(minimum, distance);
    }
  }
  return minimum;
}

function runtimeRawCandidateFor(seed, size, band, existingItems, geometric, structural, grammar, generatorVersion) {
  const variantGamma = (0xd1b54a32d192ed03n + BigInt(generatorVersion) * GAMMA) & MASK_64;
  const candidates = [];
  for (let variant = 0; variant < 3; variant += 1) {
    const subSeed = (seed + BigInt(variant) * variantGamma) & MASK_64;
    const result = rawCandidateFor(subSeed, size, band, 0);
    if (!result.rejection) {
      const replay = result.level._constructionReplay;
      const constructionSolution = { depth: replay.length, replay, discovered: 0 };
      const behaviorNovel = probeUniquenessRejection(result.level, constructionSolution, existingItems, geometric, structural, grammar) === null;
      candidates.push({ ...result, variant, behaviorNovel, distance: minimumMetricDistance(result, existingItems) });
    }
  }
  if (!candidates.length) return { rejection: "construction" };
  candidates.sort((left, right) => Number(right.behaviorNovel) - Number(left.behaviorNovel) || right.distance - left.distance || left.variant - right.variant);
  return candidates[0];
}

function probeUniquenessRejection(level, solution, existingItems, geometric, structural, grammar) {
  const item = prepareItem({ level, solution, structure: replayStructure(level, solution), dynamic: "" });
  if (geometric.has(item.geometric)) return "exactDefinitionOrGeometric";
  if (structural.has(item.structure.structuralFingerprint)) return "structural";
  if (grammar.has(item.structure.grammarFingerprint)) return "solutionGrammar";
  for (const existing of existingItems) {
    const comparison = compareSimilarity(existing, item);
    if (comparison.tier === "HARD") return "hardNear";
    if (comparison.tier === "REVIEW") return "reviewSimilarity";
  }
  return null;
}

function candidateKey(segment, ordinal, attempt, band, size) {
  return [
    "mazebloom-auto-progressive", baselineManifest.endlessBaselineRoot, versions.namespace, segment.generator,
    versions.certification, versions.fingerprint, versions.uniqueness, segment.id,
    ordinal, attempt, band, size,
  ].join("|");
}

function acceptedRecord(item) {
  return lengthPrefixed(
    "horizon-accepted-v1", item.id, item.ordinal, item.candidateKey, item.seed.toString(16).padStart(16, "0"),
    item.level.size, item.level.walls.toString(16), item.level.start, item.level.buds.toString(16),
    item.band, item.solution.depth, item.solution.replay.join(","), item.dynamic,
    item.structure.structuralFingerprint, item.structure.grammarFingerprint, item.geometric,
    versions.namespace, item.generatorVersion, versions.certification, versions.fingerprint, versions.uniqueness,
  );
}

mkdirSync(build, { recursive: true });
mkdirSync(reports, { recursive: true });
const checkpointPath = resolve(build, `checkpoint-${target}.json`);
const baseline = loadBaseline();
sampleHeap();
const accepted = [];
const registry = [...baseline];
const geometricSet = new Set(registry.map((item) => item.geometric));
const dynamicSet = new Set(registry.map((item) => item.dynamic));
const structuralSet = new Set(registry.map((item) => item.structure.structuralFingerprint));
const grammarSet = new Set(registry.map((item) => item.structure.grammarFingerprint));
const autoSeeds = new Set(registry.filter((item) => item.id?.startsWith("progressive-")).map((item) => item.seed.toString(16).padStart(16, "0")));
const rejections = emptyRejections();
let attempts = 0;
let historyRoot = sha256(`mazebloom-auto-history-v1|${baselineManifest.endlessBaselineRoot}`);
let resume = null;
const reproductionStartedAt = process.hrtime.bigint();
const reproductionLevelMillis = [];
if (existsSync(checkpointPath)) {
  resume = JSON.parse(readFileSync(checkpointPath, "utf8"));
  if (resume.baselineRoot !== baselineManifest.endlessBaselineRoot || resume.versionsKey !== JSON.stringify(versions)) resume = null;
}
if (resume) {
  segments = resume.segments;
  for (const saved of resume.accepted) {
    const levelStartedAt = process.hrtime.bigint();
    const raw = runtimeRawCandidateFor(
      BigInt(`0x${saved.seedHex}`), saved.size, saved.band, registry, geometricSet, structuralSet, grammarSet,
      saved.generatorVersion ?? initialGeneratorVersion,
    );
    const result = raw.rejection ? raw : certifyRawCandidate(BigInt(`0x${saved.seedHex}`), saved.band, raw.level);
    if (!result.item) throw new Error(`checkpoint reproduction failed at ${saved.ordinal}`);
    Object.assign(result.item, saved, { seed: BigInt(`0x${saved.seedHex}`) });
    result.item.generationSegment ??= 1;
    result.item.generatorVersion ??= initialGeneratorVersion;
    register(result.item, registry, geometricSet, dynamicSet, structuralSet, grammarSet);
    autoSeeds.add(saved.seedHex);
    accepted.push(result.item);
    reproductionLevelMillis.push(elapsedMillis(levelStartedAt));
    if (accepted.length % 25 === 0) sampleHeap();
  }
  Object.assign(rejections, resume.rejections);
  attempts = resume.attempts;
  historyRoot = resume.historyRoot;
}
const checkpointReproductionMillis = elapsedMillis(reproductionStartedAt);
const generationStartedAt = process.hrtime.bigint();
const resumedAcceptedCount = accepted.length;

for (let offset = accepted.length; offset < target; offset += 1) {
  const ordinal = 101 + offset;
  const band = campaignBand(ordinal);
  const size = campaignSize(ordinal);
  let selected = null;
  while (!selected) {
    const segment = segments.at(-1);
    let cheapSurvivors = 0;
    let fullSurvivors = 0;
    for (let windowStart = 0; windowStart <= 65_535 && !selected && cheapSurvivors <= 1024 && fullSurvivors <= 128; windowStart += 256) {
    for (let attempt = windowStart; attempt < windowStart + 256; attempt += 1) {
      attempts += 1;
      const seed = seedFor(segment, ordinal, attempt);
      const seedHex = seed.toString(16).padStart(16, "0");
      if (autoSeeds.has(seedHex)) { rejections.construction += 1; continue; }
      const raw = runtimeRawCandidateFor(seed, size, band, registry, geometricSet, structuralSet, grammarSet, segment.generator);
      if (raw.rejection) {
        rejections[raw.rejection] = (rejections[raw.rejection] ?? 0) + 1;
        continue;
      }
      if (!outsideMetricRadius(raw, registry)) {
        rejections.metricPrefilter += 1;
        continue;
      }
      cheapSurvivors += 1;
      if (cheapSurvivors > 1024) break;
      const probe = solve(raw.level, 50_000);
      const [minimum, maximum] = difficultyRange(band);
      if (!probe || probe.depth < minimum || probe.depth > maximum) {
        rejections.shortestProbe += 1;
        continue;
      }
      const probeRejection = probeUniquenessRejection(raw.level, probe, registry, geometricSet, structuralSet, grammarSet);
      if (probeRejection) {
        rejections[probeRejection] += 1;
        continue;
      }
      const result = certifyRawCandidate(seed, band, raw.level);
      if (result.rejection) {
        rejections[result.rejection] = (rejections[result.rejection] ?? 0) + 1;
        continue;
      }
      fullSurvivors += 1;
      if (fullSurvivors > 128) break;
      const rejection = uniquenessRejection(result.item, registry, geometricSet, dynamicSet, structuralSet, grammarSet);
      if (rejection) {
        rejections[rejection] += 1;
        continue;
      }
      selected = Object.assign(result.item, {
        id: `auto-v${segment.id}-${String(ordinal).padStart(15, "0")}`,
        ordinal,
        attempt,
        seed,
        seedHex: seed.toString(16).padStart(16, "0"),
        band,
        size,
        candidateKey: candidateKey(segment, ordinal, attempt, band, size),
        generationSegment: segment.id,
        generatorVersion: segment.generator,
      });
      break;
    }
    }
    if (!selected) {
      if (segment.generator >= versions.maximumGeneratorVersion) {
        throw new Error(`GENERATION_EXHAUSTED at ordinal ${ordinal}: ${JSON.stringify({ cheapSurvivors, fullSurvivors, rejections })}`);
      }
      const next = { id: segment.id + 1, start: ordinal, generator: segment.generator + 1 };
      segments.push(next);
      console.log(`segment ${segment.id} exhausted at ${ordinal}; advancing to generator ${next.generator}`);
    }
  }
  register(selected, registry, geometricSet, dynamicSet, structuralSet, grammarSet);
  autoSeeds.add(selected.seedHex);
  accepted.push(selected);
  sampleHeap();
  historyRoot = sha256(historyRoot + acceptedRecord(selected));
  writeFileSync(checkpointPath, `${JSON.stringify({
    baselineRoot: baselineManifest.endlessBaselineRoot,
    versionsKey: JSON.stringify(versions),
    segments,
    attempts,
    rejections,
    historyRoot,
    accepted: accepted.map((item) => ({
      id: item.id, ordinal: item.ordinal, attempt: item.attempt, seedHex: item.seedHex,
      candidateKey: item.candidateKey, band: item.band, size: item.size,
      generationSegment: item.generationSegment, generatorVersion: item.generatorVersion,
    })),
  }, null, 2)}\n`);
  if (accepted.length % 10 === 0 || accepted.length === target) console.log(`accepted ${accepted.length}/${target}; ordinal ${ordinal}; attempt ${selected.attempt}`);
}
const generationMillis = elapsedMillis(generationStartedAt);

const horizon = [...baseline, ...accepted];
if (probeNamespace) {
  console.log(JSON.stringify({ segments, accepted: accepted.length, firstAttempt: accepted[0]?.attempt, rejections }));
  process.exit(0);
}
let pairChecks = 0n;
let newCollisions = 0;
const independentVerifierStartedAt = process.hrtime.bigint();
for (let right = 1; right < horizon.length; right += 1) {
  for (let left = 0; left < right; left += 1) {
    pairChecks += 1n;
    const comparison = compareSimilarity(horizon[left], horizon[right]);
    if (right >= baseline.length && comparison.tier !== "NONE") {
      newCollisions += 1;
      throw new Error(`new collision ${horizon[left].id}/${horizon[right].id}: ${comparison.reason}`);
    }
  }
  if (right % 100 === 0) sampleHeap();
  if (right > 0 && right % 250 === 0) console.log(`independent verifier ${right}/${horizon.length - 1}`);
}
const independentVerifierMillis = elapsedMillis(independentVerifierStartedAt);
const expectedPairs = BigInt(horizon.length) * BigInt(horizon.length - 1) / 2n;
if (pairChecks !== expectedPairs) throw new Error(`pair accounting mismatch ${pairChecks}/${expectedPairs}`);
const newObligations = 2220n * BigInt(target) + BigInt(target) * BigInt(target - 1) / 2n;
const schedule = accepted.map((item) => `${item.ordinal}|${item.band}|${item.size}|${item.attempt}|${item.seedHex}`);
const horizonRoot = hashFields("endless-horizon-v1", baselineManifest.endlessBaselineRoot, ...schedule);
const toolingHash = sha256(readFileSync(resolve(root, "tools/generate-content.mjs"), "utf8") + readFileSync(fileURLToPath(import.meta.url), "utf8"));
const segmentExhaustions = segments.length - 1;
const rawCandidates = attempts - rejections.construction;
const metricPasses = rawCandidates - rejections.metricPrefilter;
const shortestProbeCalls = metricPasses - segmentExhaustions;
const solvedProbeCalls = shortestProbeCalls - rejections.shortestProbe;
const fullCertificationCalls = solvedProbeCalls - rejections.exactDefinitionOrGeometric - rejections.structural -
  rejections.solutionGrammar - rejections.hardNear - rejections.reviewSimilarity;
const rejectedAttempts = Object.values(rejections).reduce((sum, count) => sum + count, 0);
const solverMetrics = {
  acceptedOptimalMoveMin: Math.min(...accepted.map((item) => item.solution.depth)),
  acceptedOptimalMoveMax: Math.max(...accepted.map((item) => item.solution.depth)),
  acceptedExpandedStates: accepted.reduce((sum, item) => sum + item.solution.expanded, 0),
  acceptedDiscoveredStates: accepted.reduce((sum, item) => sum + item.solution.discovered, 0),
  acceptedOptimalSolutionCountSum: accepted.reduce((sum, item) => sum + item.solution.optimalCount, 0n).toString(),
};
const percentile = (values, fraction) => {
  if (!values.length) return null;
  const sorted = [...values].sort((left, right) => left - right);
  return Number(sorted[Math.ceil(fraction * sorted.length) - 1].toFixed(3));
};
const scheduleBlocks = Array.from({ length: Math.ceil(target / 100) }, (_, block) => {
  const blockItems = accepted.slice(block * 100, Math.min(target, (block + 1) * 100));
  return {
    block: block + 1,
    count: blockItems.length,
    difficulty: Object.fromEntries(["EASY", "NORMAL", "HARD", "EXPERT", "MASTER"].map((band) =>
      [band, blockItems.filter((item) => item.band === band).length])),
    boardSize: Object.fromEntries([5, 6].map((size) => [size, blockItems.filter((item) => item.size === size).length])),
  };
});
sampleHeap();
const report = {
  status: "PASS",
  generatedCount: target,
  baselineCount: 2220,
  corpusCount: horizon.length,
  versions,
  releaseKeyVersions,
  segments,
  namespaceBases: Object.fromEntries(segments.map((segment) => [segment.id, namespaceBaseFor(segment).toString(16).padStart(16, "0")])),
  endlessBaselineRoot: baselineManifest.endlessBaselineRoot,
  horizonRoot,
  historyRoot,
  attempts,
  candidateAccounting: {
    attempts,
    accepted: target,
    rejectedAttempts,
    segmentExhaustions,
    accountedAttempts: rejectedAttempts + target + segmentExhaustions,
    rawCandidates,
    metricPasses,
    shortestProbeCalls,
    fullCertificationCalls,
  },
  rejections,
  scheduleBlocks,
  solverMetrics,
  exactStageQueryCounts: {
    metricPrefilterCalls: rawCandidates,
    shortestProbeCalls,
    canonicalProbeFingerprintLookups: solvedProbeCalls,
    fullCertificationCalls,
  },
  theoreticalNewPairObligations: newObligations.toString(),
  independentFullCorpusPairChecks: pairChecks.toString(),
  expectedFullCorpusPairChecks: expectedPairs.toString(),
  newCollisions,
  actualFullComparatorCalls: pairChecks.toString(),
  performanceDiagnostics: {
    canonical: false,
    configuredHeapCapBytes: 512 * 1024 * 1024,
    sampledPeakHeapBytes,
    checkpointCertificationReplayP50Millis: percentile(reproductionLevelMillis, 0.50),
    checkpointCertificationReplayP95Millis: percentile(reproductionLevelMillis, 0.95),
    checkpointReproductionMillis: Number(checkpointReproductionMillis.toFixed(3)),
    generatedThisRun: target - resumedAcceptedCount,
    generationMillis: Number(generationMillis.toFixed(3)),
    independentVerifierMillis: Number(independentVerifierMillis.toFixed(3)),
    totalRunMillis: Number(elapsedMillis(runStartedAt).toFixed(3)),
  },
  toolingHash,
  horizonBoardsBundled: false,
};

const metricDistance = (left, right) => Math.min(...right.geometry.map((geometry) =>
  popcount(left.level.walls ^ geometry.walls) + 2 * popcount(left.level.buds ^ geometry.buds) +
  (left.level.start === geometry.start ? 0 : 2)));
const reviewSamples = ["EASY", "NORMAL", "HARD", "EXPERT", "MASTER"].flatMap((band) => {
  const items = accepted.filter((item) => item.band === band);
  return [items[0], items[Math.floor(items.length / 2)], items.at(-1)].filter(Boolean);
});
const csv = [[
  "level_id", "ordinal", "predicted_band", "board_size", "optimal_moves", "expanded_states",
  "nearest_noncolliding_level", "nearest_metric_distance", "reviewer", "attempts", "time_seconds",
  "hint_use", "perceived_difficulty", "duplicate_concern", "decision", "notes",
], ...reviewSamples.map((item) => {
  const nearest = horizon.filter((candidate) => candidate.id !== item.id)
    .map((candidate) => ({ candidate, distance: metricDistance(item, candidate) }))
    .sort((left, right) => left.distance - right.distance || String(left.candidate.id).localeCompare(String(right.candidate.id)))[0];
  return [
    item.id, item.ordinal, item.band, `${item.size}x${item.size}`, item.solution.depth, item.solution.expanded,
    nearest.candidate.id, nearest.distance, "", "", "", "", "", "", "", "",
  ];
})].map((row) => row.map((value) => `"${String(value).replaceAll('"', '""')}"`).join(",")).join("\n") + "\n";
writeFileSync(resolve(reports, "endless-human-review-worksheet.csv"), csv);
writeFileSync(resolve(reports, `endless-horizon-${target}.json`), `${JSON.stringify(report, null, 2)}\n`);
if (target === 1000) writeFileSync(resolve(content, "endless-horizon-stamp.json"), `${JSON.stringify(report, null, 2)}\n`);
console.log(JSON.stringify(report));

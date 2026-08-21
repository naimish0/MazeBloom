import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const content = resolve(root, "app/src/main/assets/content");
const campaignText = readFileSync(resolve(content, "campaign.jsonl"), "utf8");
const dailyText = readFileSync(resolve(content, "daily.jsonl"), "utf8");
const manifest = JSON.parse(readFileSync(resolve(content, "manifest.json"), "utf8"));
const sha256 = (text) => createHash("sha256").update(text, "utf8").digest("hex");

function parseAndVerify(text, expectedCount, type) {
  const lines = text.split("\n").filter(Boolean);
  if (type === "campaign" && lines.length !== expectedCount) throw new Error(`Expected ${expectedCount} campaign levels; found ${lines.length}`);
  if (type === "daily" && lines.length < expectedCount) throw new Error(`Expected at least ${expectedCount} daily levels; found ${lines.length}`);
  const ids = new Set();
  const levels = lines.map((line) => {
    const level = JSON.parse(line);
    if (ids.has(level.id)) throw new Error(`Duplicate ${type} ID: ${level.id}`);
    ids.add(level.id);
    const checksum = level.certificationChecksum;
    delete level.certificationChecksum;
    if (sha256(JSON.stringify(level)) !== checksum) throw new Error(`${level.id}: source checksum mismatch`);
    if (level.width !== level.height || ![5, 6].includes(level.width)) throw new Error(`${level.id}: invalid dimensions`);
    if (level.buds.length < 1 || level.buds.length > 7) throw new Error(`${level.id}: invalid Bud count`);
    for (const field of ["stones", "buds"]) {
      const values = level[field];
      if (new Set(values).size !== values.length || values.some((value, index) => index && value <= values[index - 1])) {
        throw new Error(`${level.id}: ${field} are not canonical`);
      }
      if (values.some((value) => value < 0 || value >= level.width * level.height)) throw new Error(`${level.id}: ${field} out of bounds`);
    }
    if (level.stones.includes(level.start) || level.buds.includes(level.start)) throw new Error(`${level.id}: invalid start overlap`);
    if (!/^[0-9a-f]{16}$/.test(level.generatorSeed)) throw new Error(`${level.id}: invalid seed`);
    if (!/^[0-9a-f]{64}$/.test(checksum)) throw new Error(`${level.id}: invalid checksum`);
    if (!/^[0-9a-f]{64}$/.test(level.dynamicFingerprint)) throw new Error(`${level.id}: invalid dynamic fingerprint`);
    const metrics = level.metrics;
    if (!metrics || metrics.minimumBloomAssistedStops < 0 || metrics.meaningfulDecisionCount < 0 || metrics.forcedMoveRatio < 0 || metrics.forcedMoveRatio > 1) {
      throw new Error(`${level.id}: invalid certification metrics`);
    }
    const profilePass = level.difficulty === "TUTORIAL" ? metrics.forcedMoveRatio <= 1 && (level.campaignOrder !== 4 || metrics.minimumBloomAssistedStops >= 1)
      : level.difficulty === "EASY" ? metrics.meaningfulDecisionCount >= 1 && metrics.minimumBloomAssistedStops >= 1 && metrics.forcedMoveRatio <= .85
      : level.difficulty === "NORMAL" ? metrics.meaningfulDecisionCount >= 2 && metrics.minimumBloomAssistedStops >= 1 && metrics.forcedMoveRatio <= .75
      : level.difficulty === "HARD" ? metrics.meaningfulDecisionCount >= 3 && metrics.minimumBloomAssistedStops >= 2 && metrics.doomedStateCount >= 1 && metrics.forcedMoveRatio <= .70
      : level.difficulty === "EXPERT" ? metrics.meaningfulDecisionCount >= 4 && metrics.minimumBloomAssistedStops >= 2 && metrics.canonicalReplayMaxCreatorUseDistance >= 3 && metrics.forcedMoveRatio <= .65
      : level.difficulty === "MASTER" ? metrics.meaningfulDecisionCount >= 5 && metrics.minimumBloomAssistedStops >= 3 && metrics.canonicalReplayMaxCreatorUseDistance >= 4 && metrics.forcedMoveRatio <= .60
      : false;
    if (!profilePass) throw new Error(`${level.id}: certification profile mismatch`);
    return level;
  });
  return levels;
}

const campaign = parseAndVerify(campaignText, 100, "campaign");
const daily = parseAndVerify(dailyText, 120, "daily");
if (manifest.campaignCount !== campaign.length || manifest.dailyCount !== daily.length) throw new Error("Manifest count drift");
if (manifest.campaignSha256 !== sha256(campaignText)) throw new Error("Campaign aggregate checksum drift");
if (manifest.dailySha256 !== sha256(dailyText)) throw new Error("Daily aggregate checksum drift");
if (new Set([...campaign, ...daily].map((level) => level.id)).size !== campaign.length + daily.length) throw new Error("Cross-pool duplicate ID");
if (new Set([...campaign, ...daily].map((level) => level.dynamicFingerprint)).size !== campaign.length + daily.length) throw new Error("Exact dynamic duplicate");
console.log(`Verified ${campaign.length} campaign levels and ${daily.length} daily levels.`);
console.log(`Campaign SHA-256 ${manifest.campaignSha256}`);
console.log(`Daily SHA-256 ${manifest.dailySha256}`);

import { createHash } from "node:crypto";
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const MASK_64 = (1n << 64n) - 1n;
const DIRECTIONS = [
  ["UP", 0, -1],
  ["RIGHT", 1, 0],
  ["DOWN", 0, 1],
  ["LEFT", -1, 0],
];

class SplitMix64 {
  constructor(seed) { this.state = seed & MASK_64; }
  next() {
    this.state = (this.state + 0x9e3779b97f4a7c15n) & MASK_64;
    let z = this.state;
    z = ((z ^ (z >> 30n)) * 0xbf58476d1ce4e5b9n) & MASK_64;
    z = ((z ^ (z >> 27n)) * 0x94d049bb133111ebn) & MASK_64;
    return (z ^ (z >> 31n)) & MASK_64;
  }
  int(bound) { return Number(this.next() % BigInt(bound)); }
}

const bit = (cell) => 1n << BigInt(cell);
const popcount = (mask) => {
  let count = 0;
  while (mask) { count += 1; mask &= mask - 1n; }
  return count;
};
const cells = (mask, area) => Array.from({ length: area }, (_, cell) => cell).filter((cell) => mask & bit(cell));
const sha256 = (text) => createHash("sha256").update(text, "utf8").digest("hex");

function canMove(level, state, directionIndex) {
  const [, dx, dy] = DIRECTIONS[directionIndex];
  const x = state.seed % level.size;
  const y = Math.floor(state.seed / level.size);
  const nextX = x + dx;
  const nextY = y + dy;
  if (nextX < 0 || nextX >= level.size || nextY < 0 || nextY >= level.size) return false;
  const next = nextY * level.size + nextX;
  return !(level.walls & bit(next)) && !(state.bloom & bit(next));
}

function transition(level, state, directionIndex) {
  const [, dx, dy] = DIRECTIONS[directionIndex];
  let current = state.seed;
  let bloom = state.bloom;
  let buds = state.buds;
  const path = [];
  const departed = [];
  let stopSource = "BOUNDARY";
  while (true) {
    const x = current % level.size;
    const y = Math.floor(current / level.size);
    const nextX = x + dx;
    const nextY = y + dy;
    if (nextX < 0 || nextX >= level.size || nextY < 0 || nextY >= level.size) {
      stopSource = "BOUNDARY";
      break;
    }
    const next = nextY * level.size + nextX;
    if (level.walls & bit(next)) { stopSource = "STONE"; break; }
    if (state.bloom & bit(next)) { stopSource = "BLOOM"; break; }
    departed.push(current);
    current = next;
    path.push(current);
    buds &= ~bit(current);
  }
  if (!path.length) return null;
  departed.forEach((cell) => { bloom |= bit(cell); });
  const after = { seed: current, bloom, buds, status: buds === 0n ? "SOLVED" : "ACTIVE" };
  if (after.status === "ACTIVE" && !DIRECTIONS.some((_, index) => canMove(level, after, index))) after.status = "DEAD";
  return { state: after, path, departed, stopSource };
}

function solve(level, cap = 150_000) {
  const initial = { seed: level.start, bloom: 0n, buds: level.buds, status: "ACTIVE" };
  if (!DIRECTIONS.some((_, index) => canMove(level, initial, index))) return null;
  const key = (state) => `${state.seed}/${state.bloom.toString(16)}/${state.buds.toString(16)}`;
  const queue = [initial];
  const depth = [0];
  const parent = [-1];
  const parentDirection = [-1];
  const seen = new Map([[key(initial), 0]]);
  let head = 0;
  while (head < queue.length && head < cap) {
    const state = queue[head];
    for (let direction = 0; direction < 4; direction += 1) {
      const result = transition(level, state, direction);
      if (!result) continue;
      const stateKey = key(result.state);
      if (seen.has(stateKey)) continue;
      const index = queue.length;
      seen.set(stateKey, index);
      queue.push(result.state);
      depth.push(depth[head] + 1);
      parent.push(head);
      parentDirection.push(direction);
      if (result.state.status === "SOLVED") {
        const replay = [];
        let cursor = index;
        while (parent[cursor] >= 0) {
          replay.push(parentDirection[cursor]);
          cursor = parent[cursor];
        }
        return { depth: depth[index], replay: replay.reverse(), discovered: queue.length };
      }
    }
    head += 1;
  }
  return null;
}

const stateKey = (state) => `${state.seed}/${state.bloom.toString(16)}/${state.buds.toString(16)}`;

function minimumBloomStops(level) {
  const initial = { seed: level.start, bloom: 0n, buds: level.buds, status: "ACTIVE" };
  const best = new Map([[stateKey(initial), 0]]);
  const queue = [{ state: initial, cost: 0, order: 0 }];
  let order = 1;
  let expanded = 0;
  while (queue.length && expanded < 500_000) {
    queue.sort((left, right) => left.cost - right.cost || left.order - right.order);
    const current = queue.shift();
    if (best.get(stateKey(current.state)) !== current.cost) continue;
    if (current.state.status === "SOLVED") return current.cost;
    expanded += 1;
    for (let direction = 0; direction < 4; direction += 1) {
      const result = transition(level, current.state, direction);
      if (!result) continue;
      const cost = current.cost + (result.stopSource === "BLOOM" ? 1 : 0);
      const key = stateKey(result.state);
      if (cost < (best.get(key) ?? Number.MAX_SAFE_INTEGER)) {
        best.set(key, cost);
        queue.push({ state: result.state, cost, order: order++ });
      }
    }
  }
  return null;
}

function audit(level, solution) {
  const initial = { seed: level.start, bloom: 0n, buds: level.buds, status: "ACTIVE" };
  const initialKey = stateKey(initial);
  const states = new Map([[initialKey, initial]]);
  const depth = new Map([[initialKey, 0]]);
  const edges = new Map();
  const reverse = new Map();
  const queue = [initialKey];
  let head = 0;
  while (head < queue.length && states.size < 550_000) {
    const from = queue[head++];
    const state = states.get(from);
    if (state.status !== "ACTIVE") continue;
    const outgoing = [];
    for (let direction = 0; direction < 4; direction += 1) {
      const result = transition(level, state, direction);
      if (!result) continue;
      const to = stateKey(result.state);
      outgoing.push({ to, direction, result });
      if (!reverse.has(to)) reverse.set(to, []);
      reverse.get(to).push(from);
      if (!states.has(to)) {
        states.set(to, result.state);
        depth.set(to, depth.get(from) + 1);
        queue.push(to);
      }
    }
    edges.set(from, outgoing);
  }
  if (states.size >= 550_000) return null;
  const distance = new Map();
  const solvedQueue = [];
  for (const [key, state] of states) if (state.status === "SOLVED") { distance.set(key, 0); solvedQueue.push(key); }
  head = 0;
  while (head < solvedQueue.length) {
    const to = solvedQueue[head++];
    for (const from of reverse.get(to) ?? []) {
      const candidate = distance.get(to) + 1;
      if (candidate < (distance.get(from) ?? Number.MAX_SAFE_INTEGER)) {
        distance.set(from, candidate);
        solvedQueue.push(from);
      }
    }
  }
  let forced = 0;
  let solvableActive = 0;
  let meaningful = 0;
  let earliestMeaningful = null;
  for (const [from, state] of states) {
    if (state.status !== "ACTIVE" || !distance.has(from)) continue;
    solvableActive += 1;
    const outgoing = [...new Map((edges.get(from) ?? []).map((edge) => [edge.to, edge])).values()];
    if (outgoing.length === 1) forced += 1;
    const successorDistances = outgoing.map((edge) => distance.get(edge.to));
    const finite = successorDistances.filter((value) => value !== undefined);
    if (outgoing.length >= 2 && (finite.length !== outgoing.length || new Set(finite).size >= 2)) {
      meaningful += 1;
      earliestMeaningful = Math.min(earliestMeaningful ?? Number.MAX_SAFE_INTEGER, depth.get(from));
    }
  }
  let replayState = initial;
  const createdAt = new Map();
  let maximumDependency = 0;
  solution.replay.forEach((direction, moveIndex) => {
    const result = transition(level, replayState, direction);
    if (result.stopSource === "BLOOM") {
      const [, dx, dy] = DIRECTIONS[direction];
      const x = result.state.seed % level.size;
      const y = Math.floor(result.state.seed / level.size);
      const blocker = (y + dy) * level.size + x + dx;
      maximumDependency = Math.max(maximumDependency, moveIndex - (createdAt.get(blocker) ?? moveIndex));
    }
    result.departed.forEach((cell) => createdAt.set(cell, moveIndex));
    replayState = result.state;
  });
  return {
    reachable: states.size,
    doomed: [...states].filter(([key, state]) => state.status === "ACTIVE" && !distance.has(key)).length,
    meaningful,
    earliestMeaningful,
    forcedRatio: solvableActive ? forced / solvableActive : 0,
    minimumBloomStops: minimumBloomStops(level),
    maximumDependency,
  };
}

function meetsProfile(band, metrics, order) {
  if (!metrics || metrics.minimumBloomStops === null) return false;
  if (band === "TUTORIAL") return metrics.forcedRatio <= 1 && (order !== 4 || metrics.minimumBloomStops >= 1);
  if (band === "EASY") return metrics.meaningful >= 1 && metrics.minimumBloomStops >= 1 && metrics.forcedRatio <= 0.85;
  if (band === "NORMAL") return metrics.meaningful >= 2 && metrics.minimumBloomStops >= 1 && metrics.forcedRatio <= 0.75;
  if (band === "HARD") return metrics.meaningful >= 3 && metrics.minimumBloomStops >= 2 && metrics.doomed >= 1 && metrics.forcedRatio <= 0.70;
  if (band === "EXPERT") return metrics.meaningful >= 4 && metrics.minimumBloomStops >= 2 && metrics.maximumDependency >= 3 && metrics.forcedRatio <= 0.65;
  return metrics.meaningful >= 5 && metrics.minimumBloomStops >= 3 && metrics.maximumDependency >= 4 && metrics.forcedRatio <= 0.60;
}

function geometricEncoding(level) {
  const transformCell = (cell, transform) => {
    let x = cell % level.size;
    let y = Math.floor(cell / level.size);
    const rotations = transform % 4;
    const mirror = transform >= 4;
    for (let count = 0; count < rotations; count += 1) {
      const oldX = x;
      x = level.size - 1 - y;
      y = oldX;
    }
    if (mirror) x = level.size - 1 - x;
    return y * level.size + x;
  };
  return Array.from({ length: 8 }, (_, transform) => {
    const walls = cells(level.walls, level.size * level.size).map((cell) => transformCell(cell, transform)).sort((a, b) => a - b);
    const buds = cells(level.buds, level.size * level.size).map((cell) => transformCell(cell, transform)).sort((a, b) => a - b);
    return `${level.size}|${walls}|${transformCell(level.start, transform)}|${buds}`;
  }).sort()[0];
}

function dynamicFingerprint(level) {
  const initial = { seed: level.start, bloom: 0n, buds: level.buds, status: "ACTIVE" };
  const states = new Map([[stateKey(initial), initial]]);
  const edges = new Map();
  const queue = [initial];
  let head = 0;
  while (head < queue.length) {
    const state = queue[head++];
    const from = stateKey(state);
    const outgoing = [];
    if (state.status === "ACTIVE") for (let direction = 0; direction < 4; direction += 1) {
      const result = transition(level, state, direction);
      if (!result) continue;
      const to = stateKey(result.state);
      outgoing.push({ direction, result, to });
      if (!states.has(to)) { states.set(to, result.state); queue.push(result.state); }
    }
    edges.set(from, outgoing);
  }
  const transformCell = (cell, transform) => {
    let x = cell % level.size;
    let y = Math.floor(cell / level.size);
    for (let count = 0; count < transform % 4; count += 1) { const oldX = x; x = level.size - 1 - y; y = oldX; }
    if (transform >= 4) x = level.size - 1 - x;
    return y * level.size + x;
  };
  const transformMask = (mask, transform) => cells(mask, level.size ** 2).reduce((value, cell) => value | bit(transformCell(cell, transform)), 0n);
  const transformDirection = (direction, transform) => {
    let value = direction;
    for (let count = 0; count < transform % 4; count += 1) value = (value + 1) % 4;
    if (transform >= 4) value = value === 1 ? 3 : value === 3 ? 1 : value;
    return value;
  };
  const encodings = [];
  for (let transform = 0; transform < 8; transform += 1) {
    const normalized = (state) => `${transformCell(state.seed, transform)}:${transformMask(state.bloom, transform).toString(16)}:${transformMask(state.buds, transform).toString(16)}`;
    const records = [];
    for (const [key, state] of states) {
      const outgoing = (edges.get(key) ?? []).map((edge) => `${transformDirection(edge.direction, transform)}>${normalized(states.get(edge.to))}:${edge.result.path.length}:${popcount(state.buds) - popcount(edge.result.state.buds)}:${edge.result.stopSource}`).sort();
      records.push(`${normalized(state)}[${outgoing.join(";")}]`);
    }
    encodings.push(`initial=${normalized(initial)}|${records.sort().join("|")}`);
  }
  return sha256(encodings.sort()[0]);
}

function replayStructure(level, solution) {
  let state = { seed: level.start, bloom: 0n, buds: level.buds, status: "ACTIVE" };
  const pickups = [];
  const stops = [];
  for (const direction of solution.replay) {
    const beforeBuds = state.buds;
    const result = transition(level, state, direction);
    pickups.push(popcount(beforeBuds) - popcount(result.state.buds));
    stops.push(result.stopSource);
    state = result.state;
  }
  return { pickups: pickups.join(","), stops: stops.join(",") };
}

function hardNearDuplicate(candidate, selected) {
  const transformCell = (cell, size, transform) => {
    let x = cell % size;
    let y = Math.floor(cell / size);
    for (let count = 0; count < transform % 4; count += 1) { const oldX = x; x = size - 1 - y; y = oldX; }
    if (transform >= 4) x = size - 1 - x;
    return y * size + x;
  };
  const transformDirection = (direction, transform) => {
    let value = direction;
    for (let count = 0; count < transform % 4; count += 1) value = (value + 1) % 4;
    if (transform >= 4) value = value === 1 ? 3 : value === 3 ? 1 : value;
    return value;
  };
  for (const existing of selected) {
    if (existing.level.size !== candidate.level.size) continue;
    const distances = [];
    for (let transform = 0; transform < 8; transform += 1) {
      let walls = 0n;
      let buds = 0n;
      cells(candidate.level.walls, candidate.level.size ** 2).forEach((cell) => { walls |= bit(transformCell(cell, candidate.level.size, transform)); });
      cells(candidate.level.buds, candidate.level.size ** 2).forEach((cell) => { buds |= bit(transformCell(cell, candidate.level.size, transform)); });
      const start = transformCell(candidate.level.start, candidate.level.size, transform);
      distances.push({ transform, distance: popcount(existing.level.walls ^ walls) + 2 * popcount(existing.level.buds ^ buds) + (existing.level.start === start ? 0 : 2) });
    }
    const minimum = Math.min(...distances.map((value) => value.distance));
    if (minimum > 2) continue;
    const patternsMatch = existing.structure.pickups === candidate.structure.pickups && existing.structure.stops === candidate.structure.stops;
    if (!patternsMatch) continue;
    if (distances.filter((value) => value.distance === minimum).some(({ transform }) =>
      existing.solution.replay.join(",") === candidate.solution.replay.map((direction) => transformDirection(direction, transform)).join(","))) return true;
  }
  return false;
}

function constructCandidate(seed, size, targetDepth, budCount) {
  const random = new SplitMix64(seed);
  let walls = 0n;
  const density = size === 5 ? 4 + random.int(4) : 6 + random.int(6);
  for (let index = 0; index < density; index += 1) walls |= bit(random.int(size * size));
  const start = random.int(size * size);
  walls &= ~bit(start);
  const level = { size, walls, start, buds: 0n };
  let beam = [{ state: { seed: start, bloom: 0n, buds: 1n, status: "ACTIVE" }, history: [], paths: [], score: 0 }];
  for (let depth = 0; depth < targetDepth; depth += 1) {
    const next = [];
    for (const node of beam) {
      for (let direction = 0; direction < 4; direction += 1) {
        const result = transition(level, node.state, direction);
        if (!result) continue;
        const bloomStops = node.paths.filter((path) => path.stopSource === "BLOOM").length + (result.stopSource === "BLOOM" ? 1 : 0);
        const turns = node.history.length && node.history.at(-1) !== direction ? 1 : 0;
        const open = size * size - density - popcount(result.state.bloom);
        next.push({
          state: { ...result.state, buds: 1n, status: "ACTIVE" },
          history: [...node.history, direction],
          paths: [...node.paths, result],
          score: bloomStops * 500 + turns * 80 + open * 5 + random.int(1000) / 1000,
        });
      }
    }
    if (!next.length) return null;
    next.sort((left, right) => right.score - left.score);
    const deduped = new Set();
    beam = [];
    for (const item of next) {
      const key = `${item.state.seed}/${item.state.bloom}`;
      if (deduped.has(key)) continue;
      deduped.add(key);
      beam.push(item);
      if (beam.length >= 128) break;
    }
  }
  const picked = beam[random.int(Math.min(beam.length, 12))];
  const eligible = picked.paths.filter((path) => path.path.length);
  if (!eligible.length) return null;
  const traversedCells = [...new Set(eligible.flatMap((path) => path.path))]
    .filter((cell) => cell !== start && !(walls & bit(cell)));
  const desiredBudCount = Math.min(budCount, traversedCells.length);
  if (desiredBudCount < Math.min(2, budCount)) return null;
  const selectedMoves = [eligible.length - 1];
  while (selectedMoves.length < Math.min(desiredBudCount, eligible.length)) {
    const index = random.int(eligible.length);
    if (!selectedMoves.includes(index)) selectedMoves.push(index);
  }
  let buds = 0n;
  selectedMoves.forEach((moveIndex) => {
    const path = eligible[moveIndex].path;
    buds |= bit(path[random.int(path.length)]);
  });
  while (popcount(buds) < desiredBudCount) buds |= bit(traversedCells[random.int(traversedCells.length)]);
  buds &= ~bit(start);
  buds &= ~walls;
  if (!buds) return null;
  return { ...level, buds };
}

function buildPool(count, sizeForOrder, bandForOrder, baseSeed, usedEncodings, usedDynamicFingerprints, selectedItems, initialItems = []) {
  const accepted = [...initialItems];
  let attempts = 0;
  const rejections = { construction: 0, unsolvedOrDepth: 0, profile: 0, exactDuplicate: 0, dynamicDuplicate: 0, hardNearDuplicate: 0 };
  const ranges = { TUTORIAL: [1, 4], EASY: [3, 7], NORMAL: [5, 10], HARD: [7, 13], EXPERT: [9, 16], MASTER: [11, 20] };
  while (accepted.length < count && attempts < 150_000) {
    const order = accepted.length + 1;
    const band = bandForOrder(order);
    const [minimum, maximum] = ranges[band];
    const seed = (baseSeed + BigInt(attempts) * 0x9e3779b97f4a7c15n) & MASK_64;
    attempts += 1;
    const target = minimum + Number(seed % BigInt(Math.min(maximum - minimum + 1, 6)));
    const budCount = band === "TUTORIAL" ? 1 + Number(seed % 2n) : 2 + Number((seed >> 8n) % 6n);
    const level = constructCandidate(seed, sizeForOrder(order), target, budCount);
    if (!level) { rejections.construction += 1; continue; }
    const solution = solve(level);
    if (!solution || solution.depth < minimum || solution.depth > maximum) { rejections.unsolvedOrDepth += 1; continue; }
    const metrics = audit(level, solution);
    if (!meetsProfile(band, metrics, order)) { rejections.profile += 1; continue; }
    const encoding = geometricEncoding(level);
    if (usedEncodings.has(encoding)) { rejections.exactDuplicate += 1; continue; }
    const dynamic = dynamicFingerprint(level);
    if (usedDynamicFingerprints.has(dynamic)) { rejections.dynamicDuplicate += 1; continue; }
    const item = { level, solution, seed, band, ordinal: attempts - 1, metrics, dynamic, structure: replayStructure(level, solution) };
    if (hardNearDuplicate(item, selectedItems)) { rejections.hardNearDuplicate += 1; continue; }
    usedEncodings.add(encoding);
    usedDynamicFingerprints.add(dynamic);
    accepted.push(item);
    selectedItems.push(item);
  }
  if (accepted.length !== count) throw new Error(`Generation budget exhausted at ${accepted.length}/${count}`);
  return { accepted, attempts, rejections };
}

function contentRecord(item, id, chapter, campaignOrder) {
  const { level, solution, seed, band } = item;
  const record = {
    id,
    schemaVersion: 1,
    contentVersion: 1,
    rulesVersion: 1,
    width: level.size,
    height: level.size,
    stones: cells(level.walls, level.size * level.size),
    start: level.start,
    buds: cells(level.buds, level.size * level.size),
    chapter,
    campaignOrder,
    generatorVersion: item.handmade ? 0 : 1,
    generatorSeed: seed.toString(16).padStart(16, "0"),
    solverVersion: 1,
    certificationProfileVersion: 1,
    fingerprintVersion: 1,
    difficulty: band,
    optimalMoves: solution.depth,
    canonicalReplay: solution.replay.map((direction) => DIRECTIONS[direction][0]),
    dynamicFingerprint: item.dynamic,
    metrics: {
      reachableStates: item.metrics.reachable,
      doomedStateCount: item.metrics.doomed,
      meaningfulDecisionCount: item.metrics.meaningful,
      earliestMeaningfulBranch: item.metrics.earliestMeaningful,
      forcedMoveRatio: Number(item.metrics.forcedRatio.toFixed(6)),
      minimumBloomAssistedStops: item.metrics.minimumBloomStops,
      canonicalReplayMaxCreatorUseDistance: item.metrics.maximumDependency,
    },
  };
  return JSON.stringify({ ...record, certificationChecksum: sha256(JSON.stringify(record)) });
}

export function generateAll() {
const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const contentDirectory = resolve(root, "app/src/main/assets/content");
mkdirSync(contentDirectory, { recursive: true });
const usedEncodings = new Set();
const usedDynamicFingerprints = new Set();
const selectedItems = [];
const handmadeDefinitions = [
  { walls: [], start: 10, buds: [14], seed: 0x0000000000000001n },
  { walls: [], start: 10, buds: [11, 14], seed: 0x0000000000000002n },
  { walls: [2, 6, 7, 23], start: 5, buds: [11, 17], seed: 0x0000000000000003n },
  { walls: [0, 2, 6, 7, 23], start: 4, buds: [3, 9], seed: 0x0000000000000004n },
  { walls: [0, 2, 6, 7, 23], start: 5, buds: [10, 11], seed: 0x0000000000000005n },
];
const handmade = handmadeDefinitions.map((definition, index) => {
  const level = { size: 5, walls: definition.walls.reduce((mask, cell) => mask | bit(cell), 0n), start: definition.start, buds: definition.buds.reduce((mask, cell) => mask | bit(cell), 0n) };
  const solution = solve(level);
  const metrics = audit(level, solution);
  if (!solution || solution.depth > 4 || definition.buds.length > 2 || !meetsProfile("TUTORIAL", metrics, index + 1)) throw new Error(`Handmade onboarding ${index + 1} failed certification`);
  const item = { level, solution, seed: definition.seed, band: "TUTORIAL", ordinal: index, metrics, dynamic: dynamicFingerprint(level), structure: replayStructure(level, solution), handmade: true };
  const encoding = geometricEncoding(level);
  if (usedEncodings.has(encoding) || usedDynamicFingerprints.has(item.dynamic) || hardNearDuplicate(item, selectedItems)) throw new Error(`Handmade onboarding ${index + 1} is a duplicate`);
  usedEncodings.add(encoding);
  usedDynamicFingerprints.add(item.dynamic);
  selectedItems.push(item);
  return item;
});
const campaign = buildPool(
  100,
  (order) => order <= 70 ? 5 : 6,
  (order) => order <= 5 ? "TUTORIAL" : order <= 20 ? "EASY" : order <= 40 ? "NORMAL" : order <= 70 ? "HARD" : order <= 95 ? "EXPERT" : "MASTER",
  0x4d415a45424c4f4fn,
  usedEncodings,
  usedDynamicFingerprints,
  selectedItems,
  handmade,
);
const daily = buildPool(
  120,
  (order) => order % 3 === 0 ? 6 : 5,
  (order) => order % 10 < 3 ? "EASY" : order % 10 < 7 ? "NORMAL" : order % 10 < 9 ? "HARD" : "EXPERT",
  0x4441494c59424c4fn,
  usedEncodings,
  usedDynamicFingerprints,
  selectedItems,
);
const campaignText = campaign.accepted.map((item, index) => contentRecord(item, `campaign-${String(index + 1).padStart(3, "0")}`, Math.floor(index / 20) + 1, index + 1)).join("\n") + "\n";
const dailyText = daily.accepted.map((item, index) => contentRecord(item, `daily-${String(index + 1).padStart(3, "0")}`, 0, 0)).join("\n") + "\n";
const manifest = {
  schemaVersion: 1,
  contentVersion: 1,
  rulesVersion: 1,
  campaignCount: 100,
  dailyPoolVersion: 1,
  dailyCount: 120,
  campaignSha256: sha256(campaignText),
  dailySha256: sha256(dailyText),
  humanReviewed: false,
  generator: {
    version: 1,
    campaignAttempts: campaign.attempts,
    dailyAttempts: daily.attempts,
    campaignRejections: campaign.rejections,
    dailyRejections: daily.rejections,
  },
};
writeFileSync(resolve(contentDirectory, "campaign.jsonl"), campaignText, "utf8");
writeFileSync(resolve(contentDirectory, "daily.jsonl"), dailyText, "utf8");
writeFileSync(resolve(contentDirectory, "manifest.json"), `${JSON.stringify(manifest, null, 2)}\n`, "utf8");
const reportDirectory = resolve(root, "reports/content");
mkdirSync(reportDirectory, { recursive: true });
const all = [...campaign.accepted, ...daily.accepted];
const distribution = (items, field) => Object.fromEntries([...new Set(items.map((item) => field(item)))].sort().map((value) => [value, items.filter((item) => field(item) === value).length]));
const report = {
  reportVersion: 1,
  automatedCertification: "PASS",
  humanReview: "PENDING",
  counts: { campaign: campaign.accepted.length, daily: daily.accepted.length },
  checksums: { campaign: manifest.campaignSha256, daily: manifest.dailySha256 },
  attempts: { campaign: campaign.attempts, daily: daily.attempts },
  rejections: { campaign: campaign.rejections, daily: daily.rejections },
  distributions: {
    campaignDifficulty: distribution(campaign.accepted, (item) => item.band),
    campaignBoardSize: distribution(campaign.accepted, (item) => `${item.level.size}x${item.level.size}`),
    dailyDifficulty: distribution(daily.accepted, (item) => item.band),
    dailyBoardSize: distribution(daily.accepted, (item) => `${item.level.size}x${item.level.size}`),
  },
  authoritativeMaxima: {
    shortestDiscoveredStates: Math.max(...all.map((item) => item.solution.discovered)),
    auditReachableStates: Math.max(...all.map((item) => item.metrics.reachable)),
  },
};
writeFileSync(resolve(reportDirectory, "certification-summary.json"), `${JSON.stringify(report, null, 2)}\n`, "utf8");
const reviewHeader = "levelId,predictedBand,optimalMoves,meaningfulDecisions,forcedMoveRatio,minimumBloomStops,automatedCertified,humanApproved,reviewer,attempts,solveTimeSeconds,undos,restarts,hints,perceivedDifficulty,duplicateConcern,decision,notes";
const reviewRows = campaign.accepted.map((item, index) => [
  `campaign-${String(index + 1).padStart(3, "0")}`, item.band, item.solution.depth, item.metrics.meaningful,
  item.metrics.forcedRatio.toFixed(6), item.metrics.minimumBloomStops, "true", "", "", "", "", "", "", "", "", "", "", "",
].join(","));
writeFileSync(resolve(reportDirectory, "human-review-worksheet.csv"), `${reviewHeader}\n${reviewRows.join("\n")}\n`, "utf8");
console.log(JSON.stringify(manifest));
return manifest;
}

export { audit, geometricEncoding, solve, transition };

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) generateAll();

import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
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
  const pathCounts = [1n];
  const seen = new Map([[key(initial), 0]]);
  let head = 0;
  let solvedDepth = null;
  let solvedIndex = null;
  let solvedCount = 0n;
  let optimalOverflow = false;
  const addCount = (left, right) => {
    const maximum = (1n << 63n) - 1n;
    const sum = left + right;
    if (sum > maximum) { optimalOverflow = true; return maximum; }
    return sum;
  };
  while (head < queue.length && head < cap) {
    const state = queue[head];
    if (solvedDepth !== null && depth[head] >= solvedDepth) { head += 1; continue; }
    for (let direction = 0; direction < 4; direction += 1) {
      const result = transition(level, state, direction);
      if (!result) continue;
      const stateKey = key(result.state);
      const nextDepth = depth[head] + 1;
      const known = seen.get(stateKey);
      if (known === undefined) {
        const index = queue.length;
        seen.set(stateKey, index);
        queue.push(result.state);
        depth.push(nextDepth);
        parent.push(head);
        parentDirection.push(direction);
        pathCounts.push(pathCounts[head]);
        if (result.state.status === "SOLVED") {
          if (solvedDepth === null) { solvedDepth = nextDepth; solvedIndex = index; }
          if (nextDepth === solvedDepth) solvedCount = addCount(solvedCount, pathCounts[head]);
        }
      } else if (depth[known] === nextDepth) {
        pathCounts[known] = addCount(pathCounts[known], pathCounts[head]);
        if (result.state.status === "SOLVED" && nextDepth === solvedDepth) solvedCount = addCount(solvedCount, pathCounts[head]);
      }
    }
    head += 1;
  }
  if (solvedIndex === null) return null;
  const replay = [];
  let cursor = solvedIndex;
  while (parent[cursor] >= 0) {
    replay.push(parentDirection[cursor]);
    cursor = parent[cursor];
  }
  return { depth: solvedDepth, replay: replay.reverse(), discovered: queue.length, expanded: head, optimalCount: solvedCount, optimalOverflow };
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
  const traversedCells = new Set([level.start]);
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
      result.path.forEach((cell) => traversedCells.add(cell));
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
    traversedCells,
  };
}

function meetsProfile(band, metrics, order, profileVersion = CERTIFICATION_PROFILE_VERSION) {
  if (!metrics || metrics.minimumBloomStops === null) return false;
  if (profileVersion < 3) {
    if (band === "TUTORIAL") return metrics.forcedRatio <= 1 && (order !== 4 || metrics.minimumBloomStops >= 1);
    if (band === "EASY") return metrics.meaningful >= 1 && metrics.minimumBloomStops >= 1 && metrics.forcedRatio <= 0.85;
    if (band === "NORMAL") return metrics.meaningful >= 2 && metrics.minimumBloomStops >= 1 && metrics.forcedRatio <= 0.75;
    if (band === "HARD") return metrics.meaningful >= 3 && metrics.minimumBloomStops >= 2 && metrics.doomed >= 1 && metrics.forcedRatio <= 0.70;
    if (band === "EXPERT") return metrics.meaningful >= 4 && metrics.minimumBloomStops >= 2 && metrics.maximumDependency >= 3 && metrics.forcedRatio <= 0.65;
    return metrics.meaningful >= 5 && metrics.minimumBloomStops >= 3 && metrics.maximumDependency >= 4 && metrics.forcedRatio <= 0.60;
  }
  if (band === "TUTORIAL") return metrics.meaningful >= 1 && metrics.minimumBloomStops >= 1 && metrics.forcedRatio <= 0.90;
  if (band === "EASY") return metrics.meaningful >= 2 && metrics.minimumBloomStops >= 1 && metrics.forcedRatio <= 0.80;
  if (band === "NORMAL") return metrics.meaningful >= 3 && metrics.minimumBloomStops >= 1 && metrics.doomed >= 2 && metrics.forcedRatio <= 0.70;
  if (band === "HARD") return metrics.meaningful >= 4 && metrics.minimumBloomStops >= 2 && metrics.doomed >= 2 && metrics.maximumDependency >= 3 && metrics.forcedRatio <= 0.65;
  if (band === "EXPERT") return metrics.meaningful >= 5 && metrics.minimumBloomStops >= 2 && metrics.doomed >= 2 && metrics.maximumDependency >= 4 && metrics.forcedRatio <= 0.60;
  return metrics.meaningful >= 6 && metrics.minimumBloomStops >= 3 && metrics.doomed >= 3 && metrics.maximumDependency >= 5 && metrics.forcedRatio <= 0.55;
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
  const directions = [];
  const pickups = [];
  const stops = [];
  const slideLengths = [];
  const bloomCounts = [];
  const remainingBudCounts = [];
  const creatorUseDistances = [];
  const createdAt = new Map();
  solution.replay.forEach((direction, moveIndex) => {
    const beforeBuds = state.buds;
    const result = transition(level, state, direction);
    directions.push(direction);
    pickups.push(popcount(beforeBuds) - popcount(result.state.buds));
    stops.push(result.stopSource);
    slideLengths.push(result.path.length);
    bloomCounts.push(result.departed.length);
    remainingBudCounts.push(popcount(result.state.buds));
    const [, dx, dy] = DIRECTIONS[direction];
    const x = result.state.seed % level.size;
    const y = Math.floor(result.state.seed / level.size);
    const blocker = (y + dy) * level.size + x + dx;
    creatorUseDistances.push(result.stopSource === "BLOOM" ? moveIndex - (createdAt.get(blocker) ?? moveIndex) : 0);
    result.departed.forEach((cell) => createdAt.set(cell, moveIndex));
    state = result.state;
  });
  const transformDirection = (direction, transform) => {
    let value = direction;
    for (let count = 0; count < transform % 4; count += 1) value = (value + 1) % 4;
    if (transform >= 4) value = value === 1 ? 3 : value === 3 ? 1 : value;
    return value;
  };
  const structures = Array.from({ length: 8 }, (_, transform) => directions.map((direction, index) => [
    transformDirection(direction, transform), slideLengths[index], pickups[index], stops[index],
    creatorUseDistances[index], bloomCounts[index], remainingBudCounts[index],
  ].join(":")).join("|"));
  const grammars = Array.from({ length: 8 }, (_, transform) =>
    `${directions.map((direction) => transformDirection(direction, transform)).join(",")}|${pickups.join(",")}|${stops.join(",")}`);
  return {
    directions, pickups, stops, slideLengths, bloomCounts, remainingBudCounts, creatorUseDistances,
    structuralFingerprint: sha256(structures.sort()[0]),
    grammarFingerprint: sha256(grammars.sort()[0]),
  };
}

function transformedGeometry(level) {
  const transformCell = (cell, transform) => {
    let x = cell % level.size;
    let y = Math.floor(cell / level.size);
    for (let count = 0; count < transform % 4; count += 1) { const oldX = x; x = level.size - 1 - y; y = oldX; }
    if (transform >= 4) x = level.size - 1 - x;
    return y * level.size + x;
  };
  return Array.from({ length: 8 }, (_, transform) => ({
    transform,
    walls: cells(level.walls, level.size ** 2).reduce((mask, cell) => mask | bit(transformCell(cell, transform)), 0n),
    buds: cells(level.buds, level.size ** 2).reduce((mask, cell) => mask | bit(transformCell(cell, transform)), 0n),
    start: transformCell(level.start, transform),
  }));
}

function transformDirection(direction, transform) {
  let value = direction;
  for (let count = 0; count < transform % 4; count += 1) value = (value + 1) % 4;
  if (transform >= 4) value = value === 1 ? 3 : value === 3 ? 1 : value;
  return value;
}

function prepareItem(item) {
  return { ...item, geometry: transformedGeometry(item.level), geometric: geometricEncoding(item.level) };
}

function compareSimilarity(first, second) {
  if (first.structure.structuralFingerprint === second.structure.structuralFingerprint) return { tier: "HARD", reason: "STRUCTURAL_SIGNATURE" };
  if (first.structure.grammarFingerprint === second.structure.grammarFingerprint) return { tier: "HARD", reason: "SOLUTION_GRAMMAR" };
  if (first.level.size !== second.level.size) return { tier: "NONE" };
  const alignments = second.geometry.map((geometry) => {
    const stoneDistance = popcount(first.level.walls ^ geometry.walls);
    const budDistance = 2 * popcount(first.level.buds ^ geometry.buds);
    const startDistance = first.level.start === geometry.start ? 0 : 2;
    return {
      transform: geometry.transform,
      stoneDistance, budDistance, startDistance,
      distance: stoneDistance + budDistance + startDistance,
      directionMatch: first.solution.replay.join(",") === second.solution.replay.map((direction) => transformDirection(direction, geometry.transform)).join(","),
      pickupMatch: first.structure.pickups.join(",") === second.structure.pickups.join(","),
      stopMatch: first.structure.stops.join(",") === second.structure.stops.join(","),
      encoding: `${geometry.walls.toString(16)}|${geometry.start}|${geometry.buds.toString(16)}`,
    };
  });
  const minimum = Math.min(...alignments.map(({ distance }) => distance));
  const chosen = alignments.filter(({ distance }) => distance === minimum).sort((left, right) => {
    const leftMatches = Number(left.directionMatch) + Number(left.pickupMatch) + Number(left.stopMatch);
    const rightMatches = Number(right.directionMatch) + Number(right.pickupMatch) + Number(right.stopMatch);
    return rightMatches - leftMatches || left.encoding.localeCompare(right.encoding) || left.transform - right.transform;
  })[0];
  const matched = Number(chosen.directionMatch) + Number(chosen.pickupMatch) + Number(chosen.stopMatch);
  const tier = minimum <= 6 || (minimum <= 12 && matched >= 1) || (minimum <= 18 && matched >= 2)
    ? "HARD"
    : minimum <= 12 || (minimum <= 18 && matched === 1) ? "REVIEW" : "NONE";
  const reason = tier === "HARD" && minimum <= 6 ? "MASK_DISTANCE_6"
    : tier === "HARD" && minimum <= 12 ? "MASK_DISTANCE_12_BEHAVIOR"
    : tier === "HARD" ? "MASK_DISTANCE_18_TWO_BEHAVIORS"
    : tier === "REVIEW" && minimum <= 12 ? "REVIEW_MASK_DISTANCE_12"
    : tier === "REVIEW" ? "REVIEW_MASK_DISTANCE_18_BEHAVIOR" : "NONE";
  return { tier, reason, ...chosen };
}

function hardNearDuplicate(candidate, selected) {
  return selected.some((existing) => compareSimilarity(existing, candidate).tier !== "NONE");
}

function constructCandidate(seed, size, targetDepth, budCount, densityBoost = 0) {
  const random = new SplitMix64(seed);
  let walls = 0n;
  const density = size === 5
    ? 4 + densityBoost + random.int(4 + densityBoost)
    : 6 + densityBoost * 2 + random.int(6 + densityBoost);
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
        const futureMoves = DIRECTIONS.filter((_, nextDirection) => canMove(level, { ...result.state, status: "ACTIVE" }, nextDirection)).length;
        next.push({
          state: { ...result.state, buds: 1n, status: "ACTIVE" },
          history: [...node.history, direction],
          paths: [...node.paths, result],
          score: bloomStops * 260 + turns * 60 + futureMoves * 220 + open * 55 - result.path.length * 35 + random.int(1000) / 1000,
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
      if (beam.length >= 64) break;
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
  return { ...level, buds, _constructionReplay: picked.history };
}

const LEGACY_RANGES = { TUTORIAL: [1, 4], EASY: [3, 7], NORMAL: [5, 10], HARD: [7, 13], EXPERT: [9, 16], MASTER: [11, 20] };
const RANGES = { TUTORIAL: [2, 5], EASY: [4, 8], NORMAL: [6, 11], HARD: [8, 14], EXPERT: [10, 17], MASTER: [12, 20] };
const PACING = [
  "EASY EASY NORMAL EASY NORMAL HARD NORMAL EASY NORMAL HARD EXPERT EASY NORMAL HARD NORMAL EASY NORMAL HARD EXPERT MASTER",
  "EASY NORMAL HARD NORMAL EASY NORMAL HARD EXPERT NORMAL HARD EASY NORMAL HARD EXPERT EASY NORMAL NORMAL HARD EXPERT MASTER",
  "EASY NORMAL HARD NORMAL HARD EXPERT NORMAL HARD EASY NORMAL HARD EXPERT HARD NORMAL HARD EXPERT NORMAL HARD EXPERT MASTER",
  "EASY EASY NORMAL HARD EXPERT MASTER EASY NORMAL HARD EXPERT HARD NORMAL HARD EXPERT HARD NORMAL HARD EXPERT EXPERT MASTER",
  "EASY NORMAL HARD EXPERT MASTER NORMAL HARD EXPERT HARD NORMAL HARD EXPERT MASTER NORMAL HARD EXPERT HARD EXPERT EXPERT MASTER",
].map((value) => value.split(" "));
const FIVE_BY_FIVE = { EASY: 12, NORMAL: 14, HARD: 0, EXPERT: 0, MASTER: 0 };
const TOTAL_BY_BAND = { EASY: 16, NORMAL: 28, HARD: 28, EXPERT: 20, MASTER: 8 };
const GENERATION_NAMESPACE_VERSION = 1;
const GENERATOR_VERSION = 4;
const CERTIFICATION_PROFILE_VERSION = 3;
const FINGERPRINT_VERSION = 2;
const UNIQUENESS_PROFILE_VERSION = 2;
const DAILY_POOL_VERSION = 2;
const PROGRESSIVE_POOL_VERSION = 1;

const internalLevel = (record) => ({
  size: record.width,
  walls: record.stones.reduce((mask, cell) => mask | bit(cell), 0n),
  start: record.start,
  buds: record.buds.reduce((mask, cell) => mask | bit(cell), 0n),
});

function hashFields(...fields) {
  return sha256(fields.map((field) => {
    const value = String(field);
    return `${Buffer.byteLength(value, "utf8")}:${value}`;
  }).join(""));
}

function deriveSeed(...fields) {
  const digest = createHash("sha256").update(fields.map((field) => {
    const value = String(field);
    return `${Buffer.byteLength(value, "utf8")}:${value}`;
  }).join(""), "utf8").digest();
  let seed = 0n;
  for (let index = 0; index < 8; index += 1) seed = (seed << 8n) | BigInt(digest[index]);
  return seed & MASK_64;
}

function repeatingBand(order) {
  const local = (order - 1) % 100;
  return PACING[Math.floor(local / 20)][local % 20];
}

function repeatingSize(order) {
  const local = (order - 1) % 100;
  const band = repeatingBand(order);
  let rank = 0;
  for (let index = 0; index < local; index += 1) if (PACING[Math.floor(index / 20)][index % 20] === band) rank += 1;
  const quota = FIVE_BY_FIVE[band];
  const total = TOTAL_BY_BAND[band];
  const ceilDiv = (a, b) => Math.floor((a + b - 1) / b);
  return ceilDiv((rank + 1) * quota, total) > ceilDiv(rank * quota, total) ? 5 : 6;
}

function campaignBand(order) {
  if (order <= 5) return "TUTORIAL";
  if (order <= 20) return "EASY";
  if (order <= 40) return "NORMAL";
  if (order <= 70) return "HARD";
  if (order <= 95) return "EXPERT";
  if (order <= 100) return "MASTER";
  return repeatingBand(order);
}

function campaignSize(order) {
  if (order <= 70) return 5;
  if (order <= 100) return 6;
  return repeatingSize(order);
}

function isOneLine(level) {
  const budCells = cells(level.buds, level.size ** 2);
  return new Set(budCells.map((cell) => cell % level.size)).size === 1 || new Set(budCells.map((cell) => Math.floor(cell / level.size))).size === 1;
}

function hasCompleteOpenCellCoverage(level, metrics) {
  for (let cell = 0; cell < level.size ** 2; cell += 1) {
    if (!(level.walls & bit(cell)) && !metrics.traversedCells.has(cell)) return false;
  }
  return true;
}

function emptyRejections() {
  return {
    construction: 0, unsolvableOrRequiredBudget: 0, trivialOrOneLine: 0, profile: 0,
    irrelevantOpenCell: 0, exactDefinitionOrGeometric: 0, dynamic: 0, structural: 0,
    solutionGrammar: 0, hardNear: 0, reviewSimilarity: 0,
  };
}

function legacyItem(record, line) {
  const level = internalLevel(record);
  const solution = {
    depth: record.optimalMoves,
    replay: record.canonicalReplay.map((name) => DIRECTIONS.findIndex(([direction]) => direction === name)),
    discovered: record.metrics.reachableStates,
    optimalCount: Number(record.optimalSolutionCount ?? 1),
    optimalOverflow: Boolean(record.optimalCountOverflow),
  };
  const metrics = {
    reachable: record.metrics.reachableStates,
    doomed: record.metrics.doomedStateCount,
    meaningful: record.metrics.meaningfulDecisionCount,
    earliestMeaningful: record.metrics.earliestMeaningfulBranch,
    forcedRatio: record.metrics.forcedMoveRatio,
    minimumBloomStops: record.metrics.minimumBloomAssistedStops,
    maximumDependency: record.metrics.canonicalReplayMaxCreatorUseDistance,
    traversedCells: new Set(Array.from({ length: level.size ** 2 }, (_, cell) => cell).filter((cell) => !(level.walls & bit(cell)))),
  };
  return prepareItem({
    id: record.id, campaignOrder: record.campaignOrder, level, solution, metrics,
    seed: BigInt(`0x${record.generatorSeed}`), band: record.difficulty, ordinal: record.campaignOrder - 1,
    dynamic: record.dynamicFingerprint, structure: replayStructure(level, solution), legacy: true, sourceLine: line,
  });
}

function rawCandidateFor(seed, size, band, densityBoost = 0) {
  const [minimum, maximum] = RANGES[band];
  const target = band === "MASTER" || band === "EXPERT"
    ? minimum
    : Math.min(maximum, minimum + (band === "EASY" ? 1 : 0) + Number(seed % BigInt(Math.min(maximum - minimum + 1, 6))));
  const budCount = band === "MASTER" || band === "EXPERT" ? 7 : Math.min(7, (band === "EASY" ? 3 : 2) + Number((seed >> 8n) % 6n));
  let level = constructCandidate(seed, size, target, budCount, densityBoost);
  if (!level) return { rejection: "construction" };
  if (isOneLine(level)) return { rejection: "trivialOrOneLine" };
  return { level };
}

function certifyRawCandidate(seed, band, initialLevel) {
  const [minimum, maximum] = RANGES[band];
  let level = initialLevel;
  let solution = solve(level, 250_000);
  if (!solution || solution.depth < minimum || solution.depth > maximum) return { rejection: "unsolvableOrRequiredBudget" };
  let metrics = audit(level, solution);
  if (metrics) {
    let irrelevant = 0n;
    for (let cell = 0; cell < level.size ** 2; cell += 1) {
      if (!(level.walls & bit(cell)) && !metrics.traversedCells.has(cell) && cell !== level.start && !(level.buds & bit(cell))) irrelevant |= bit(cell);
    }
    if (irrelevant) {
      level = { ...level, walls: level.walls | irrelevant };
      solution = solve(level, 250_000);
      metrics = solution ? audit(level, solution) : null;
    }
  }
  if (!metrics || !meetsProfile(band, metrics, 101)) return { rejection: "profile", diagnostics: metrics };
  if (!hasCompleteOpenCellCoverage(level, metrics)) return { rejection: "irrelevantOpenCell" };
  const item = prepareItem({ level, solution, seed, band, metrics, dynamic: dynamicFingerprint(level), structure: replayStructure(level, solution) });
  return { item };
}

function candidateFor(seed, size, band) {
  const raw = rawCandidateFor(seed, size, band);
  return raw.rejection ? raw : certifyRawCandidate(seed, band, raw.level);
}

function difficultyRange(band, profileVersion = CERTIFICATION_PROFILE_VERSION) {
  return (profileVersion < 3 ? LEGACY_RANGES : RANGES)[band];
}

function uniquenessRejection(item, registry, geometricSet, dynamicSet, structuralSet, grammarSet) {
  if (geometricSet.has(item.geometric)) return "exactDefinitionOrGeometric";
  if (dynamicSet.has(item.dynamic)) return "dynamic";
  if (structuralSet.has(item.structure.structuralFingerprint)) return "structural";
  if (grammarSet.has(item.structure.grammarFingerprint)) return "solutionGrammar";
  for (const existing of registry) {
    const comparison = compareSimilarity(existing, item);
    if (comparison.tier === "HARD") return "hardNear";
    if (comparison.tier === "REVIEW") return "reviewSimilarity";
  }
  return null;
}

function register(item, registry, geometricSet, dynamicSet, structuralSet, grammarSet) {
  registry.push(item);
  geometricSet.add(item.geometric);
  dynamicSet.add(item.dynamic);
  structuralSet.add(item.structure.structuralFingerprint);
  grammarSet.add(item.structure.grammarFingerprint);
}

function generateExtension(legacyItems) {
  const accepted = [...legacyItems];
  const geometricSet = new Set(legacyItems.map((item) => item.geometric));
  const dynamicSet = new Set(legacyItems.map((item) => item.dynamic));
  const structuralSet = new Set(legacyItems.map((item) => item.structure.structuralFingerprint));
  const grammarSet = new Set(legacyItems.map((item) => item.structure.grammarFingerprint));
  const streamOrdinals = new Map();
  const rejections = emptyRejections();
  let attempts = 0;
  for (let order = legacyItems.length + 1; order <= 2000; order += 1) {
    const gardenOrder = Math.floor((order - 1) / 100) + 1;
    const band = campaignBand(order);
    const size = campaignSize(order);
    const stream = `${gardenOrder}|${band}|${size}`;
    let ordinal = streamOrdinals.get(stream) ?? 0;
    let selected = null;
    const streamRejections = emptyRejections();
    const streamDiagnostics = { maxMeaningful: 0, maxBloomStops: 0, maxDependency: 0, minForcedRatio: 1 };
    while (ordinal < 100_000 && !selected) {
      const seed = deriveSeed("mazebloom-candidate", GENERATION_NAMESPACE_VERSION, GENERATOR_VERSION, gardenOrder, band, size, ordinal);
      const candidate = candidateFor(seed, size, band);
      attempts += 1;
      if (!candidate.item) {
        rejections[candidate.rejection] += 1;
        streamRejections[candidate.rejection] += 1;
        if (candidate.diagnostics) {
          streamDiagnostics.maxMeaningful = Math.max(streamDiagnostics.maxMeaningful, candidate.diagnostics.meaningful);
          streamDiagnostics.maxBloomStops = Math.max(streamDiagnostics.maxBloomStops, candidate.diagnostics.minimumBloomStops);
          streamDiagnostics.maxDependency = Math.max(streamDiagnostics.maxDependency, candidate.diagnostics.maximumDependency);
          streamDiagnostics.minForcedRatio = Math.min(streamDiagnostics.minForcedRatio, candidate.diagnostics.forcedRatio);
        }
        ordinal += 1;
        continue;
      }
      const rejection = uniquenessRejection(candidate.item, accepted, geometricSet, dynamicSet, structuralSet, grammarSet);
      if (rejection) {
        rejections[rejection] += 1;
        streamRejections[rejection] += 1;
        ordinal += 1;
        continue;
      }
      const idWidth = order <= 100 ? 3 : 4;
      selected = { ...candidate.item, id: `campaign-${String(order).padStart(idWidth, "0")}`, campaignOrder: order, ordinal };
      register(selected, accepted, geometricSet, dynamicSet, structuralSet, grammarSet);
      ordinal += 1;
    }
    streamOrdinals.set(stream, ordinal);
    if (!selected) throw new Error(`Generation stream exhausted: ${stream}; remaining campaign order ${order}; streamRejections=${JSON.stringify(streamRejections)}; diagnostics=${JSON.stringify(streamDiagnostics)}; allRejections=${JSON.stringify(rejections)}`);
    if (order % 100 === 0) console.error(`Generated Garden ${gardenOrder}/20 (${order}/2000)`);
  }
  return { accepted, attempts, rejections, streamOrdinals: Object.fromEntries([...streamOrdinals].sort()) };
}

function generateDaily(campaignItems, target = 120) {
  const accepted = [];
  const registry = [...campaignItems];
  const geometricSet = new Set(registry.map((item) => item.geometric));
  const dynamicSet = new Set(registry.map((item) => item.dynamic));
  const structuralSet = new Set(registry.map((item) => item.structure.structuralFingerprint));
  const grammarSet = new Set(registry.map((item) => item.structure.grammarFingerprint));
  const rejections = emptyRejections();
  let ordinal = 0;
  while (accepted.length < target && ordinal < 500_000) {
    const band = ["EASY", "NORMAL", "HARD", "EXPERT"][accepted.length % 4];
    const size = accepted.length % 3 === 0 ? 5 : 6;
    const seed = deriveSeed("mazebloom-daily", DAILY_POOL_VERSION, GENERATOR_VERSION, band, size, ordinal);
    const candidate = candidateFor(seed, size, band);
    ordinal += 1;
    if (!candidate.item) { rejections[candidate.rejection] += 1; continue; }
    const rejection = uniquenessRejection(candidate.item, registry, geometricSet, dynamicSet, structuralSet, grammarSet);
    if (rejection) { rejections[rejection] += 1; continue; }
    const item = { ...candidate.item, id: `daily-${String(accepted.length + 1).padStart(4, "0")}`, campaignOrder: 0, ordinal: ordinal - 1 };
    accepted.push(item);
    register(item, registry, geometricSet, dynamicSet, structuralSet, grammarSet);
  }
  if (accepted.length < 120) throw new Error(`Daily generation budget exhausted at ${accepted.length}/120`);
  return { accepted, attempts: ordinal, rejections };
}

function generateProgressive(campaignItems, dailyItems, target = 100) {
  const accepted = [];
  const registry = [...campaignItems, ...dailyItems];
  const geometricSet = new Set(registry.map((item) => item.geometric));
  const dynamicSet = new Set(registry.map((item) => item.dynamic));
  const structuralSet = new Set(registry.map((item) => item.structure.structuralFingerprint));
  const grammarSet = new Set(registry.map((item) => item.structure.grammarFingerprint));
  const streamOrdinals = new Map();
  const rejections = emptyRejections();
  let attempts = 0;
  for (let progressiveOrder = 1; progressiveOrder <= target; progressiveOrder += 1) {
    const band = repeatingBand(progressiveOrder);
    const size = repeatingSize(progressiveOrder);
    const stream = `${band}|${size}`;
    let ordinal = streamOrdinals.get(stream) ?? 0;
    let selected = null;
    while (ordinal < 100_000 && !selected) {
      const seed = deriveSeed("mazebloom-progressive", PROGRESSIVE_POOL_VERSION, GENERATOR_VERSION, band, size, ordinal);
      const candidate = candidateFor(seed, size, band);
      attempts += 1;
      if (!candidate.item) {
        rejections[candidate.rejection] += 1;
        ordinal += 1;
        continue;
      }
      const rejection = uniquenessRejection(candidate.item, registry, geometricSet, dynamicSet, structuralSet, grammarSet);
      if (rejection) {
        rejections[rejection] += 1;
        ordinal += 1;
        continue;
      }
      selected = {
        ...candidate.item,
        id: `progressive-${String(progressiveOrder).padStart(4, "0")}`,
        campaignOrder: 0,
        progressiveOrder,
        ordinal,
      };
      accepted.push(selected);
      register(selected, registry, geometricSet, dynamicSet, structuralSet, grammarSet);
      ordinal += 1;
    }
    streamOrdinals.set(stream, ordinal);
    if (!selected) throw new Error(`Progressive generation stream exhausted: ${stream}; order=${progressiveOrder}; rejections=${JSON.stringify(rejections)}`);
  }
  return { accepted, attempts, rejections, streamOrdinals: Object.fromEntries([...streamOrdinals].sort()) };
}

function definitionHashFor(record) {
  return hashFields(
    "definition-v2", record.schemaVersion, record.contentVersion, record.id, record.campaignOrder,
    record.gardenId ?? (record.campaignOrder ? `garden-${String(Math.floor((record.campaignOrder - 1) / 100) + 1).padStart(2, "0")}` : "daily"),
    record.chapterId ?? (record.campaignOrder ? `garden-${String(Math.floor((record.campaignOrder - 1) / 100) + 1).padStart(2, "0")}/chapter-${String(record.chapter).padStart(2, "0")}` : "daily"),
    record.chapterOrderWithinGarden ?? record.chapter, record.width, record.height,
    record.stones.join(","), record.start, record.buds.join(","), record.generatorVersion, record.generatorSeed,
  );
}

function contentRecord(item) {
  if (item.legacy) return item.sourceLine;
  const order = item.campaignOrder;
  const progressive = item.id.startsWith("progressive-");
  const gardenOrder = order > 0 ? Math.floor((order - 1) / 100) + 1 : 0;
  const chapter = order > 0 ? Math.floor(((order - 1) % 100) / 20) + 1 : 0;
  const gardenId = order > 0 ? `garden-${String(gardenOrder).padStart(2, "0")}` : progressive ? "progressive" : "daily";
  const chapterId = order > 0 ? `${gardenId}/chapter-${String(chapter).padStart(2, "0")}` : progressive ? "progressive" : "daily";
  const record = {
    id: item.id,
    schemaVersion: 2,
    contentVersion: 2,
    rulesVersion: 1,
    width: item.level.size,
    height: item.level.size,
    stones: cells(item.level.walls, item.level.size ** 2),
    start: item.level.start,
    buds: cells(item.level.buds, item.level.size ** 2),
    chapter,
    campaignOrder: order,
    progressiveOrder: item.progressiveOrder,
    gardenId,
    chapterId,
    chapterOrderWithinGarden: chapter,
    generatorVersion: GENERATOR_VERSION,
    generatorSeed: item.seed.toString(16).padStart(16, "0"),
    solverVersion: 2,
    certificationProfileVersion: CERTIFICATION_PROFILE_VERSION,
    fingerprintVersion: FINGERPRINT_VERSION,
    uniquenessProfileVersion: UNIQUENESS_PROFILE_VERSION,
    difficulty: item.band,
    optimalMoves: item.solution.depth,
    canonicalReplay: item.solution.replay.map((direction) => DIRECTIONS[direction][0]),
    optimalSolutionCount: String(item.solution.optimalCount ?? 1),
    optimalCountOverflow: Boolean(item.solution.optimalOverflow),
    nearOptimalCountStatus: "NOT_COMPUTED_BUDGET",
    geometricFingerprint: sha256(item.geometric),
    dynamicFingerprint: item.dynamic,
    solutionGrammarFingerprint: item.structure.grammarFingerprint,
    structuralFingerprint: item.structure.structuralFingerprint,
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
  record.definitionHash = definitionHashFor(record);
  record.solutionHash = hashFields(record.definitionHash, record.rulesVersion, record.optimalMoves, record.canonicalReplay.join(","));
  record.publishedCertificateHash = hashFields(
    record.definitionHash, record.solutionHash, record.solverVersion, record.certificationProfileVersion,
    record.fingerprintVersion, record.uniquenessProfileVersion, record.dynamicFingerprint,
    record.solutionGrammarFingerprint, record.structuralFingerprint, record.optimalSolutionCount,
    record.optimalCountOverflow, record.nearOptimalCountStatus, JSON.stringify(record.metrics),
  );
  return JSON.stringify({ ...record, certificationChecksum: sha256(JSON.stringify(record)) });
}

function summaryFor(record) {
  const gardenOrder = Math.floor((record.campaignOrder - 1) / 100) + 1;
  const chapter = Math.floor(((record.campaignOrder - 1) % 100) / 20) + 1;
  const gardenId = record.gardenId ?? `garden-${String(gardenOrder).padStart(2, "0")}`;
  return {
    id: record.id,
    campaignOrder: record.campaignOrder,
    gardenId,
    chapterId: record.chapterId ?? `${gardenId}/chapter-${String(chapter).padStart(2, "0")}`,
    chapterOrderWithinGarden: chapter,
    levelOrderWithinChapter: (record.campaignOrder - 1) % 20 + 1,
    difficulty: record.difficulty,
    boardSize: record.width,
    optimalMoves: record.optimalMoves,
    definitionChecksum: record.definitionHash ?? definitionHashFor(record),
  };
}

function auditPairs(items, startLeft = 0, startRight = 1) {
  let pairComparisons = 0;
  let alignmentComparisons = 0;
  const collisions = [];
  for (let left = startLeft; left < items.length; left += 1) {
    const minimumRight = left === startLeft ? Math.max(left + 1, startRight) : left + 1;
    for (let right = minimumRight; right < items.length; right += 1) {
      pairComparisons += 1;
      if (items[left].level.size === items[right].level.size) alignmentComparisons += 8;
      const result = compareSimilarity(items[left], items[right]);
      if (result.tier === "NONE") continue;
      collisions.push({
        leftId: items[left].id, rightId: items[right].id,
        leftPriorityKey: items[left].campaignOrder || 1_000_000 + left,
        rightPriorityKey: items[right].campaignOrder || 1_000_000 + right,
        collisionTier: result.tier, transformId: result.transform ?? null,
        maskDistance: result.distance ?? null, stoneDistance: result.stoneDistance ?? null,
        budDistance: result.budDistance ?? null, startDistance: result.startDistance ?? null,
        matchedPatterns: [result.directionMatch && "direction", result.pickupMatch && "pickup", result.stopMatch && "stop"].filter(Boolean),
        reasonCode: result.reason,
      });
    }
  }
  collisions.sort((left, right) => left.leftPriorityKey - right.leftPriorityKey || left.rightPriorityKey - right.rightPriorityKey || left.collisionTier.localeCompare(right.collisionTier) || String(left.transformId).localeCompare(String(right.transformId)) || left.reasonCode.localeCompare(right.reasonCode));
  return { pairComparisons, alignmentComparisons, collisions };
}

function distribution(items, field) {
  return Object.fromEntries([...new Set(items.map(field))].sort().map((value) => [value, items.filter((item) => field(item) === value).length]));
}

export function generateAll() {
  const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
  const contentDirectory = resolve(root, "app/src/main/assets/content");
  const legacyPath = resolve(contentDirectory, "campaign.jsonl");
  if (!existsSync(legacyPath)) throw new Error("Missing historical Campaign source: content/campaign.jsonl");
  const legacyText = readFileSync(legacyPath, "utf8");
  const historicalLines = legacyText.split("\n").filter(Boolean);
  if (historicalLines.length !== 100) throw new Error(`Historical Campaign source must contain exactly 100 records; found ${historicalLines.length}`);
  const legacyLines = historicalLines.slice(0, 5);
  const legacyRecords = legacyLines.map(JSON.parse);
  if (legacyRecords.map((record) => record.campaignOrder).join(",") !== "1,2,3,4,5") throw new Error("Tutorial prefix order drift");
  legacyRecords.forEach((record, index) => {
    const payload = legacyLines[index].replace(/,"certificationChecksum":"[0-9a-f]{64}"}$/, "}");
    if (sha256(payload) !== record.certificationChecksum) throw new Error(`Legacy source checksum drift: ${record.id}`);
  });

  const legacyItems = legacyRecords.map((record, index) => legacyItem(record, legacyLines[index]));
  const legacyDefinitionRoot = hashFields(...legacyRecords.map(definitionHashFor));
  const legacySolutionRoot = hashFields(...legacyRecords.map((record) => hashFields(definitionHashFor(record), record.rulesVersion, record.optimalMoves, record.canonicalReplay.join(","))));
  const legacyCertificateRoot = hashFields(...legacyRecords.map((record) => record.certificationChecksum));
  const legacyPrefixCompatibilityRoot = hashFields(legacyDefinitionRoot, legacySolutionRoot, legacyCertificateRoot, ...legacyRecords.map((record) => `${record.id}|${record.campaignOrder}|${record.difficulty}|${record.generatorVersion}|${record.generatorSeed}`));

  const campaign = generateExtension(legacyItems);
  const campaignLines = campaign.accepted.map(contentRecord);
  const campaignRecords = campaignLines.map(JSON.parse);
  const campaignAudit = auditPairs(campaign.accepted);
  const illegalCampaignCollisions = campaignAudit.collisions.filter((collision) => collision.rightPriorityKey > 5);
  if (illegalCampaignCollisions.length) throw new Error(`Strict campaign uniqueness failed: ${JSON.stringify(illegalCampaignCollisions[0])}`);
  const grandfatheredLegacyCollisions = campaignAudit.collisions.map((collision) => ({ ...collision, reasonCode: `GRANDFATHERED_LEGACY_COLLISION:${collision.reasonCode}` }));

  const campaignDirectory = resolve(contentDirectory, "campaign");
  if (existsSync(campaignDirectory)) rmSync(campaignDirectory, { recursive: true, force: true });
  mkdirSync(campaignDirectory, { recursive: true });
  const chapters = [];
  const shardRoots = [];
  const auditShardRoots = [];
  for (let garden = 1; garden <= 20; garden += 1) {
    const gardenId = `garden-${String(garden).padStart(2, "0")}`;
    const gardenDirectory = resolve(campaignDirectory, gardenId);
    mkdirSync(gardenDirectory, { recursive: true });
    for (let chapter = 1; chapter <= 5; chapter += 1) {
      const first = (garden - 1) * 100 + (chapter - 1) * 20;
      const records = campaignRecords.slice(first, first + 20);
      const chapterId = `${gardenId}/chapter-${String(chapter).padStart(2, "0")}`;
      const shard = { schemaVersion: 2, campaignCatalogVersion: 3, gardenId, chapterId, firstCampaignOrder: first + 1, lastCampaignOrder: first + 20, levels: records };
      const shardText = `${JSON.stringify(shard)}\n`;
      const relativePath = `content/campaign/${gardenId}/chapter-${String(chapter).padStart(2, "0")}.json`;
      writeFileSync(resolve(gardenDirectory, `chapter-${String(chapter).padStart(2, "0")}.json`), shardText, "utf8");
      const shardRoot = sha256(shardText);
      const auditRoot = hashFields(...campaign.accepted.slice(first, first + 20).map((item) => `${item.dynamic}|${item.structure.grammarFingerprint}|${item.structure.structuralFingerprint}|${JSON.stringify(item.metrics, (_, value) => value instanceof Set ? [...value].sort((a, b) => a - b) : value)}`));
      shardRoots.push(shardRoot);
      auditShardRoots.push(auditRoot);
      chapters.push({ id: chapterId, gardenId, orderWithinGarden: chapter, firstCampaignOrder: first + 1, levelCount: 20, shardPath: relativePath, shardSha256: shardRoot, auditShardRoot: auditRoot, levels: records.map(summaryFor) });
    }
  }

  const pacingScheduleHash = hashFields(...PACING.flat());
  const campaignContentRoot = hashFields("campaign-content-v2", legacyPrefixCompatibilityRoot, ...shardRoots, pacingScheduleHash);
  const campaignUniquenessRoot = hashFields("campaign-uniqueness-v2", UNIQUENESS_PROFILE_VERSION, campaignAudit.pairComparisons, campaignAudit.alignmentComparisons, ...campaign.accepted.map((item) => `${item.id}|${item.geometric}|${item.dynamic}|${item.structure.grammarFingerprint}|${item.structure.structuralFingerprint}`), JSON.stringify(grandfatheredLegacyCollisions));
  const campaignAuditRoot = hashFields("campaign-audit-v2", ...auditShardRoots, campaignUniquenessRoot);
  const campaignRoot = hashFields(campaignContentRoot, campaignAuditRoot);

  const daily = generateDaily(campaign.accepted, 120);
  const dailyLines = daily.accepted.map(contentRecord);
  const dailyText = `${dailyLines.join("\n")}\n`;
  writeFileSync(resolve(contentDirectory, "daily.jsonl"), dailyText, "utf8");
  const dailyContentRoot = hashFields(...dailyLines);
  const dailyAuditRoot = hashFields(...daily.accepted.map((item) => `${item.dynamic}|${item.structure.grammarFingerprint}|${item.structure.structuralFingerprint}`));

  let crossPairs = 0;
  let crossAlignments = 0;
  const globalCollisions = [...grandfatheredLegacyCollisions];
  for (let left = 0; left < campaign.accepted.length; left += 1) {
    for (let right = 0; right < daily.accepted.length; right += 1) {
      crossPairs += 1;
      if (campaign.accepted[left].level.size === daily.accepted[right].level.size) crossAlignments += 8;
      const result = compareSimilarity(campaign.accepted[left], daily.accepted[right]);
      if (result.tier !== "NONE") throw new Error(`Campaign/Daily collision: ${campaign.accepted[left].id}/${daily.accepted[right].id}`);
    }
  }
  const dailyAudit = auditPairs(daily.accepted);
  if (dailyAudit.collisions.length) throw new Error(`Daily collision: ${JSON.stringify(dailyAudit.collisions[0])}`);
  const progressive = generateProgressive(campaign.accepted, daily.accepted, 100);
  const progressiveLines = progressive.accepted.map(contentRecord);
  const progressiveText = `${progressiveLines.join("\n")}\n`;
  writeFileSync(resolve(contentDirectory, "progressive.jsonl"), progressiveText, "utf8");
  const progressiveContentRoot = hashFields(...progressiveLines);
  const progressiveAuditRoot = hashFields(...progressive.accepted.map((item) => `${item.dynamic}|${item.structure.grammarFingerprint}|${item.structure.structuralFingerprint}`));
  const progressiveAudit = auditPairs(progressive.accepted);
  if (progressiveAudit.collisions.length) throw new Error(`Progressive collision: ${JSON.stringify(progressiveAudit.collisions[0])}`);
  let progressiveCrossPairs = 0;
  let progressiveCrossAlignments = 0;
  for (const existing of [...campaign.accepted, ...daily.accepted]) {
    for (const candidate of progressive.accepted) {
      progressiveCrossPairs += 1;
      if (existing.level.size === candidate.level.size) progressiveCrossAlignments += 8;
      const result = compareSimilarity(existing, candidate);
      if (result.tier !== "NONE") throw new Error(`Existing/Progressive collision: ${existing.id}/${candidate.id}`);
    }
  }
  const globalPairComparisons = campaignAudit.pairComparisons + crossPairs + dailyAudit.pairComparisons + progressiveCrossPairs + progressiveAudit.pairComparisons;
  const globalAlignmentComparisons = campaignAudit.alignmentComparisons + crossAlignments + dailyAudit.alignmentComparisons + progressiveCrossAlignments + progressiveAudit.alignmentComparisons;
  const progressiveUniquenessRoot = hashFields(
    "progressive-uniqueness-v1", PROGRESSIVE_POOL_VERSION, progressiveCrossPairs,
    progressiveAudit.pairComparisons, progressiveAudit.alignmentComparisons,
    ...progressive.accepted.map((item) => `${item.id}|${item.geometric}|${item.dynamic}|${item.structure.grammarFingerprint}|${item.structure.structuralFingerprint}`),
  );
  const globalUniquenessRoot = hashFields(
    "global-uniqueness-v3", globalPairComparisons, globalAlignmentComparisons,
    campaignUniquenessRoot, progressiveUniquenessRoot,
    ...daily.accepted.map((item) => `${item.id}|${item.geometric}|${item.dynamic}|${item.structure.grammarFingerprint}|${item.structure.structuralFingerprint}`),
    ...progressive.accepted.map((item) => `${item.id}|${item.geometric}|${item.dynamic}|${item.structure.grammarFingerprint}|${item.structure.structuralFingerprint}`),
    JSON.stringify(globalCollisions),
  );
  const releaseContentRoot = hashFields(
    campaignRoot, dailyContentRoot, dailyAuditRoot,
    progressiveContentRoot, progressiveAuditRoot, progressiveUniquenessRoot, globalUniquenessRoot,
  );

  const gardens = Array.from({ length: 20 }, (_, index) => {
    const order = index + 1;
    const id = `garden-${String(order).padStart(2, "0")}`;
    return { id, order, nameKey: `garden_${String(order).padStart(2, "0")}`, chapters: chapters.filter((chapter) => chapter.gardenId === id) };
  });
  const catalog = {
    schemaVersion: 2, campaignCatalogVersion: 3, campaignCount: 2000, gardenCount: 20,
    chapterCount: 100, levelsPerChapter: 20, parentCampaignRoot: null, initializationMode: "INITIAL_CATALOG",
    legacyDefinitionRoot, legacySolutionRoot, legacyCertificateRoot, legacyPrefixCompatibilityRoot,
    campaignContentRoot, campaignUniquenessRoot, campaignAuditRoot, campaignRoot,
    pacingScheduleHash, campaignPairComparisons: campaignAudit.pairComparisons,
    campaignAlignmentComparisons: campaignAudit.alignmentComparisons,
    grandfatheredLegacyCollisionCount: grandfatheredLegacyCollisions.length,
    gardens,
  };
  writeFileSync(resolve(campaignDirectory, "campaign-manifest.json"), `${JSON.stringify(catalog)}\n`, "utf8");

  const manifest = {
    schemaVersion: 2, contentVersion: 2, rulesVersion: 1, solverVersion: 2,
    certificationProfileVersion: CERTIFICATION_PROFILE_VERSION, fingerprintVersion: FINGERPRINT_VERSION,
    uniquenessProfileVersion: UNIQUENESS_PROFILE_VERSION, campaignCatalogVersion: 3,
    campaignCount: 2000, gardenCount: 20, chapterCount: 100,
    dailyPoolVersion: DAILY_POOL_VERSION, dailyCount: daily.accepted.length,
    progressivePoolVersion: PROGRESSIVE_POOL_VERSION, progressiveCount: progressive.accepted.length,
    legacyDefinitionRoot, legacySolutionRoot, legacyCertificateRoot, legacyPrefixCompatibilityRoot,
    campaignContentRoot, campaignUniquenessRoot, campaignAuditRoot, campaignRoot,
    dailyContentRoot, dailyAuditRoot, progressiveContentRoot, progressiveAuditRoot,
    progressiveUniquenessRoot, globalUniquenessRoot, releaseContentRoot,
    campaignPairComparisons: campaignAudit.pairComparisons,
    campaignAlignmentComparisons: campaignAudit.alignmentComparisons,
    globalPairComparisons, globalAlignmentComparisons,
    humanReviewed: false,
  };
  const all = [...campaign.accepted, ...daily.accepted, ...progressive.accepted];
  writeFileSync(resolve(contentDirectory, "manifest.json"), `${JSON.stringify(manifest, null, 2)}\n`, "utf8");
  writeFileSync(resolve(contentDirectory, "certification-stamp.json"), `${JSON.stringify({ releaseContentRoot, rulesVersion: 1, solverVersion: 2, certificationProfileVersion: CERTIFICATION_PROFILE_VERSION, fingerprintVersion: FINGERPRINT_VERSION, uniquenessProfileVersion: UNIQUENESS_PROFILE_VERSION, status: "PASS" }, null, 2)}\n`, "utf8");
  const endlessBaselineRoot = hashFields(
    "endless-baseline-v1", campaignRoot, dailyContentRoot, dailyAuditRoot,
    progressiveContentRoot, progressiveAuditRoot, progressiveUniquenessRoot, globalUniquenessRoot,
    CERTIFICATION_PROFILE_VERSION, FINGERPRINT_VERSION, UNIQUENESS_PROFILE_VERSION,
    all.length, globalPairComparisons, globalAlignmentComparisons, "zero-new-collisions",
  );
  writeFileSync(resolve(contentDirectory, "endless-baseline.json"), `${JSON.stringify({
    schemaVersion: 1, recordCount: all.length, campaignRoot, dailyContentRoot, dailyAuditRoot,
    progressiveContentRoot, progressiveAuditRoot, progressiveUniquenessRoot, globalUniquenessRoot,
    certificationProfileVersion: CERTIFICATION_PROFILE_VERSION, fingerprintVersion: FINGERPRINT_VERSION,
    uniquenessProfileVersion: UNIQUENESS_PROFILE_VERSION,
    expectedPairComparisons: String(globalPairComparisons), performedAlignmentComparisons: String(globalAlignmentComparisons),
    collisionResult: "zero-new-collisions", endlessBaselineRoot,
  }, null, 2)}\n`, "utf8");

  const reportDirectory = resolve(root, "reports/content");
  mkdirSync(reportDirectory, { recursive: true });
  const report = {
    reportVersion: 2, automatedCertification: "PASS", humanReview: "PENDING",
    counts: { campaign: 2000, gardens: 20, chapters: 100, daily: daily.accepted.length, progressive: progressive.accepted.length },
    distributions: {
      campaignDifficulty: distribution(campaign.accepted, (item) => item.band),
      extensionDifficulty: distribution(campaign.accepted.slice(100), (item) => item.band),
      mutableCampaignDifficulty: distribution(campaign.accepted.slice(5), (item) => item.band),
      campaignBoardSize: distribution(campaign.accepted, (item) => `${item.level.size}x${item.level.size}`),
      extensionBoardSize: distribution(campaign.accepted.slice(100), (item) => `${item.level.size}x${item.level.size}`),
      mutableCampaignBoardSize: distribution(campaign.accepted.slice(5), (item) => `${item.level.size}x${item.level.size}`),
      dailyDifficulty: distribution(daily.accepted, (item) => item.band),
      dailyBoardSize: distribution(daily.accepted, (item) => `${item.level.size}x${item.level.size}`),
      progressiveDifficulty: distribution(progressive.accepted, (item) => item.band),
      progressiveBoardSize: distribution(progressive.accepted, (item) => `${item.level.size}x${item.level.size}`),
    },
    comparisons: {
      campaignPairsExpected: 1_999_000, campaignPairsPerformed: campaignAudit.pairComparisons,
      campaignAlignmentsPerformed: campaignAudit.alignmentComparisons,
      campaignDailyPairsPerformed: crossPairs, dailyPairsPerformed: dailyAudit.pairComparisons,
      existingProgressivePairsPerformed: progressiveCrossPairs, progressivePairsPerformed: progressiveAudit.pairComparisons,
      globalPairsPerformed: globalPairComparisons, globalAlignmentsPerformed: globalAlignmentComparisons,
      illegalNewCollisionCount: 0, grandfatheredLegacyCollisionCount: grandfatheredLegacyCollisions.length,
    },
    roots: manifest,
    attempts: { campaignExtension: campaign.attempts, daily: daily.attempts, progressive: progressive.attempts },
    rejections: { campaignExtension: campaign.rejections, daily: daily.rejections, progressive: progressive.rejections },
    streamOrdinals: { campaign: campaign.streamOrdinals, progressive: progressive.streamOrdinals },
    authoritativeMaxima: {
      shortestDiscoveredStates: Math.max(...all.map((item) => item.solution.discovered ?? 0)),
      auditReachableStates: Math.max(...all.map((item) => item.metrics.reachable)),
    },
    diagnosticTiming: { status: "RECORDED_BY_VERIFICATION_RUN", canonical: false },
  };
  writeFileSync(resolve(reportDirectory, "certification-summary.json"), `${JSON.stringify(report, null, 2)}\n`, "utf8");
  writeFileSync(resolve(reportDirectory, "collision-audit.json"), `${JSON.stringify({ profileVersion: UNIQUENESS_PROFILE_VERSION, grandfatheredLegacyCollisions, illegalNewCollisions: [] }, null, 2)}\n`, "utf8");

  const sampledIds = new Set(legacyItems.map((item) => item.id));
  for (let garden = 1; garden <= 20; garden += 1) {
    for (const band of ["EASY", "NORMAL", "HARD", "EXPERT", "MASTER"]) {
      campaign.accepted.filter((item) => item.campaignOrder > 5 && Math.floor((item.campaignOrder - 1) / 100) + 1 === garden && item.band === band)
        .sort((left, right) => hashFields("review-v1", left.id).localeCompare(hashFields("review-v1", right.id))).slice(0, 2)
        .forEach((item) => sampledIds.add(item.id));
    }
  }
  campaign.accepted.filter((item) => item.campaignOrder % 20 === 0).forEach((item) => sampledIds.add(item.id));
  daily.accepted.forEach((item) => sampledIds.add(item.id));
  progressive.accepted.forEach((item) => sampledIds.add(item.id));
  const reviewHeader = "levelId,predictedBand,optimalMoves,meaningfulDecisions,forcedMoveRatio,minimumBloomStops,automatedCertified,humanApproved,reviewer,attempts,solveTimeSeconds,undos,restarts,hints,perceivedDifficulty,nearestNeighbors,duplicateConcern,decision,notes";
  const reviewRows = all.filter((item) => sampledIds.has(item.id)).map((item) => [
    item.id, item.band, item.solution.depth, item.metrics.meaningful, item.metrics.forcedRatio.toFixed(6),
    item.metrics.minimumBloomStops, "true", "", "", "", "", "", "", "", "", "", "", "", "",
  ].join(","));
  writeFileSync(resolve(reportDirectory, "human-review-worksheet.csv"), `${reviewHeader}\n${reviewRows.join("\n")}\n`, "utf8");
  console.log(JSON.stringify(manifest));
  return manifest;
}

export {
  audit, candidateFor, campaignBand, campaignSize, certifyRawCandidate, compareSimilarity, difficultyRange,
  dynamicFingerprint, geometricEncoding, hashFields, legacyItem, meetsProfile, prepareItem, rawCandidateFor, register,
  replayStructure, solve, transition, uniquenessRejection,
};

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) generateAll();

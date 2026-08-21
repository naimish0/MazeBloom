#!/usr/bin/env node
import { readFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { audit, generateAll, geometricEncoding, solve, transition } from "./generate-content.mjs";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const contentDirectory = resolve(root, "app/src/main/assets/content");
const records = ["campaign.jsonl", "daily.jsonl"].flatMap((name) =>
  readFileSync(resolve(contentDirectory, name), "utf8").split("\n").filter(Boolean).map(JSON.parse));
const bit = (cell) => 1n << BigInt(cell);
const toMask = (values) => values.reduce((mask, cell) => mask | bit(cell), 0n);
const internal = (record) => ({ size: record.width, walls: toMask(record.stones), start: record.start, buds: toMask(record.buds) });
const command = process.argv[2] ?? "help";
const id = process.argv[3] ?? "campaign-001";
const record = records.find((value) => value.id === id);
const requireLevel = () => {
  if (!record) throw new Error(`Unknown level ID: ${id}`);
  return internal(record);
};
const render = (level, state = { seed: level.start, bloom: 0n, buds: level.buds }) => {
  const rows = [];
  rows.push(`   ${Array.from({ length: level.size }, (_, index) => index).join(" ")}`);
  for (let y = 0; y < level.size; y += 1) {
    const cells = [];
    for (let x = 0; x < level.size; x += 1) {
      const cell = y * level.size + x;
      cells.push(cell === state.seed ? "S" : level.walls & bit(cell) ? "#" : state.bloom & bit(cell) ? "B" : state.buds & bit(cell) ? "*" : ".");
    }
    rows.push(`${y}: ${cells.join(" ")}`);
  }
  return rows.join("\n");
};

switch (command) {
  case "generate":
  case "generate-campaign":
  case "generate-daily-pool":
    generateAll();
    break;
  case "verify":
  case "certify":
  case "certify-campaign": {
    const result = spawnSync(process.execPath, [resolve(root, "tools/verify-content.mjs")], { stdio: "inherit" });
    process.exitCode = result.status ?? 1;
    break;
  }
  case "solve": {
    const solution = solve(requireLevel());
    console.log(JSON.stringify({ id, status: solution ? "SOLVED" : "UNSOLVABLE", optimalMoves: solution?.depth, canonicalReplay: solution?.replay.map((index) => ["UP", "RIGHT", "DOWN", "LEFT"][index]), discoveredStates: solution?.discovered }, null, 2));
    break;
  }
  case "analyze": {
    const level = requireLevel();
    const solution = solve(level);
    console.log(JSON.stringify({ id, ...audit(level, solution) }, null, 2));
    break;
  }
  case "render":
    console.log(render(requireLevel()));
    break;
  case "replay": {
    const level = requireLevel();
    const solution = solve(level);
    let state = { seed: level.start, bloom: 0n, buds: level.buds, status: "ACTIVE" };
    console.log(`0\n${render(level, state)}`);
    solution.replay.forEach((direction, index) => {
      state = transition(level, state, direction).state;
      console.log(`\n${index + 1}. ${["UP", "RIGHT", "DOWN", "LEFT"][direction]}\n${render(level, state)}`);
    });
    break;
  }
  case "dedupe": {
    const groups = new Map();
    records.forEach((value) => {
      const fingerprint = geometricEncoding(internal(value));
      groups.set(fingerprint, [...(groups.get(fingerprint) ?? []), value.id]);
    });
    const duplicates = [...groups.values()].filter((group) => group.length > 1);
    console.log(JSON.stringify({ levels: records.length, exactGeometricDuplicateGroups: duplicates }, null, 2));
    if (duplicates.length) process.exitCode = 1;
    break;
  }
  case "help":
    console.log("MazeBloom content CLI: generate | solve <id> | analyze <id> | certify | dedupe | replay <id> | render <id> | verify");
    break;
  default:
    throw new Error(`Unknown command: ${command}`);
}

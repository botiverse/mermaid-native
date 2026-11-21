import { readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseManifest } from './extract-corpus.mjs';

export const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');

/** `--key value` parser with defaults; unknown keys are rejected. */
export function parseArgs(argv, defaults) {
  const out = { ...defaults };
  for (let i = 2; i < argv.length; i++) {
    const key = argv[i].replace(/^--/, '');
    if (!(key in defaults)) throw new Error(`unknown argument ${argv[i]}`);
    const next = argv[i + 1];
    if (typeof defaults[key] === 'boolean') out[key] = true;
    else { out[key] = next; i++; }
  }
  return out;
}

/** Manifest rows with fixture source and parsed options; `only` is a substring filter on family/case. */
export function loadCorpus(corpusDir, only = '') {
  const rows = parseManifest(readFileSync(join(corpusDir, 'manifest.tsv'), 'utf8'));
  return rows
    .filter((r) => !only || `${r.family}/${r.case}`.includes(only))
    .map((r) => ({
      ...r,
      source: readFileSync(join(corpusDir, r.family, `${r.case}.mmd`), 'utf8'),
      options: JSON.parse(r.options_json),
    }));
}

export function readJson(path, fallback = undefined) {
  try { return JSON.parse(readFileSync(path, 'utf8')); } catch (e) { if (fallback !== undefined) return fallback; throw e; }
}

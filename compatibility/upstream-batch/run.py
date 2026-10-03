#!/usr/bin/env python3
"""Verify/import whole upstream modules and run their unchanged reference tests.

This runner deliberately cannot promote Native coverage: all imported assertion
occurrences remain interface_not_integrated until a production Native adapter
executes them. It never labels a reference result as a Native result.
"""
import argparse
import collections
import hashlib
import json
import os
from pathlib import Path
import subprocess

HERE = Path(__file__).resolve().parent
MANIFEST = json.loads((HERE / 'manifest.json').read_text())


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify(upstream):
    # Release archives lack .git; all imported sources are still hash checked.
    if upstream and (upstream / '.git').exists():
        revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=upstream, text=True).strip()
        if revision != MANIFEST['revision']:
            raise ValueError(f'Upstream revision mismatch: {revision}')
    keys = [(a['file'], a['test'], a['occurrence']) for a in MANIFEST['assertions']]
    if len(keys) != len(set(keys)):
        raise ValueError('Duplicate assertion identity')
    for name, expected in MANIFEST['files'].items():
        paths = [HERE / 'originals' / name]
        if upstream:
            paths.append(upstream / name)
        for path in paths:
            if digest(path) != expected:
                raise ValueError(f'Pinned source changed: {path}')
    if collections.Counter(a['group'] for a in MANIFEST['assertions']) != MANIFEST['groups']:
        raise ValueError('Manifest group counts do not reconcile')


def run_group(upstream, output, group, timeout):
    target = [a for a in MANIFEST['assertions'] if a['group'] == group]
    files = sorted({a['file'] for a in target})
    script_files = [f for f in files if f == 'packages/mermaid/scripts/docs.spec.ts']
    work = upstream / '.native-batch-import-audit' / group
    work.mkdir(parents=True, exist_ok=True)
    dest = output / group
    dest.mkdir(parents=True, exist_ok=True)
    # Load imported bytes at their original module IDs so relative imports,
    # snapshots, inline tests and vi.mock paths retain upstream semantics.
    imported = {str(upstream / f): str(HERE / 'originals' / f) for f in files}
    config = """import {defineConfig} from 'vitest/config';
import jison from '../../.vite/jisonPlugin.js';
import jsonSchemaPlugin from '../../.vite/jsonSchemaPlugin.js';
import {readFileSync} from 'node:fs';
import {resolve} from 'node:path';
import ts from 'typescript';
const imported = IMPORTED;
export default defineConfig({
 plugins:[{name:'pinned-imported-tests',enforce:'pre',load(id){const f=imported[id.split('?')[0]];return f?readFileSync(f,'utf8'):null;},transform(code,id){if(id.endsWith('/architecture.spec.ts'))return {code:ts.transpileModule(code,{compilerOptions:{target:ts.ScriptTarget.ES2018,module:ts.ModuleKind.ESNext}}).outputText,map:null};}},jison(),jsonSchemaPlugin()],
 resolve:{alias:{'@mermaid-js/parser':resolve('packages/parser/src/index.ts')}},
 define:{'injected.includeLargeFeatures':'true','injected.profiling':'false',packageVersion:"'0.0.0'"},
 test:{environment:'jsdom',globals:true,maxWorkers:1,testTimeout:30000,clearMocks:true,
 include:FILES,includeSource:INLINE}
});
""".replace('IMPORTED', json.dumps(imported)).replace('FILES', json.dumps([f for f in files if f not in script_files])).replace(
        'INLINE', json.dumps([f for f in files if '.spec.' not in f and '.test.' not in f]))
    (work / 'reference.config.ts').write_text(config)
    projects = [str(work / 'reference.config.ts')]
    if script_files:
        # docs.mts reads ../mermaid/package.json from its package working
        # directory. A distinct package-root project preserves that contract
        # without patching the original test or mocking filesystem results.
        script_config = config.replace('defineConfig({', 'defineConfig({root:' + json.dumps(str(upstream / 'packages/mermaid')) + ',')
        script_config = script_config.replace("resolve('packages/parser/src/index.ts')", json.dumps(str(upstream / 'packages/parser/src/index.ts')))
        script_config = script_config.replace('include:' + json.dumps([f for f in files if f not in script_files]), 'include:' + json.dumps([str(upstream / f) for f in script_files]))
        (work / 'scripts.config.ts').write_text(script_config)
        projects.append(str(work / 'scripts.config.ts'))
    # Upstream's workspace executes documentation tests in both its main and
    # documentation projects. Preserve those occurrences with a real second
    # project; never duplicate a result row to manufacture the second execution.
    docs_files = [f for f in files if '/src/docs/' in f]
    if docs_files:
        docs_config = """import {mergeConfig} from 'vitest/config';
import original from '../../packages/mermaid/src/docs/vite.config.ts';
import {readFileSync} from 'node:fs';
const imported = IMPORTED;
export default mergeConfig(original,{
 plugins:[{name:'pinned-imported-docs',enforce:'pre',load(id){const f=imported[id.split('?')[0]];return f?readFileSync(f,'utf8'):null;}}],
 test:{name:'original-docs-project',environment:'node',globals:false,maxWorkers:1,testTimeout:30000,include:FILES}
});
""".replace('IMPORTED', json.dumps(imported)).replace('FILES', json.dumps(docs_files))
        (work / 'docs.config.ts').write_text(docs_config)
        projects.append(str(work / 'docs.config.ts'))
    result_path = dest / 'reference.json'
    # A prior run must never silently stand in for a failed current invocation.
    result_path.unlink(missing_ok=True)
    timed_out, exit_code = False, 0
    combined = {'testResults': [], 'projects': []}
    with (dest / 'reference.log').open('w') as log:
        for index, project in enumerate(projects):
            workspace = work / f'project-{index}.workspace.ts'
            workspace.write_text("import {defineWorkspace} from 'vitest/config';export default defineWorkspace([" + json.dumps(project) + ']);\n')
            project_result = dest / f'project-{index}.json'
            project_result.unlink(missing_ok=True)
            cwd = upstream / 'packages/mermaid' if Path(project).name == 'scripts.config.ts' else upstream
            try:
                result = subprocess.run(
                    ['pnpm', 'exec', 'vitest', 'run', '--config', project,
                     '--workspace', str(workspace), '--reporter=json', '--outputFile=' + str(project_result)],
                    cwd=cwd, env={**os.environ, 'TZ': 'UTC'}, stdout=log, stderr=subprocess.STDOUT, timeout=timeout)
                code = result.returncode
                exit_code = exit_code or code
            except subprocess.TimeoutExpired:
                code, timed_out, exit_code = None, True, 1
            combined['projects'].append({'config': Path(project).name, 'exitCode': code})
            if project_result.exists():
                combined['testResults'].extend(json.loads(project_result.read_text())['testResults'])
    result_path.write_text(json.dumps(combined, indent=2) + '\n')
    actual = {}
    suite_errors = []
    if result_path.exists():
        report = json.loads(result_path.read_text())
        occurrences = collections.Counter()
        for suite in report['testResults']:
            file = str(Path(suite['name']).relative_to(upstream))
            if not suite['assertionResults'] and suite.get('message'):
                suite_errors.append({'file': file, 'message': suite['message']})
            for assertion in suite['assertionResults']:
                key = (file, assertion['fullName'])
                occurrence = occurrences[key]
                occurrences[key] += 1
                actual[(*key, occurrence)] = assertion
    rows = []
    for assertion in target:
        key = (assertion['file'], assertion['test'], assertion['occurrence'])
        ref = actual.get(key)
        rows.append({**assertion,
                     'imported': True,
                     'referenceRun': ref['status'] if ref else 'not_collected',
                     'referenceErrors': ref.get('failureMessages', []) if ref else [],
                     'nativeRun': 'not_executed',
                     'blocker': 'No validated Native adapter for this assertion occurrence'})
    summary = {'group': group, 'filesImported': len(files), 'assertionsImported': len(rows),
               'referenceExitCode': exit_code, 'referenceTimedOut': timed_out,
               'referenceCounts': dict(collections.Counter(a['referenceRun'] for a in rows)),
               'nativeCounts': {'not_executed': len(rows)}, 'nativeNewPasses': 0,
               'suiteErrors': suite_errors}
    (dest / 'assertions.json').write_text(json.dumps(rows, indent=2) + '\n')
    (dest / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    print(json.dumps(summary), flush=True)
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--upstream', type=Path, help='Required for execution; optional for imported-only hash verification')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--group', action='append', choices=sorted(MANIFEST['groups']),
                        help='Repeat for several modules; default imports/runs all modules')
    parser.add_argument('--verify-only', action='store_true')
    parser.add_argument('--timeout', type=int, default=900)
    args = parser.parse_args()
    if not args.verify_only and args.upstream is None:
        parser.error('--upstream is required for execution')
    upstream = args.upstream.resolve() if args.upstream else None
    output = args.output.resolve()
    verify(upstream)
    groups = args.group or list(MANIFEST['groups'])
    output.mkdir(parents=True, exist_ok=True)
    receipt = {'revision': MANIFEST['revision'], 'files': len(MANIFEST['files']),
               'assertions': len(MANIFEST['assertions']), 'groups': MANIFEST['groups'],
               'importedSourcesVerified': True, 'upstreamSourcesVerified': upstream is not None, 'nativeNewPasses': 0}
    (output / 'import-verification.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt), flush=True)
    if not args.verify_only:
        summaries = [run_group(upstream, output, g, args.timeout) for g in groups]
        # Independent invocations can run disjoint groups into one output root.
        # Keep their receipts separate, and collect every completed group.
        all_summaries = [json.loads(path.read_text()) for path in sorted(output.glob('*/summary.json'))]
        (output / 'batch-summary.json').write_text(json.dumps(all_summaries, indent=2) + '\n')
        return int(any(s['referenceExitCode'] != 0 or s['referenceCounts'].get('not_collected', 0) for s in summaries))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

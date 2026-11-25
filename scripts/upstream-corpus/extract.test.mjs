import assert from 'node:assert/strict';
import { test } from 'node:test';
import { diagramType, extractStrings, hasExactHeader } from './extract.mjs';

test('diagram type skips front matter, directives and comments', () => {
  assert.equal(diagramType('---\ntitle: x\n---\n%%{init: {}}%%\n%% c\ngraph TD\nA-->B\n'), 'flowchart');
  assert.equal(diagramType("%%{\n  init: { 'theme': 'base' }\n}%%\nclassDiagram\nA <|-- B\n"), 'class');
  assert.equal(diagramType('C4Context\ntitle t\n'), 'c4');
  assert.equal(diagramType('hello'), null);
  assert.equal(hasExactHeader('usecase-beta\nactor A\n'), true);
  assert.equal(hasExactHeader('QuadRantChart\nx\n'), false);
});

test('spec strings: sources, const templates, titles and prose', () => {
  const code = `
    const dir = 'LR';
    describe('pie diagrams', () => {
      it('flow', () => {
        parser.parse(\`graph \${dir}\n  A-->B\`);
        parser.parse('graph TD;' + 'A-->B');
        expect(t).toBe('Block diagram');
        parser.parse(\`graph \${unknownVar}\nA\`);
        detectType('pie');
      });
    });`;
  const { cases, skipped } = extractStrings(code, 'packages/x.spec.js');
  assert.deepEqual(cases.map((c) => c.source), ['graph LR\nA-->B\n', 'graph TD;A-->B\n']);
  assert.equal(cases[0].title, 'pie diagrams > flow');
  assert.equal(skipped.length, 1);
  assert.match(skipped[0].reason, /unknownVar/);
});

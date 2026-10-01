# Native Markdown line compatibility

Runs the nine enabled, unchanged `markdownToLines` assertions from the pinned upstream `handle-markdown-text.spec.ts`. The original test and implementation hashes are checked before either run. Java transports each UTF-8 source to production `MermaidMarkdown.lines`, then projects the public content/type fields. It does not parse emphasis or split words. The same Kotlin API supplies real flowchart Markdown node measurement and drawing.

The Native model additionally retains word adjacency for punctuation around emphasis; that layout field is outside the original word projection. HTML output, non-Markdown conversion and the upstream skipped no-auto-wrap test are unselected. This is an inline-emphasis contract, not a complete CommonMark implementation. Group and edge label formatting are unchanged.

Run after building the Android runtime jars:

```sh
python3 compatibility/upstream-markdown-lines/run.py --upstream /path/to/upstream-mermaid --stdlib /path/to/kotlin-stdlib.jar
```

Evidence is written into upstream `.native-markdown-lines-audit`: original/native JSON reports, captured calls, Kotlin outputs, logs and runtime hashes. The adapter throws for inputs/configurations it has not replayed through Native.

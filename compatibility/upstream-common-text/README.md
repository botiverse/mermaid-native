# Original common text assertions

Runs the pinned 32-case `common.spec.ts` unchanged. Six generic-formatting, seven occurrence-counting and ten line-break assertions delegate to the production Native `MermaidText` functions. The other nine DOMPurify/security assertions remain original JavaScript and are explicitly not Native coverage. The adapter performs no formatting or sanitization; exact captured arguments are processed by a fresh Native JAR.

`ClassMemberFormatter` calls shared generic formatting and occurrence counting. Both class notes and sequence text use shared break detection/splitting. This fixes `</br>`, `</br >` and `</BR>` appearing as text instead of separate lines. Existing renderer styles stay unchanged. These functions operate on plain text and do not parse or evaluate HTML.

The old baseline uses the actual production generic formatter and class-note splitter. It has no separately callable occurrence counter, so seven baseline assertions record missing observability rather than seven product bugs. Baseline22/32 includes nine original JS-only assertions; the three closing-break cases fail in real layout preprocessing. Native layout regressions check separate measured/drawn lines, and original93 class-member assertions are replayed after the extraction.

Run `python3 compatibility/upstream-common-text/run.py --upstream /path/to/pinned/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar` after building core/layout debug runtime JARs. `--repo` selects a baseline. `.native-common-text-audit/` contains reports, captured calls and runtime JAR SHA-256. Passing the retained browser security cases does not certify Native sanitization.

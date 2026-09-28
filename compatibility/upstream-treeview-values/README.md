# Original TreeView token conversion assertions

Replays all 19 pinned `treeViewValueConverter.test.ts` assertions unchanged. The official converter records rule/input pairs, then a production Native JAR evaluates those pairs. The adapter projects the Native scalar result (`null` becomes JavaScript `undefined`) without converting token values itself. Production `TreeViewParser` uses the same six conversion functions after validating token boundaries.

The baseline projects the existing real parser's node fields for the 18 recognized rules; its unknown-rule fallback is adapter-only and is explicitly marked in the captured results. The final Native converter handles all 19 calls. This is a shared-boundary extraction and test integration, not a claim that the previous 18 token conversions were broken. Original TreeView consumer assertions and real-entry whitespace/annotation checks protect existing behavior. No full Langium or third-party icon asset support is claimed.

Run `python3 compatibility/upstream-treeview-values/run.py --upstream /path/to/pinned/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar` after building the debug core JAR. `--repo` selects a baseline checkout. Results and JAR SHA-256 are written under the upstream checkout's `.native-treeview-values-audit/`.

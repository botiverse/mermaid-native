# Original route-refinement contract

Ports three complete upstream modules: source port swapping, destination terminal-stub cleanup (including label relocation), and shared interior track separation. The three unchanged original test files contain four assertions. The harness invokes the Native typed APIs, compares each resulting route/node collection with the reference, then replays the original assertions. Four controls restore the unrefined inputs and must fail the intended assertion. JavaScript transports data and assertions; it does not calculate geometry.

The default Native APIs preserve upstream behavior. The actual flow renderer adds a minimum8px terminal-run constraint to track candidate search and accepts each pass atomically only for fully orthogonal rectangle-node graphs when no node penetration or new strict crossings occur. Other shapes and diagonal paths retain their previous geometry. This executes before label placement; callers are not mutated.

Validation on producer fc273a7: original4 passed,4 full-result comparisons,4 corruption controls; existing endpoint11 and geometry12/24inputs passed. JVM304 layout,5 samples and10 SVG tests passed. Five new tests cover collision/hidden-route guards, label relocation and immutability, blocked rail search, production gates, and actual painted self-loop clearance. Across160 generated flowgraphs the final guarded pipeline left baseline geometry unchanged; the initial unrestricted version shortened terminals in3graphs to2px, which motivated the production clearance constraint. This is not evidence of broad layout improvement or general loop parity.

Platform/optimized-Wasm and actual-browser validation is still pending; see validation.json. No SDK release and no cumulative coverage credit yet.

Run with the repository's built Android runtime JARs, pinned upstream checkout and Kotlin stdlib:

```sh
python3 compatibility/upstream-route-refinement/run.py --upstream /path/to/upstream --stdlib /path/to/kotlin-stdlib.jar --verify-mutations
```

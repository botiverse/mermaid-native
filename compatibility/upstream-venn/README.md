# Original Venn parser assertions

Pinned upstream revision: `04ee3364045d6573f84034d3c9368cc50233a92f`.
Run `run.py --upstream PATH --stdlib PATH` after building the Native JVM runtime. Spec hashes are checked, original Jison calls are captured, and the unchanged 17 assertions are repeated through the real `MermaidParser` and Native model. No original parser or database mutation implementation is reused in the Native phase.

The bridge projects subsets, text nodes and styles. It sorts identifier lists for the original database contract and resolves omitted subset sizes as `10 / memberCount²`. Reflection for the newly added text/style getters allows an older baseline JAR to run (absent getters expose empty collections). Baseline: 4/17. Passing assertions are parser-boundary evidence, not pixel or area-equivalence coverage.

The renderer retains Native overlap circles and opacity, supports empty and single-set diagrams, and displays explicit text nodes. Applied styles are a bounded color subset (fill/stroke/text color with named, hex, rgb/rgba values); arbitrary CSS is not executed. Native admission still caps diagrams at three sets and requires positive finite sizes and unique subsets. General multi-set optimization, all upstream style properties, untested grammar edge cases and the separate original DB suite are outside this increment.

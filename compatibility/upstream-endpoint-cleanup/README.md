# Orthogonal endpoint and polyline cleanup

Two complete unchanged upstream files contain 11 assertions: five polyline cleanup and six endpoint clipping/preparation tests. The harness passes original points, node bounds and edge metadata directly to Native APIs. The mutable JavaScript interface receives only the Native output point lists. Six corruption controls target orientation, spikes, buried endpoints, corner clearance, duplicate terminal points and target approach side. No JavaScript geometry logic is used.

The flow renderer invokes endpoint clipping only for finite, already-orthogonal paths between rectangle nodes, before straight-edge label placement. Diagonal routes and other shapes retain their previous paths. This bounded cleanup does not solve routing through other nodes or groups. The upstream renderer duplication shim is exposed separately and never fed into Native drawing commands.

Native empty and singleton input paths are safe. API outputs are immutable lists by convention and caller inputs are not mutated. Upstream dimensions default to zero; only positive rectangles participate. These APIs do not emulate arbitrary JavaScript coercion.

Build core/layout-api/layout-simple Android debug runtime jars, then run run.py --upstream PATH --stdlib PATH --verify-mutations. The full 11 original assertions, 12 semantic call comparisons and six corruption controls pass. JVM layout299, samples5 and SVG10 pass; previous geometry12 also pass. Other platform and browser checks remain pending. No original coverage promoted and no SDK release.

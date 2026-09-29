# Original Requirement database model assertions

Pinned upstream `04ee3364045d6573f84034d3c9368cc50233a92f`; source hashes are verified before each run.

Run `python3 compatibility/upstream-requirement-model/run.py --upstream /path/to/installed/upstream --stdlib /path/to/kotlin-stdlib.jar` after building the core debug runtime JAR.

Nine unchanged original assertions check requirement/element publication, relationships, class definitions, direction, direct styles, class membership and inherited styles. The adapter translates original database setter calls to Requirement source, then uses actual `MermaidParser` and `RequirementDiagram.resolvedStyles`; the same style resolution is consumed by production card drawing. Java only projects typed data, and JavaScript stores per-test source/results. It does not compute style inheritance or substitute the official database for Native results.

The unique edge ID assertion is unselected because Native scene edges do not expose those DOM IDs. No JS lifecycle or DOM ID coverage is claimed. These model assertions do not establish parity for all setter timing combinations or all CSS properties.

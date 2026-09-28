# Original Packet assertion bridge

Runs the unchanged pinned `packages/parser/tests/packet.test.ts` (8 grammar assertions) and `packages/mermaid/src/diagrams/packet/packet.spec.ts` (15 consumer assertions), with 20 distinct inputs. The reference calls are captured; the Native run obtains shared Kotlin models/diagnostics and projects their resolved ranges into the original AST shape. Original source hashes and runtime JAR hashes are checked and recorded.

The 15 consumer assertions retain the original JavaScript packet row population, including its historical segment-bit-count convention. They are mixed-component coverage, not proof of Native row layout or bit counts. Native rendering independently uses inclusive range widths; its multirow geometry and empty diagram behavior have direct tests. The grammar assertions are parser-boundary coverage, not Native screenshot equivalence.

Run `python3 compatibility/upstream-packet/run.py --upstream /path/to/upstream --stdlib /path/to/kotlin-stdlib.jar` after building the core debug runtime JAR. The runner does not launch Gradle. Results are under the upstream `.native-packet-audit` directory.

The Native renderer retains its existing 4095 maximum bit index; larger diagrams fail explicitly rather than allocating unbounded layouts. This bridge does not claim complete Langium recovery AST compatibility.

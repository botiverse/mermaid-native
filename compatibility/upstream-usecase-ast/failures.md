# Remaining original Usecase publication/AST failures

Source6ef0d1b498d59da279c38ee558073ec83a3f9f81. All77 reference assertions pass; Native40pass/37fail, zero skipped. These failures remain nonzero and are not counted as passes. Detailed raw results are preserved with the audit.

## rejects conflicting kind declarations transactionally

Error: promise resolved "undefined" instead of rejecting

## rejects conflicting shape declarations transactionally

Error: promise resolved "undefined" instead of rejecting

## rejects conflicting label declarations transactionally

Error: promise resolved "undefined" instead of rejecting

## rejects conflicting stereotype declarations transactionally

Error: promise resolved "undefined" instead of rejecting

## rejects conflicting parent declarations transactionally

Error: promise resolved "undefined" instead of rejecting

## rejects a generated ID collision in the diagram-global namespace

Error: promise resolved "undefined" instead of rejecting

## rejects a generated and explicit ID collision in the diagram-global namespace

Error: promise resolved "undefined" instead of rejecting

## rejects a global cross-kind ID collision in the diagram-global namespace

Error: promise resolved "undefined" instead of rejecting

## rejects a element and explicit edge ID collision in the diagram-global namespace

Error: promise resolved "undefined" instead of rejecting

## rejects a duplicate explicit edge ID collision in the diagram-global namespace

Error: promise resolved "undefined" instead of rejecting

## names both exact locations and the source labels for generated and explicit ID collisions

AssertionError: expected undefined to be an instance of Error

## still rejects a parenthesis inside an unquoted label

AssertionError: expected false to be true // Object.is equality

## still rejects a bracket inside an unquoted label

AssertionError: expected false to be true // Object.is equality

## still rejects a brace inside an unquoted label

AssertionError: expected undefined to be an instance of Error

## still rejects a relationship operator inside an unquoted label

AssertionError: expected undefined to be an instance of Error

## still rejects the class suffix inside an unquoted label

AssertionError: expected undefined to be an instance of Error

## still rejects the metadata suffix inside an unquoted label

AssertionError: expected false to be true // Object.is equality

## still rejects a double quote inside an unquoted label

Error: promise resolved "undefined" instead of rejecting

## still rejects a single quote inside an unquoted label

Error: promise resolved "undefined" instead of rejecting

## reports exact actor metadata and incompatible icon locations

AssertionError: expected 'Invalid actor type \'giant\' for \'Us…' to be 'Metadata property \'type\' is invalid…' // Object.is equality

## rejects mixed-kind generalization and actor include at the exact relation span

AssertionError: expected undefined to be an instance of Error

## rejects duplicate and unknown edge class/style targets with exact locations

AssertionError: expected undefined to be an instance of Error

## publishes exact true, fast, slow, and false animation state and rejects invalid values

AssertionError: expected [ { id: 'trueEdge', …(2) }, …(3) ] to deeply equal [ { id: 'trueEdge', …(2) }, …(3) ]

## locates rejected 'relation' content inside a boundary

AssertionError: expected false to be true // Object.is equality

## locates rejected 'note' content inside a boundary

AssertionError: expected false to be true // Object.is equality

## locates rejected 'JSON' content inside a boundary

AssertionError: expected false to be true // Object.is equality

## locates rejected 'nested boundary' content inside a boundary

AssertionError: expected false to be true // Object.is equality

## rejects notes targeting JSON, boundary, and explicit edge IDs with both locations

AssertionError: expected 'Unknown usecase target Payload' to be 'Note target \'Payload\' must be an ac…' // Object.is equality

## locates a stereotype on invalid boundary syntax

AssertionError: expected false to be true // Object.is equality

## locates a stereotype on invalid note syntax

AssertionError: expected false to be true // Object.is equality

## locates a stereotype on invalid JSON syntax

AssertionError: expected false to be true // Object.is equality

## locates a stereotype on invalid edge syntax

AssertionError: expected undefined to be an instance of Error

## rejects business icon, awesome, and rectangular declarations at exact locations

AssertionError: expected 'Business actor \'Icon\' must use norm…' to be 'Business actor \'Icon\' must use norm…' // Object.is equality

## rejects JSON in boundaries and JSON semantic or circle relations at exact locations

AssertionError: expected false to be true // Object.is equality

## requires inline boundary metadata before the class suffix

AssertionError: expected false to be true // Object.is equality

## rejects inline boundary metadata that a boundary does not accept

AssertionError: expected undefined to be an instance of Error

## publishes a complete serializable AST v1 with ordered statements and exact spans

AssertionError: expected { version: 1, …(8) } to match object { version: 1, …(6) }

# Remaining original Usecase publication/AST failures

Production 2edf843be5e02326aaf74d8d05ef296686b82c24. All 77 reference assertions pass; Native 68 pass / 9 fail, zero skipped. Nine previously failing statement-grammar diagnostics now pass. Detailed raw results are preserved with the audit.

## reports exact actor metadata and incompatible icon locations

AssertionError: expected 'Invalid actor type \'giant\' for \'Us…' to be 'Metadata property \'type\' is invalid…' // Object.is equality

## rejects mixed-kind generalization and actor include at the exact relation span

AssertionError: expected undefined to be an instance of Error

## rejects duplicate and unknown edge class/style targets with exact locations

AssertionError: expected 'Unknown usecase target missingEdge' to be 'Class/style target \'missingEdge\' is…' // Object.is equality

## publishes exact true, fast, slow, and false animation state and rejects invalid values

AssertionError: expected [ { id: 'trueEdge', …(2) }, …(3) ] to deeply equal [ { id: 'trueEdge', …(2) }, …(3) ]

## rejects notes targeting JSON, boundary, and explicit edge IDs with both locations

AssertionError: expected 'Unknown usecase target Payload' to be 'Note target \'Payload\' must be an ac…' // Object.is equality

## rejects business icon, awesome, and rectangular declarations at exact locations

AssertionError: expected 'Business actor \'Icon\' must use norm…' to be 'Business actor \'Icon\' must use norm…' // Object.is equality

## rejects JSON in boundaries and JSON semantic or circle relations at exact locations

AssertionError: expected undefined to be an instance of Error

## rejects inline boundary metadata that a boundary does not accept

AssertionError: expected undefined to be an instance of Error

## publishes a complete serializable AST v1 with ordered statements and exact spans

AssertionError: expected { version: 1, …(8) } to match object { version: 1, …(6) }

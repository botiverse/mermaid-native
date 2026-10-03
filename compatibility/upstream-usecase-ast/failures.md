# Remaining original Usecase publication/AST failures

Production 03c585a7f78f6764fd025860219ecfaf5bc4fa41. All77 reference assertions pass; Native71 pass /6fail, zero skipped. Three previously failing metadata assertions now pass.

## rejects mixed-kind generalization and actor include at the exact relation span

AssertionError: expected undefined to be an instance of Error

## rejects duplicate and unknown edge class/style targets with exact locations

AssertionError: expected 'Unknown usecase target missingEdge' to be 'Class/style target \'missingEdge\' is…' // Object.is equality

## publishes exact true, fast, slow, and false animation state and rejects invalid values

AssertionError: expected [ { id: 'trueEdge', …(2) }, …(3) ] to deeply equal [ { id: 'trueEdge', …(2) }, …(3) ]

## rejects notes targeting JSON, boundary, and explicit edge IDs with both locations

AssertionError: expected 'Unknown usecase target Payload' to be 'Note target \'Payload\' must be an ac…' // Object.is equality

## rejects JSON in boundaries and JSON semantic or circle relations at exact locations

AssertionError: expected undefined to be an instance of Error

## publishes a complete serializable AST v1 with ordered statements and exact spans

AssertionError: expected { version: 1, …(8) } to match object { version: 1, …(6) }

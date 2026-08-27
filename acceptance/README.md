# Mermaid Native Web consumer acceptance

This is a long-lived browser consumer for the public `MermaidWebAdapter` Wasm
boundary. It deliberately contains no parser, layout engine, or release code.
The host injects the generated Wasm module as `window.mermaidNative`; `index.html`
can therefore be served by any static test server after the Web target build.

Acceptance contract:

- successful SVG is parsed detached, restricted to the SVG namespace, and stripped of executable nodes/handlers;
- failures remain typed JSON diagnostics with line/column information;
- keyboard-accessible controls and a live status region are present;
- no network, eval, inline event handlers, or third-party runtime are required;
- the Gradle `verifyWebAcceptance` gate checks the 32-family matrix and shell invariants.

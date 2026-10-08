# Security

Please report suspected vulnerabilities privately to the repository maintainers. Use [GitHub private vulnerability reporting](https://github.com/botiverse/mermaid-native/security/advisories/new) if available; otherwise contact a maintainer privately before sharing exploit details. Include the affected version, minimal input and impact without credentials or private customer data. Do not post an exploit in a public issue before maintainers can assess it.

## Processing untrusted diagrams

Treat diagram source as untrusted input. The library is not a sandbox or a guarantee of bounded resource consumption for arbitrary graphs. Hosts should limit source size, graph size and nesting, bound layout work, and avoid doing expensive parsing/layout on the UI thread.

Labels are serialized as text. Supported Kanban ticket links are bounded absolute HTTP(S) links; hosts remain responsible for navigation policy. Native Canvas renderers do not automatically execute links or JavaScript callbacks. General raw HTML and scriptable SVG are not supported integration features.

If you embed SVG, keep your sanitizer enabled. The sample sanitizer in `acceptance/svg-sanitizer.js` allows the library's limited local gradients and link structures while rejecting executable schemes and external resource references. Do not broaden its allowlist indiscriminately. Review [the integration guide](docs-site/guide/getting-started.md) when adopting new renderer output.

Avoid logging private diagram text, signed URLs or customer attachments in diagnostic reports. Keep application-level permissions, storage and network policy in the host application.

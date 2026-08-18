# Security

Treat diagram source as untrusted input. The parser and future renderers must
bound source size, statement count, node/edge count, nesting, layout work, and
serialized output. Labels are text by default; raw HTML, links, click handlers,
and scriptable SVG are not part of the current support matrix. Any future opt-in must sanitize and
test its boundary before release.

Please report vulnerabilities privately to the repository maintainers rather
than opening a public issue with an exploit.

# Upstream TreeView icon selection

Run `run.py --upstream /path/to/upstream --stdlib /path/to/kotlin-stdlib.jar`
after building the production Android core runtime JAR. Source/spec hashes are
pinned in sources.json.

The unchanged original icon spec calls the production Kotlin `TreeViewIcons`
resolver through a transport-only bridge. The same resolver is consumed by
TreeView's Native layout, which draws built-in file/folder vector glyphs and
keeps custom pack references readable as fallback text. Maps and pack defaults
can be supplied through TreeViewDiagram.iconConfig; no YAML/config parsing or
external icon-pack downloading is added here.

Twenty original detection/selection assertions are selected. Three tests about
the upstream SVG icon-pack object are excluded because Native draws its own
vectors rather than transporting DOM path strings. This does not claim external
pack rendering or SVG-path parity. No original assertion inputs/expectations are
edited. Java/Python/JavaScript only transport fields and nullable strings.

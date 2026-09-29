// The dense Treemap regression lays out 30,000 leaves. Keep its full geometry
// assertions while allowing Wasm execution on shared development machines.
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 15000 });

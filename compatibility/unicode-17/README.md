# Unicode 17 grapheme and width data

Production grapheme segmentation follows [UAX #29 revision 47](https://www.unicode.org/reports/tr29/tr29-47.html), Unicode 17.0.0, including extended grapheme rules GB3–GB9c, GB11, GB12/13 and GB999. It preserves original UTF-16 substrings without normalization. Isolated surrogates are preserved; the optional code-point fallback preserves surrogate pairs but does not join grapheme clusters.

`sources.json` records exact official source URLs and SHA-256 hashes. `generate.py` verifies these inputs before generating compact range tables in common production Kotlin and the 766 unchanged boundary-token cases from `GraphemeBreakTest.txt` in common tests. Hangul syllable LV/LVT properties are calculated algorithmically; other properties come from the pinned tables. Run from any directory:

```sh
python3 compatibility/unicode-17/generate.py
```

`UnicodeGraphemesTest.officialUnicode17Conformance` checks every supplied boundary on each configured Kotlin target. Additional tests cover code-point fallback, formatting preservation, impossible widths, and actual Usecase/JSON layout consumption. Native fonts determine cluster widths; this is segmentation and wrapping support, not a claim of font shaping parity with browsers.

The source data, generated tables and conformance cases are covered by the accompanying Unicode License v3. These cases are not counted as Mermaid original test coverage.

The same generator also emits the 59 BMP Wide/Fullwidth ranges from the pinned
[EastAsianWidth.txt](https://www.unicode.org/Public/17.0.0/ucd/EastAsianWidth.txt).
`FixedWidthTextMeasurer` uses these to reserve one em for wide BMP glyphs, fixing
CJK labels and JSON cell wrapping in hosts without real font metrics. Other
UTF-16 units retain the previous 0.6 em estimate, including supplementary pairs
at 1.2 em. Ambiguous-width characters retain their previous estimate; this does not
introduce combining-mark or emoji shaping. Hosts needing font-accurate measurements should supply `TextMeasurer`.
These fallback metric regression tests do not add original Mermaid coverage.

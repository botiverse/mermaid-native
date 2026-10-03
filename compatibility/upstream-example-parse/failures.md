# Original example parse failures

Production source: `5e4eb8224a474f3ef7426542f4ced8476b26b678`. All 79 unchanged original parse assertions passed against upstream; 66 pass and 13 fail against Native. These are known parser gaps, not skipped tests.

## Flowchart: Expanded Node Shapes

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_SYNTAX, message=No such shape: manual-input., location=SourceLocation(line=2, column=5))]

```mermaid
flowchart TD
    Form@{ shape: manual-input, label: "User fills in form" }
    Docs@{ shape: docs, label: "Uploaded documents" }
    Check@{ shape: procs, label: "Automated checks" }
    Decision@{ shape: diam, label: "Application approved?" }
    DB@{ shape: cyl, label: "Customer database" }
    Letter@{ shape: stadium, label: "Send welcome email" }

    Form --> Docs
    Docs --> Check
    Check --> Decision
    Decision -->|Yes| DB
    Decision -->|No| Form
    DB --> Letter
```

## Kanban Diagram: Mermaid Sprint Board

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_DIAGRAM, message=Unsupported Mermaid diagram header: ---, location=SourceLocation(line=1, column=1))]

```mermaid
---
config:
  kanban:
    ticketBaseUrl: 'https://github.com/mermaid-js/mermaid/issues/#TICKET#'
---
kanban
  todo[Todo]
    docs[Create documentation]
    blog[Write blog post about the new diagram]@{ priority: 'Low' }
  inProgress[In progress]
    renderer[Improve renderer for edge cases]@{ assigned: 'knsv', priority: 'High' }
  readyForTest[Ready for test]
    parserTests[Create parsing tests]@{ ticket: 2038, assigned: 'K.Sveidqvist', priority: 'High' }
  done[Done]
    grammar[Design grammar]@{ assigned: 'knsv' }
    longTitle[Title of diagram is more than 100 chars when user duplicates diagram with 100 char]@{ ticket: 2036, priority: 'Very High' }
    dbFunction[Update DB function]@{ ticket: 2037, assigned: 'knsv', priority: 'High' }
```

## Radar Diagram: Student Grades

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_DIAGRAM, message=Unsupported Mermaid diagram header: ---, location=SourceLocation(line=1, column=1))]

```mermaid
---
title: "Grades"
---
radar-beta
  axis m["Math"], s["Science"], e["English"]
  axis h["History"], g["Geography"], a["Art"]
  curve a["Alice"]{85, 90, 80, 70, 75, 90}
  curve b["Bob"]{70, 75, 85, 80, 90, 85}

  max 100
  min 0

```

## XY Chart: Coffee Sales with Data Labels

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_DIAGRAM, message=Unsupported Mermaid diagram header: ---, location=SourceLocation(line=1, column=1))]

```mermaid
---
config:
  xyChart:
    showDataLabel: true
---
xychart-beta
    title "Cups sold per day"
    x-axis [Espresso, Latte, "Cold Brew", Mocha, Tea]
    y-axis "Cups" 0 --> 120
    bar [95, 110, 68, 45, 30]
```

## XY Chart: Sign-ups vs Churn

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_DIAGRAM, message=Unsupported Mermaid diagram header: ---, location=SourceLocation(line=1, column=1))]

```mermaid
---
config:
  themeVariables:
    xyChart:
      plotColorPalette: '#2563eb, #dc2626'
---
xychart-beta
    title "Sign-ups vs churned users"
    x-axis [Q1, Q2, Q3, Q4]
    y-axis "Users" 0 --> 500
    line [120, 260, 380, 470]
    line [40, 60, 90, 110]
```

## Sankey Diagram: Energy Flow (UK)

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_DIAGRAM, message=Unsupported Mermaid diagram header: ---, location=SourceLocation(line=1, column=1))]

```mermaid
---
config:
  sankey:
    showValues: false
---
sankey-beta

Agricultural 'waste',Bio-conversion,124.729
Bio-conversion,Liquid,0.597
Bio-conversion,Losses,26.862
Bio-conversion,Solid,280.322
Bio-conversion,Gas,81.144
Biofuel imports,Liquid,35
Biomass imports,Solid,35
Coal imports,Coal,11.606
Coal reserves,Coal,63.965
Coal,Solid,75.571
District heating,Industry,10.639
District heating,Heating and cooling - commercial,22.505
District heating,Heating and cooling - homes,46.184
Electricity grid,Over generation / exports,104.453
Electricity grid,Heating and cooling - homes,113.726
Electricity grid,H2 conversion,27.14
Electricity grid,Industry,342.165
Electricity grid,Road transport,37.797
Electricity grid,Agriculture,4.412
Electricity grid,Heating and cooling - commercial,40.858
Electricity grid,Losses,56.691
Electricity grid,Rail transport,7.863
Electricity grid,Lighting & appliances - commercial,90.008
Electricity grid,Lighting & appliances - homes,93.494
Gas imports,NGas,40.719
Gas reserves,NGas,82.233
Gas,Heating and cooling - commercial,0.129
Gas,Losses,1.401
Gas,Thermal generation,151.891
Gas,Agriculture,2.096
Gas,Industry,48.58
Geothermal,Electricity grid,7.013
H2 conversion,H2,20.897
H2 conversion,Losses,6.242
H2,Road transport,20.897
Hydro,Electricity grid,6.995
Liquid,Industry,121.066
Liquid,International shipping,128.69
Liquid,Road transport,135.835
Liquid,Domestic aviation,14.458
Liquid,International aviation,206.267
Liquid,Agriculture,3.64
Liquid,National navigation,33.218
Liquid,Rail transport,4.413
Marine algae,Bio-conversion,4.375
NGas,Gas,122.952
Nuclear,Thermal generation,839.978
Oil imports,Oil,504.287
Oil reserves,Oil,107.703
Oil,Liquid,611.99
Other waste,Solid,56.587
Other waste,Bio-conversion,77.81
Pumped heat,Heating and cooling - homes,193.026
Pumped heat,Heating and cooling - commercial,70.672
Solar PV,Electricity grid,59.901
Solar Thermal,Heating and cooling - homes,19.263
Solar,Solar Thermal,19.263
Solar,Solar PV,59.901
Solid,Agriculture,0.882
Solid,Thermal generation,400.12
Solid,Industry,46.477
Thermal generation,Electricity grid,525.531
Thermal generation,Losses,787.129
Thermal generation,District heating,79.329
Tidal,Electricity grid,9.452
UK land based bioenergy,Bio-conversion,182.01
Wave,Electricity grid,19.013
Wind,Electricity grid,289.366
```

## Packet Diagram: TCP Packet

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_DIAGRAM, message=Unsupported Mermaid diagram header: ---, location=SourceLocation(line=1, column=1))]

```mermaid
---
title: "TCP Packet"
---
packet
0-15: "Source Port"
16-31: "Destination Port"
32-63: "Sequence Number"
64-95: "Acknowledgment Number"
96-99: "Data Offset"
100-105: "Reserved"
106: "URG"
107: "ACK"
108: "PSH"
109: "RST"
110: "SYN"
111: "FIN"
112-127: "Window"
128-143: "Checksum"
144-159: "Urgent Pointer"
160-191: "(Options and Padding)"
192-255: "Data (variable length)"
```

## Block Diagram: Three-Tier Web Architecture

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_SYNTAX, message=Expected block identifier, location=SourceLocation(line=3, column=1))]

```mermaid
block-beta
  columns 3
  user(("User")):3
  space:3
  ui["Web UI"] api["API Server"] db[("Database")]

  user --> ui
  ui --> api
  api --> db

  style user fill:#ffe0b2,stroke:#fb8c00
  style db fill:#bbdefb,stroke:#1e88e5
```

## Block Diagram: Block Arrows and Nested Blocks

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_SYNTAX, message=Expected block identifier, location=SourceLocation(line=3, column=1))]

```mermaid
block-beta
columns 1
  db(("DB"))
  blockArrowId6<["&nbsp;&nbsp;&nbsp;"]>(down)
  block:ID
    A
    B["A wide one in the middle"]
    C
  end
  space
  D
  ID --> D
  C --> D
  style B fill:#969,stroke:#333,stroke-width:4px
```

## Treemap: Monthly Household Budget

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_DIAGRAM, message=Unsupported Mermaid diagram header: ---, location=SourceLocation(line=1, column=1))]

```mermaid
---
config:
  treemap:
    valueFormat: '$0,0'
---
treemap-beta
"Monthly Budget"
    "Housing"
        "Rent": 1400
        "Utilities": 220
        "Internet": 60
    "Food"
        "Groceries": 480
        "Dining out": 180
    "Transport"
        "Car payment": 320
        "Fuel": 140
    "Savings"
        "Emergency fund": 300
        "Retirement": 400
```

## TreeView: Annotations

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_DIAGRAM, message=Unsupported Mermaid diagram header: ---, location=SourceLocation(line=1, column=1))]

```mermaid
---
config:
  treeView:
    showIcons: true
---
treeView-beta
            src/
                App.tsx :::highlight icon(logos:react) ## main component
                index.js ## entry point
                styles.css icon(none)
            data/
                model.bin icon(logos:mysql)
            .env ## environment variables
            Dockerfile
            package.json
```

## TreeView: File-Type Icons via Config Maps

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_DIAGRAM, message=Unsupported Mermaid diagram header: ---, location=SourceLocation(line=1, column=1))]

```mermaid
---
config:
  treeView:
    showIcons: true
    defaultIconPack: material-icon-theme
    filenameIcons:
      Dockerfile: docker
    extensionIcons:
      .ts: typescript
      .tsx: react-ts
      .txt: none
---
treeView-beta
            my-project/
                src/
                    App.tsx
                    utils.ts
                Dockerfile
                notes.txt
                README.md
```

## Wardley Maps: GPT Tokeniser Architecture

FAIL
[MermaidDiagnostic(code=UNSUPPORTED_SYNTAX, message=Invalid Wardley name, location=SourceLocation(line=24, column=1))]

```mermaid
wardley-beta
title GPT Tokeniser
size [1100, 800]

anchor GPT Tokeniser [0.90, 0.58]

component tokeniser [0.81, 0.58]
component encoder [0.60, 0.32] label [1, -9]
component decoder [0.60, 0.72]
component methodology [0.72, 0.53]
component training code [0.68, 0.26] label [-90, 2]
component inference code [0.65, 0.37] label [-50, -12]
component algo [0.53, 0.50] label [-15, -17]
component GPT2 [0.81, 0.65] label [-14, 27]
component GPT3 [0.81, 0.73] label [-15, 27]
component GPT4 [0.81, 0.77] label [-14, 28]
component GPT5 [0.81, 0.37] label [-10, 28]
component GPT6 [0.81, 0.17] label [-20, 28]
component GPT7 [0.81, 0.13] label [-13, 27]
component tokeniser training data [0.29, 0.34] label [-74, -32]
component special tokens [0.59, 0.26] label [-61, 15]
component UTF8 [0.17, 0.74]
component token vocabulary [0.41, 0.56] label [-25, 16]
component byte pair encoding (BPE) [0.53, 0.76] label [-27, 19]
component english text data [0.15, 0.37] label [0, 10]
component code data [0.15, 0.30] label [-31, 23]
component foreign text data [0.15, 0.23] label [-51, 18]
component python [0.35, 0.84]
component sentencepiece [0.25, 0.80] label [-49, 19]
component IDE [0.27, 0.86]
component MEGABYTE [0.53, 0.18] label [-28, 28]
component text merging rules [0.60, 0.21] label [-64, -10]
component security framework [0.67, 0.58] label [-27, 10]
component Unicode Consortium [0.06, 0.55]
component Unicode License v3 [0.13, 0.72] label [-29, 11]

GPT Tokeniser -> tokeniser

tokeniser -> methodology
methodology -> training code
methodology -> inference code
methodology -> security framework

training code -> special tokens
training code -> python

algo -> tokeniser training data
training code -> text merging rules

training code -> encoder
inference code -> decoder
encoder -> algo
decoder -> algo

algo -> token vocabulary

byte pair encoding (BPE) -> UTF8
MEGABYTE -> UTF8
UTF8 -> Unicode License v3
Unicode License v3 -> Unicode Consortium

tokeniser training data -> english text data
tokeniser training data -> code data
tokeniser training data -> foreign text data

python -> IDE
python -> sentencepiece

pipeline tokeniser {
  component tokeniser v1 [0.11]
  component tokeniser v2 [0.80]
}

pipeline methodology {
  component methodology v1 [0.20]
  component methodology v2 [0.80]
}

pipeline algo {
  component algo v1 [0.14]
  component algo v2 [0.80]
}

deaccelerator License Play [0.13, 0.78]

annotations [1, 0]
annotation 1,[0.57, 0.16] "Alternative algos in research"
annotation 2,[0.57, 0.76] "Most popular, but not the most efficient"
annotation 3,[0.20, 0.3] "Ensure balanced token vocabulary"
annotation 4,[0.60, 0.28] "Required for delimiters"
annotation 5,[0.70, 0.50] "A structured approach for achieving a goal"

note "Voting members: Adobe, Amazon, Apple, Google, Meta, Microsoft, Netflix, Salesforce" [0.04, 0.35]

```

# Implementation vs Plan

Maps the original task IDs from the planning documents to what is actually shipped. Legend:
✅ done &nbsp;•&nbsp; 🟡 partial / different from plan &nbsp;•&nbsp; ⏳ not started &nbsp;•&nbsp; ❌ explicitly skipped

## Engine (`02_core_game_engine.md`)

| ID | Task | Status | Where |
|----|------|--------|-------|
| E-01 | Core data models (immutable) | ✅ | [engine/src/commonMain/kotlin/territories/engine/model/](../engine/src/commonMain/kotlin/territories/engine/model) |
| E-02 | LegalMoveChecker | ✅ | [LegalMoveChecker.kt](../engine/src/commonMain/kotlin/territories/engine/engine/LegalMoveChecker.kt) |
| E-03 | CaptureDetector (flood fill, 4-conn interior) | ✅ | [CaptureDetector.kt](../engine/src/commonMain/kotlin/territories/engine/engine/CaptureDetector.kt) |
| E-04 | GameEngine main API | ✅ | [GameEngine.kt](../engine/src/commonMain/kotlin/territories/engine/engine/GameEngine.kt) |
| E-05 | ScoreCalculator (both variants) | ✅ | [ScoreCalculator.kt](../engine/src/commonMain/kotlin/territories/engine/engine/ScoreCalculator.kt) |
| E-06 | GameOverDetector | ✅ | [GameOverDetector.kt](../engine/src/commonMain/kotlin/territories/engine/engine/GameOverDetector.kt) |
| — | UndoManager / MoveHistory | ✅ | [history/](../engine/src/commonMain/kotlin/territories/engine/history) |

## Data models (`03_data_models.md`)

All planned types exist: `Player`, `Coord`, `Cell`, `Board`, `Territory`, `GameState`, `GameConfig`, `Score`, `Move`, `ScoringVariant`, `GamePhase`, `PlayerType`. Persistence entities (Room) live under [app-android/.../data/db/Entities.kt](../app-android/src/main/kotlin/territories/app/data/db/Entities.kt). UI state classes were folded into the per-platform ViewModels rather than a shared `BoardUiState` — pragmatic deviation, not a regression.

## Android (`04_android_implementation.md`)

| ID | Task | Status |
|----|------|--------|
| A-01 | Project bootstrap (Hilt + Room + Compose) | ✅ |
| A-02 | Navigation graph | ✅ — [AppNavGraph.kt](../app-android/src/main/kotlin/territories/app/ui/navigation/AppNavGraph.kt) |
| A-03 | Home screen | ✅ |
| A-04 | New-game setup screen | ✅ — separate `SetupScreen` rather than living inside Settings |
| A-05 | Board canvas (touch, snap, last-move ring) | ✅ — [BoardCanvas.kt](../app-android/src/main/kotlin/territories/app/ui/screens/game/BoardCanvas.kt) |
| A-06 | Game HUD (scores, undo, surrender, menu) | ✅ |
| A-07 | GameViewModel (Hilt, AI dispatch) | ✅ — uses `:session` module instead of standalone use cases |
| — | History screen | ✅ — bonus, not in original plan |
| — | How-to-play screen | ✅ |
| — | Settings screen | ✅ |
| — | Result screen | ✅ |

Pinch-to-zoom and pan on the Android board canvas: not yet verified for the largest (60×40) preset.

## Cross-platform (`05_cross_platform_strategy.md`)

| ID | Task | Status |
|----|------|--------|
| D-01 | Compose Multiplatform desktop module | ✅ — `:app-desktop` |
| D-02 | Shared UI module | 🟡 — UI was duplicated per platform instead of factored into `shared-ui`; engine + session are shared |
| D-03 | Desktop `main()` window | ✅ |
| D-04 | Mouse click → coord | ✅ |
| D-05 | Keyboard shortcuts | 🟡 — ESC menu present; Ctrl+Z undo not wired |
| D-06 | Desktop persistence | ⏳ |
| D-07 | Installer packaging | ⏳ |
| D-08 | Code signing | ⏳ |
| W-01 | Compose-Web vs WASM decision | ✅ — went **neither**; chose Kotlin/JS + hand-rolled HTML/Canvas/CSS for full design control |
| W-02 | `:app-web` module | ✅ |
| W-03 | Canvas rendering | ✅ — paper aesthetic, HiDPI, render-on-animation |
| W-04 | Touch + mouse input | ✅ |
| W-05 | URL-based game state | ⏳ |
| W-06 | Static-host deploy | ⏳ |
| W-07 | PWA manifest + service worker | ✅ |
| W-08 | Responsive layout | ✅ |
| L-01..L-03 | Linux packaging | ⏳ |
| I-01..I-07 | iOS / macOS | ⏳ |

## AI (`06_ai_opponent.md`)

| ID | Task | Status |
|----|------|--------|
| AI-01 | Easy (random) | ✅ |
| AI-02 | Medium (1-ply greedy) | ✅ |
| AI-03 | Hard (minimax + α-β + deadline) | ✅ |
| AI-04 | Territory potential heuristic | 🟡 — superseded by BFS-depth-from-border, see below |
| AI-05 | Capture imminence heuristic | 🟡 — implicit in `oppTrapped` term of new evaluator |
| AI-06 | Move ordering for α-β | ✅ |
| AI-07 | Iterative deepening | ⏳ — fixed-depth with timeout instead |

### Evaluator deviation from plan

The plan called for a sum of independent heuristics (`territoryPotential` + `borderControl` + `captureImminence`). During implementation it became clear that those terms reward *clustering* rather than *encirclement*, and the AIs would build small squares of dots and stall.

Final design ([BoardEvaluator.kt](../engine/src/commonMain/kotlin/territories/engine/ai/BoardEvaluator.kt)) replaces those with a **multi-source BFS distance from the board border**, computed twice (once with each player's pieces as walls). This produces a smooth gradient that rewards genuine wall-building between the opponent and the edge:

```
total = realScore   × 1000
      + oppTrapped  × 400
      − ownTrapped  × 380
      + oppDepth Σ  × 6
      − ownDepth  Σ × 6
      + frontier    × 3
      + borderDots  × 4
```

Verified via the `:engine:runSimulation` task: Medium-vs-Medium games now produce real captures (e.g. final 2A-1B with perimeter walls) instead of the 0A-0B blob standoffs that the plan's heuristic produced.

## UI/UX (`07_ui_ux_guidelines.md`)

The web app is the **closest** to the planning-doc mockups: home / setup / game / result / settings / how-to-play all match the layouts shown there, with a paper-on-table aesthetic that goes beyond the spec. Android implements the same screen set in Compose. Desktop is functional but cosmetically lighter.

Animations shipped: dot placement scale-in, capture ripple, result confetti (web), AI thinking indicator, score animation (web). Not yet shipped: territory-fill flood expansion animation on Android.

## Non-goals confirmed skipped

❌ Online multiplayer &nbsp;•&nbsp; ❌ Accounts / leaderboards &nbsp;•&nbsp; ❌ 3+ player variants &nbsp;•&nbsp; ❌ Animated tutorials &nbsp;•&nbsp; ❌ Monetization

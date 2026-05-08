# Project Status

**Version:** 0.1.0 &nbsp;•&nbsp; **As of:** May 2026

## Platform rollout

| # | Platform | Plan status | Actual status | Distribution |
|---|----------|-------------|---------------|--------------|
| 1 | **Android** | Primary | Code-complete (engine, all screens, Room save, AI). Not signed/released. | Not on Play Store |
| 2 | **Windows desktop** | Compose Desktop port | Code-complete via shared `:app-desktop` JVM module | No installer yet |
| 3 | **Web (browser)** | Phase 3 | **Most polished surface.** Full UX pass: home, setup, game, result, settings, about, in-game menu, AI thinking pill, paper-style board, PWA manifest, service worker, localStorage save/resume, WebAudio SFX | Runs locally from `app-web/build/dist/...` |
| 4 | **Linux desktop** | Same JVM binary as Windows | Builds and runs via `:app-desktop` | No `.deb` / `.rpm` yet |
| 5 | **Apple (iOS / macOS)** | Phase 5 | Not started | — |

## Subsystem health

| Subsystem | Status | Notes |
|-----------|--------|-------|
| Game engine (`:engine`) | ✅ Solid | Pure-Kotlin KMP module, immutable state, Result-based API |
| Capture detection | ✅ Solid | Flood-fill, 4-conn interior, 8-conn for adjacency in tests |
| Score calculation | ✅ Solid | Both variants (Territory Area / Captured Dots) |
| Legal-move checking | ✅ Solid | O(cols×rows) scan |
| Game-over detection | ✅ Solid | No-legal-moves and surrender both handled |
| Move history / undo | ✅ Solid | `MoveHistory` + `UndoManager` in engine |
| AI — Easy | ✅ Done | Random with adjacency preference |
| AI — Medium | ✅ Done | 1-ply greedy using shared `BoardEvaluator` |
| AI — Hard | ✅ Done | Minimax depth-4 + α-β + move ordering + 2 s deadline |
| AI evaluator | ✅ Rebuilt | BFS-distance-from-border heuristic; goal-aligned (see `IMPLEMENTATION_VS_PLAN.md`) |
| Session orchestration (`:session`) | ✅ Done | KMP module; `LocalGameSession` drives engine and AI, emits events |
| Android UI | ✅ Done | All planned screens + history screen; Hilt + Room + Compose Navigation |
| Desktop UI | ✅ Done | Compose Desktop window, board canvas, setup dialog |
| Web UI | ✅ Done + extra polish | See above |
| Persistence — Android | ✅ Done | Room (`AppDatabase`, entities, DAOs, repository) |
| Persistence — Desktop | ⚠️ Partial | In-memory session only; no file-based save yet |
| Persistence — Web | ✅ Done | localStorage with replay-based resume |
| Sound effects | ✅ Web only | WebAudio synth (place/capture/win/click); not on Android/Desktop |
| Theming | ✅ Done | Material3 light/dark on Android; paper aesthetic on web |
| Accessibility | ⚠️ Basic | Touch targets ≥44 px; full content descriptions and color-blind fallbacks not yet audited |
| CI/CD | ⚠️ Minimal | Single workflow at `.github/workflows/build.yml`; no per-platform matrix or release pipeline |

## Test coverage

| Test file | Covers |
|-----------|--------|
| [LegalMoveCheckerTest.kt](../engine/src/commonTest/kotlin/territories/engine/LegalMoveCheckerTest.kt) | Legality rules |
| [CaptureDetectorTest.kt](../engine/src/commonTest/kotlin/territories/engine/CaptureDetectorTest.kt) | Capture flood-fill (square, L-shape, border-touching, multi-capture, staircase) |
| [GameEngineIntegrationTest.kt](../engine/src/commonTest/kotlin/territories/engine/GameEngineIntegrationTest.kt) | End-to-end move application |
| [AiPlayerTest.kt](../engine/src/commonTest/kotlin/territories/engine/AiPlayerTest.kt) | Each AI returns legal moves; doesn't crash on crowded boards |

There are **no UI tests** (Compose, Espresso, or Playwright) yet, and no **session-module** tests.

## Diagnostic tooling

- `./gradlew :engine:runSimulation` — runs Medium-vs-Medium for 40 turns and prints a per-turn breakdown of the evaluator. Useful for catching evaluator regressions where the AI starts blobbing instead of building walls.
- `AiLog.enabled` toggles per-move logs on JS (`console.log`) and JVM (`println`).

## What "v1.0" means in scope

For a 1.0 release we still need:
- Signed Android build + Play Store listing
- Windows installer (jpackage / Conveyor)
- Web build deployed to a static host
- Basic accessibility audit
- A short rules tutorial pass beyond the existing How-To-Play screen

Everything else listed in the planning docs is either done or explicitly "future" (online multiplayer, MCTS Expert AI, iOS, Snap Store).

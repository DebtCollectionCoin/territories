# Current State

Snapshot: HEAD `b7165ed` on `main` (May 2026). Released version `0.3.0`
(versionCode 3). Modules wired in `settings.gradle.kts`:
`:engine`, `:session`, `:ranking`, `:shared-ui`, `:app-android`,
`:app-desktop`, `:app-web`.

Legend: ✅ shipped &nbsp;•&nbsp; 🟡 partial &nbsp;•&nbsp; ⏳ not started

---

## 1. `:engine` — game core (KMP commonMain) ✅

26 source files + 6 test files. Targets JVM (used by Android / desktop /
server / tests) and JS (web). Pure-data `model/`, rule logic in
`engine/`, AI in `ai/`, history in `history/`.

| Area | State | Notes |
|------|-------|-------|
| Data models | ✅ | `Player {A,B,C,D,NONE}`, `Coord`, `Cell`, `Board`, `Territory`, `GameState`, `GameConfig` (with `playerCount` + per-seat `playerXType`), `Score` (4 fields + `forPlayer()`), `Move`, `ScoringVariant`, `GamePhase`, `PlayerType` |
| Legal moves | ✅ | `LegalMoveChecker` |
| Capture | ✅ | `CaptureDetector` — flood fill, 4-conn interior, dot absorption rule |
| Engine API | ✅ | `GameEngine.applyMove / undo / initialState` — N-player turn rotation via `state.players` |
| Scoring | ✅ | `ScoreCalculator` — both variants, all seats |
| Game over | ✅ | `GameOverDetector` — N-player passes / elimination |
| Undo / history | ✅ | `UndoManager`, `MoveHistory` |
| Easy AI | ✅ | random legal move |
| Medium AI | ✅ | 1-ply greedy on `BoardEvaluator`; works for any N |
| Hard AI | ✅ | iterative-deepening **paranoid** minimax + α-β + deadline; works for any N (root maximises, every other seat minimises) |
| Evaluator | ✅ | BFS-from-border heuristic generalised: `realScore = own − maxOther`, "opponent" is `state.players − root`, BFS uses `Set<Player>` blockers |
| Bench / sim | ✅ | `:engine:runSimulation`, `:engine:runBenchmark -Pbench.mode=hard\|medium\|ffa3\|ffa4` |

**Tests:** `AiPlayerTest` (incl. FFA 3- and 4-player legal-move tests),
`CaptureDetectorTest`, `FfaEngineTest`, `GameEngineIntegrationTest`,
`HistoryReplayTest`, `LegalMoveCheckerTest`. All green.

---

## 2. `:session` — engine façade for UIs ✅

5 files. `GameSession` interface + `LocalGameSession` implementation +
`GameSessionFactory`. `GameEvent` stream emits state transitions. Used
by Android/desktop/web ViewModels so they don't talk to `GameEngine`
directly.

---

## 3. `:shared-ui` — KMP shared UI helpers 🟡

4 files. Ships `Palette` (LIGHT + DARK with playerC/D entries) and
`BoardSemantics.buildBoardDescription` (a11y). **Not yet** shipping a
shared `BoardCanvas` — each platform still has its own renderer. See
[SHARED_UI_PLAN.md](SHARED_UI_PLAN.md).

---

## 4. `:ranking` — TrueSkill ✅ (offline only)

6 files (`Gaussian`, `Rating`, `MatchOutcome`, `TrueSkillUpdater` +
tests). Pure KMP commonMain — no persistence yet. Ready to be wired
into a server (Phase D).

---

## 5. `:app-android` — Android client ✅ (offline)

26 files. Compose + Hilt + Room. Released to Play Store as v0.2.0;
v0.3.0 AAB built locally (FFA hot-seat + paranoid AI), pending upload.

| Screen | State |
|--------|-------|
| Home, How-to-play, History, Settings, Result | ✅ |
| Setup | ✅ — Players (2/3/4), per-seat Human/Easy/Med/Hard, dynamic First Player |
| Game | ✅ — `BoardCanvas` renders all 4 colours; HUD shows up to 4 score panels; TurnIndicator handles all seats |
| ViewModel | ✅ — `aiBySeat: Map<Player, AiPlayer?>` per-seat AI dispatch |

Persistence: Room `AppDatabase` v2 (`finalScoreA..D`); destructive
migration on schema bump. `AppPreferencesRepository` persists 9 setup
prefs (board / scoring / players / 4 seat types / opponent / first).

Build: `./gradlew :app-android:assembleDebug` ✅. SDK at
`/home/eli/Android/Sdk` via `local.properties` (gitignored).

**Pending:** pinch-to-zoom verified for largest 60×40 preset. Release
build + signing for next Play Store update.

---

## 6. `:app-desktop` — Compose Desktop ✅ (offline)

7 files. Mouse + keyboard (Ctrl+Z, Ctrl+N, Esc). `SavedGameStore`
auto-saves after every move. `SetupDialog` exposes Human/Easy/Med/Hard
for every FFA seat. Packaging: `packageDeb` / `packageDmg` /
`packageMsi` configured. `.deb` builds locally; signing keys not set up.

---

## 7. `:app-web` — Kotlin/JS + canvas ✅ (offline)

6 Kotlin files + hand-rolled `index.html`/`styles.css`/PWA manifest +
service worker. Deployed via `.github/workflows/deploy-pages.yml` to
GitHub Pages. URL fragment `#g=...` carries shareable game state.
4-player setup with full Human/Easy/Med/Hard per seat. Responsive +
HiDPI canvas.

---

## 8. CI / release pipeline

| Workflow | Status |
|----------|--------|
| `.github/workflows/build.yml` | ✅ Gradle build on PR/push |
| `.github/workflows/deploy-pages.yml` | ✅ web → GitHub Pages on push to `main` |
| Android Play Store | 🟡 v0.2.0 published; v0.3.0 AAB built locally, awaiting Play Console upload |
| Desktop installers | 🟡 build locally; no release artefacts published |
| Web PWA | ✅ live |

---

## 9. Multiplayer roadmap (`MULTIPLAYER_DESIGN.md`)

| Phase | Target | State |
|-------|--------|-------|
| A — N-player engine refactor | v0.3.0 | ✅ commit `5ee4bed` |
| B — Local hot-seat 4-player UI (all 3 platforms) | v0.4.0 | ✅ commits `2a80e1d` (desktop), `cc9a451` (web), `22bfc5e` (Android) |
| C — Ranking module (TrueSkill, offline) | v0.5.0 | ✅ commit `ba4e69f` |
| FFA AI (paranoid minimax) | v0.5.x | ✅ commit `e01e0b3` — **done ahead of schedule** |
| D — Ktor server skeleton + Postgres | v0.6.0 | 🟡 in progress — `:protocol` + `:server` modules added; in-memory lobby/game registry; WebSocket `/ws` with Welcome/CreateLobby/JoinLobby/SubmitMove/MoveApplied/MoveRejected/GameEnded; Postgres + auth deferred |
| E — Online real-time 4-player + matchmaking | v0.7.0 | ⏳ |
| F — Async correspondence | v0.8.0 | ⏳ |
| G — Polish + leaderboards | v1.0.0 | ⏳ |

---

## 10. Immediately-next candidates

1. **Cut v0.3.0** — ✅ done. `version.properties` bumped to
   `versionCode=3 / versionName=0.3.0`, wired via
   `app-android/build.gradle.kts` (no more hardcoded `1.0.0`). Release
   notes added to `PLAY_STORE_LISTING.md`. AAB at
   `app-android/build/outputs/bundle/release/app-android-release.aab`
   (7 MB). Upload to Play Console manually.
2. **Strength check** — ✅ done. `runBenchmark -Pbench.mode=ffa3 --games
   6 --depth 2 --time-ms 400` shows skill ladder holds (Hard 4/6 vs
   2 Easy; Hard 5/6 vs Medium across mixed matches), no seat bias in
   all-Hard self-play. AI is not over-defending. Production tuning
   wants longer budgets / larger boards (60-move cap is hit).
3. **Phase D bootstrap** — ✅ skeleton landed. New `:protocol` (KMP)
   and `:server` (JVM) modules. Server reuses `:engine` and `:ranking`
   directly; in-memory lobby/game registry; WebSocket `/ws` exchanges
   `Welcome` / `CreateLobby` / `JoinLobby` / `SubmitMove` /
   `MoveApplied` / `MoveRejected` / `GameStarted` / `GameEnded`. Run
   via `./gradlew :server:run` (port 9000). Smoke tests pass. Postgres
   schema, auth, ranked-rating updates, and reconnect/resume deferred
   to Phase E.
4. **Shared `BoardCanvas`** — finish migrating Android/desktop renderers
   into `:shared-ui` (web stays bespoke for design control). Plan in
   [SHARED_UI_PLAN.md](SHARED_UI_PLAN.md).

---

## 11. Confirmed non-goals (current cycle)

❌ Online play (until Phase D) ❌ Accounts / OAuth ❌ Monetisation
❌ Animated tutorials ❌ iOS / macOS native (deferred indefinitely)
❌ Code-signing certificates for desktop installers

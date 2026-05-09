# Known Issues & Next Steps

## Open issues / risks

| Severity | Area | Issue |
|----------|------|-------|
| 🟡 Medium | AI tuning | Heuristic was empirically tuned via `:engine:runBenchmark` (proximity → 0, frontier → 0, added `componentPressure`). New defaults beat the old heuristic 25-16 over 60 games. Re-run benchmark if human play-test reports regressions. |
| 🟡 Medium | Kotlin incremental cache | The IC cache for `:engine` corrupted twice during the AI rewrite, silently running stale compiled output. Workaround: `rm -rf engine/build/kotlin` (or full `engine/build`). Watch for repeats; consider disabling IC for that module if it recurs. |
| 🟢 Low | Shared UI | Per-platform UI duplication (Android Compose vs Desktop Compose vs JS/HTML). Refactoring into a `shared-ui` Compose Multiplatform module would shrink Desktop+Android maintenance, but web would still be hand-rolled. Defer until after first release. |
| 🟢 Low | Web | URL-share via `#g=` fragment now ships, but PWA offline cache still untested in production. |

## Recommended next steps (rough priority order)

1. **Web deployment** — deploy the existing `jsBrowserDistribution` output to a public host (Cloudflare Pages / GitHub Pages / Netlify) and wire to a CI release job. URL-share links require a stable origin to be useful.
2. **Android signing + Play Store internal track** — keystore + listing assets are documented; finish the upload.
3. **Human play-test the tuned AI** on the deployed web build, look for regressions.
4. **Compose Multiplatform shared-ui module** — only worth it once the per-platform UIs diverge in painful ways.
5. **Hard AI depth tuning** — the depth=4 / 2000 ms budget was set before the new heuristic; revisit with `:engine:runBenchmark` once Hard AI matches are run head-to-head.

## Things deliberately deferred

- **Compose Multiplatform iOS** — gated on stability; revisit when the rest is shipped.
- **MCTS Expert AI** — `06_ai_opponent.md` lists this as future. The tuned BFS-gradient evaluator already plays competently at depth 4, so this is a polish item, not a release blocker.
- **Online multiplayer** — explicit non-goal for v1.0.

## Resolved (recently completed)

- ✅ Iterative deepening for Hard AI
- ✅ Session-module coroutine tests (11 passing)
- ✅ Desktop save/load (JSON in user-data dir)
- ✅ Accessibility pass (color-blind shape, `liveRegion` board announcements, content descriptions)
- ✅ Linux packaging (jpackage `.deb` + `.rpm`)
- ✅ Android signing + Play internal track docs
- ✅ AI heuristic tuned for local encirclement (commit `d3b19b5`)
- ✅ History replay E2E test (`HistoryReplayTest` in `:engine:jvmTest`)
- ✅ Web URL-share via `#g=...` fragment + Copy share link button

## How to verify a release candidate

```bash
# unit + AI tests
./gradlew :engine:jvmTest :session:jvmTest

# AI sanity simulation (Medium vs Medium, 40 turns)
./gradlew :engine:runSimulation

# AI weight benchmark (optional, for heuristic changes)
./gradlew :engine:runBenchmark -Pbench.args="--games 30 --moves 80"

# build all surfaces
./gradlew :app-android:assembleDebug \
          :app-desktop:run \
          :app-web:jsBrowserDevelopmentExecutableDistribution
```

A green run of the first two and a successful build of all three is the current minimum bar before tagging a version.

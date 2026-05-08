# Known Issues & Next Steps

## Open issues / risks

| Severity | Area | Issue |
|----------|------|-------|
| 🟡 Medium | AI tuning | New BFS-distance evaluator validated by simulation, but human play-test feedback may show edge cases (e.g. AI over-committing to perimeter walls and ignoring center pressure). Tune weights with `runSimulation` if reported. |
| 🟡 Medium | Kotlin incremental cache | The IC cache for `:engine` corrupted twice during the AI rewrite, silently running stale compiled output. Workaround: `rm -rf engine/build/kotlin` (or full `engine/build`). Watch for repeats; consider disabling IC for that module if it recurs. |
| 🟡 Medium | Desktop persistence | `:app-desktop` keeps state in memory only — closing the window loses an in-progress game. |
| 🟢 Low | Shared UI | Per-platform UI duplication (Android Compose vs Desktop Compose vs JS/HTML). Refactoring into a `shared-ui` Compose Multiplatform module would shrink Desktop+Android maintenance, but web would still be hand-rolled. Defer until after first release. |
| 🟢 Low | Accessibility | No content descriptions or color-blind alternative shape on dots yet. |
| 🟢 Low | History (Android) | Bonus screen exists but no E2E test that saved games actually replay correctly through `GameEngine`. |
| 🟢 Low | Web | No URL-share / no deployed instance. PWA installable from `localhost` but offline cache untested in production. |

## Recommended next steps (rough priority order)

1. **Human play-test the new AI** on the web build. Decide whether weights need tuning. Reload `app-web/build/dist/js/developmentExecutable/index.html`.
2. **Wire desktop save/load** — pick SQLDelight or a JSON file under the user-data dir; reuse the move-replay approach already used by Room and the web `SavedGame`.
3. **Iterative deepening** for Hard AI (`AI-07`). With the deadline already in place this is mostly a wrapping loop around minimax; gives more consistent move quality on slow devices.
4. **Web deployment** — pick a host (Cloudflare Pages / GitHub Pages / Netlify), wire the existing `jsBrowserDistribution` output to a CI release job.
5. **Android signing + Play Store internal track** — version is already at `0.1.0` in [version.properties](../version.properties); needs keystore + listing assets.
6. **Accessibility pass** — content descriptions on Android composables, alt-shape for dots in color-blind mode (square for B, circle for A), high-contrast toggle in Settings.
7. **Session-module tests** — `LocalGameSession` is now the orchestration layer for two of three apps; it should have its own coroutine-based tests.
8. **Linux packaging** — `jpackage` `.deb` and `.rpm` from the existing desktop JVM jar. Cheap once Windows installer flow exists.

## Things deliberately deferred

- **Compose Multiplatform iOS** — gated on stability; revisit when the rest is shipped.
- **MCTS Expert AI** — `06_ai_opponent.md` lists this as future. The new BFS-gradient evaluator already plays competently at depth 4, so this is a polish item, not a release blocker.
- **Online multiplayer** — explicit non-goal for v1.0.

## How to verify a release candidate

```bash
# unit + AI tests
./gradlew :engine:jvmTest

# AI sanity simulation (Medium vs Medium, 40 turns)
./gradlew :engine:runSimulation

# build all surfaces
./gradlew :app-android:assembleDebug \
          :app-desktop:run \
          :app-web:jsBrowserDevelopmentExecutableDistribution
```

A green run of the first two and a successful build of all three is the current minimum bar before tagging a version.

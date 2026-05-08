# Territories

A digital adaptation of **Židi** (also known as *obkličovačka, území, puntíkovaná*) — a classic Czech pen-and-paper strategy game for two players. Players alternate placing dots on a grid; surround your opponent's dots to capture territory. The player with the most captured territory wins.

## Platforms

| Platform | Status |
|----------|--------|
| Android | Code-complete |
| Web (browser) | Code-complete — paper-on-table aesthetic, PWA-ready |
| Desktop (Windows / Linux) | Code-complete via Compose Multiplatform |
| iOS / macOS | Planned |

## Project structure

```
engine/          Pure-Kotlin KMP game engine (shared by all platforms)
session/         KMP session orchestration layer (drives engine + AI)
app-android/     Android app (Jetpack Compose + Hilt + Room)
app-desktop/     Desktop app (Compose Multiplatform)
app-web/         Web app (Kotlin/JS + HTML Canvas)
```

## Building

**Prerequisites:** JDK 17+, Android SDK (for Android target only).

```bash
# Engine tests
./gradlew :engine:jvmTest

# AI simulation (Medium vs Medium, 40 turns)
./gradlew :engine:runSimulation

# Web development build
./gradlew :app-web:jsBrowserDevelopmentExecutableDistribution
# Output: app-web/build/dist/js/developmentExecutable/index.html

# Desktop (run directly)
./gradlew :app-desktop:run

# Android debug APK
./gradlew :app-android:assembleDebug
```

## Game rules (summary)

- Players alternate placing one dot per turn on any empty, unclaimed intersection.
- A player captures territory by completely surrounding opponent dots — no path from the enclosed region can reach the board border without crossing your dots.
- The board border protects dots connected to it; they can never be captured.
- Use a **staircase pattern** (not a straight diagonal) to build gap-free walls.
- Game ends when no legal moves remain, or a player surrenders.
- Winner: most territory area (default) or most captured opponent dots (alternate scoring).

Full rules: [01_game_rules_specification.md](01_game_rules_specification.md)

## AI

Three difficulty levels share a common engine:

- **Easy** — random legal move
- **Medium** — 1-ply greedy heuristic
- **Hard** — minimax depth-4 with α-β pruning, 2 s deadline, and a BFS-distance-from-border evaluator that rewards genuine wall-building over clustering

## License

MIT — see [LICENSE](LICENSE)

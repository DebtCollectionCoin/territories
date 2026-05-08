# Android Implementation Plan

> **Target:** Android 8.0+ (API 26+), phone and tablet
> **Language:** Kotlin
> **UI framework:** Jetpack Compose
> **Architecture:** MVVM + Clean Architecture (Use Cases)

---

## Project Structure

```
territories-android/
├── app/
│   ├── src/main/kotlin/territories/
│   │   ├── MainActivity.kt
│   │   ├── di/                      # Dependency injection (Hilt)
│   │   ├── ui/
│   │   │   ├── navigation/
│   │   │   │   └── AppNavGraph.kt
│   │   │   ├── screens/
│   │   │   │   ├── home/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   ├── game/
│   │   │   │   │   ├── GameScreen.kt
│   │   │   │   │   ├── GameViewModel.kt
│   │   │   │   │   ├── BoardCanvas.kt      # Custom Canvas composable
│   │   │   │   │   └── GameHud.kt          # Score, turn, controls overlay
│   │   │   │   ├── settings/
│   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   └── SettingsViewModel.kt
│   │   │   │   └── results/
│   │   │   │       └── ResultScreen.kt
│   │   │   └── theme/
│   │   │       ├── Theme.kt
│   │   │       ├── Color.kt
│   │   │       └── Type.kt
│   │   ├── domain/
│   │   │   ├── usecase/
│   │   │   │   ├── StartGameUseCase.kt
│   │   │   │   ├── PlaceDotUseCase.kt
│   │   │   │   ├── UndoMoveUseCase.kt
│   │   │   │   ├── SurrenderUseCase.kt
│   │   │   │   └── SaveGameUseCase.kt
│   │   │   └── repository/
│   │   │       └── GameRepository.kt       # Interface
│   │   └── data/
│   │       ├── repository/
│   │       │   └── GameRepositoryImpl.kt
│   │       ├── persistence/
│   │       │   ├── AppDatabase.kt          # Room database
│   │       │   ├── GameDao.kt
│   │       │   └── MoveDao.kt
│   │       └── ai/
│   │           └── AiPlayer.kt             # Wraps engine AI module
└── engine/                                 # Shared game engine module (pure Kotlin)
```

---

## Gradle Setup

```kotlin
// settings.gradle.kts
include(":app", ":engine")

// app/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.x" }
}

dependencies {
    implementation(project(":engine"))

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.xx.xx")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.x")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.x")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.x")
    kapt("com.google.dagger:hilt-compiler:2.x")
    implementation("androidx.hilt:hilt-navigation-compose:1.x")

    // Room
    implementation("androidx.room:room-runtime:2.x")
    implementation("androidx.room:room-ktx:2.x")
    kapt("androidx.room:room-compiler:2.x")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.x")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.x")
}
```

---

## Implementation Tasks

### TASK A-01 — Project Bootstrap
- [ ] Create new Android project in Android Studio
- [ ] Add `:engine` module, link from `:app`
- [ ] Configure Hilt, Room, Compose, Navigation
- [ ] Set min SDK to 26, target to 35
- [ ] Add `AppDatabase`, basic Hilt `AppModule`
- [ ] Verify project builds and empty Compose screen appears

---

### TASK A-02 — Navigation Graph
**File:** `ui/navigation/AppNavGraph.kt`

Screens and routes:
```
HOME → NEW_GAME_SETUP → GAME → RESULT
HOME → SETTINGS
HOME → GAME_HISTORY (future)
```

```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NewGameSetup : Screen("new_game_setup")
    object Game : Screen("game/{gameId}") {
        fun withId(id: String) = "game/$id"
    }
    object Result : Screen("result/{gameId}") {
        fun withId(id: String) = "result/$id"
    }
    object Settings : Screen("settings")
}
```

- [ ] Implement NavHost with all routes
- [ ] Pass game config through navigation arguments or shared ViewModel
- [ ] Back-stack behavior: Game → Result replaces Game (no back to mid-game)

---

### TASK A-03 — Home Screen
**File:** `ui/screens/home/HomeScreen.kt`

Elements:
- App logo / title "Territories"
- **New Game** button → navigates to NewGameSetup
- **Resume Game** button (if saved game exists) → navigates to Game
- **Settings** button → navigates to Settings
- **How to Play** button → simple rules dialog/screen

- [ ] Implement HomeScreen composable
- [ ] HomeViewModel checks Room for in-progress game
- [ ] Show/hide Resume button based on saved state

---

### TASK A-04 — New Game Setup Screen

Settings shown before starting:
- Board size selector (Small / Medium / Large / Custom)
- Scoring variant toggle (Area / Captured Dots)
- Player B type (Human / AI Easy / AI Medium / AI Hard)
- First player (Player 1 / Random)

- [ ] `SettingsScreen` composable with Material3 components
- [ ] Persist last-used settings via DataStore Preferences
- [ ] **Start Game** button applies config → creates GameState → navigates to Game

---

### TASK A-05 — Game Board Canvas (Critical Task)
**File:** `ui/screens/game/BoardCanvas.kt`

The board is rendered using a Jetpack Compose `Canvas` composable.

**Rendering layers (back to front):**
1. Grid lines (thin, neutral color)
2. Territory fills (semi-transparent colored rectangles for each captured region)
3. Dots (filled circles, Player A = color A, Player B = color B)
4. Last-move highlight (ring around the most recently placed dot)
5. Interaction hint (highlight legal tap target on hover/press)

**Touch handling:**
```kotlin
Canvas(
    modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                val coord = offsetToCoord(offset, cellSize)
                viewModel.onCellTapped(coord)
            }
        }
) {
    // drawing code
}
```

**Coordinate math:**
```kotlin
val cellSize = size.width / (board.cols - 1)  // pixels per grid unit
fun coordToOffset(coord: Coord): Offset =
    Offset(coord.col * cellSize, coord.row * cellSize)
fun offsetToCoord(offset: Offset, cellSize: Float): Coord =
    Coord(
        col = (offset.x / cellSize).roundToInt().coerceIn(0, board.cols - 1),
        row = (offset.y / cellSize).roundToInt().coerceIn(0, board.rows - 1)
    )
```

- [ ] Grid rendering (draw lines for all rows and columns)
- [ ] Territory fill rendering (use `drawRect` per territory bounding box, or polygon fill)
- [ ] Dot rendering (draw circles at intersections)
- [ ] Touch → Coord conversion with snap-to-nearest-intersection
- [ ] Last move indicator
- [ ] Pinch-to-zoom + pan (for large boards — use `transformable` modifier)
- [ ] Accessibility: content descriptions for screen readers

---

### TASK A-06 — Game HUD (Heads-Up Display)
**File:** `ui/screens/game/GameHud.kt`

Persistent overlay showing:
- Player A score | current turn indicator | Player B score
- Move counter
- **Undo** button (greyed out if no history)
- **Surrender** button (with confirmation dialog)
- **Menu** button (pause: save & quit, rules reference)

- [ ] Implement HUD as a composable overlaid on BoardCanvas
- [ ] Animate score changes
- [ ] Show active player highlight
- [ ] Confirmation dialogs for Surrender and Quit

---

### TASK A-07 — Game ViewModel
**File:** `ui/screens/game/GameViewModel.kt`

```kotlin
@HiltViewModel
class GameViewModel @Inject constructor(
    private val placeDotUseCase: PlaceDotUseCase,
    private val undoMoveUseCase: UndoMoveUseCase,
    private val surrenderUseCase: SurrenderUseCase,
    private val saveGameUseCase: SaveGameUseCase,
    private val aiPlayer: AiPlayer
) : ViewModel() {

    val uiState: StateFlow<GameUiState>

    fun onCellTapped(coord: Coord)
    fun onUndo()
    fun onSurrender()
    fun onSaveAndQuit()
}
```

**AI turn flow:**
```kotlin
fun onCellTapped(coord: Coord) {
    if (currentState.currentPlayer == humanPlayer) {
        applyHumanMove(coord)
        if (!state.isGameOver && currentPlayer == aiPlayer) {
            launchAiMove()
        }
    }
}

private fun launchAiMove() {
    viewModelScope.launch(Dispatchers.Default) {
        val aiCoord = aiPlayer.selectMove(currentState)
        withContext(Dispatchers.Main) {
            applyMove(aiCoord)
        }
    }
}
```

- [ ] StateFlow for UI state
- [ ] Human move handling
- [ ] AI move on background coroutine
- [ ] Auto-save after each move (Room)
- [ ] Game over detection → navigate to ResultScreen

---

### TASK A-08 — Result Screen
**File:** `ui/screens/results/ResultScreen.kt`

Shows:
- Winner announcement (or "Draw")
- Final scores (both variants shown)
- Summary stats: total moves, game duration, largest territory
- **Play Again** (same config) / **New Game** (go to setup) / **Home** buttons

---

### TASK A-09 — Persistence (Room)
**File:** `data/persistence/`

- [ ] Define `AppDatabase` with `GameEntity` and `MoveEntity`
- [ ] `GameDao`: insert, update (winner/scores), query in-progress game
- [ ] `MoveDao`: insert move, query all moves for a game (for replay)
- [ ] `GameRepositoryImpl` wraps DAOs, exposes domain-friendly interface
- [ ] Auto-save: save each `Move` to Room after it's applied

---

### TASK A-10 — Settings Persistence (DataStore)
- [ ] Store `GameConfig` preferences via `DataStore<Preferences>`
- [ ] Load on `NewGameSetup` screen
- [ ] Save when user changes any setting

---

### TASK A-11 — How to Play Screen
- [ ] Static composable with scrollable rules text
- [ ] Include visual diagram of staircase pattern
- [ ] Include visual showing valid vs. invalid encirclement
- [ ] Link from Home and from in-game menu

---

## Android-Specific Requirements

| Requirement | Implementation |
|-------------|----------------|
| Portrait + Landscape | Board canvas scales, HUD repositions |
| Tablet support | Board takes left 70%, HUD on right sidebar |
| Dark mode | Material3 dynamic color, dark theme |
| Back button | Pauses game, offers save confirmation |
| App backgrounded | Auto-save current state to Room |
| Screen rotation | GameState survives via ViewModel |
| Accessibility | TalkBack labels on all interactive elements |
| Min API 26 | No API 26+ exclusive APIs without checks |

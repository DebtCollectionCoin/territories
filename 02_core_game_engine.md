# Core Game Engine

> **Agent Instructions:** Implement this module in pure Kotlin with zero Android dependencies.
> It must be a standalone JVM/KMP module: `engine/` or `:game-engine` Gradle module.
> 100% unit-testable. No UI, no persistence, no platform code here.

---

## Responsibilities

The game engine owns:
- Board state representation
- Legal move generation
- Capture detection (flood-fill algorithm)
- Territory tracking
- Score calculation (both variants)
- Game-over detection
- Full game history (for undo)

---

## Module Structure

```
engine/
├── src/main/kotlin/territories/engine/
│   ├── model/
│   │   ├── Dot.kt             # Value: EMPTY, PLAYER_A, PLAYER_B
│   │   ├── Cell.kt            # Intersection state: dot + territory owner
│   │   ├── Board.kt           # 2D grid, immutable snapshot
│   │   ├── GameState.kt       # Full game state (board + scores + turn + phase)
│   │   ├── Move.kt            # A single placement action (col, row, player)
│   │   ├── Territory.kt       # A captured region: owner + set of cell coords
│   │   └── GameConfig.kt      # Board size, scoring variant, etc.
│   ├── engine/
│   │   ├── GameEngine.kt      # Public API — apply moves, query state
│   │   ├── CaptureDetector.kt # Flood-fill capture logic
│   │   ├── LegalMoveChecker.kt
│   │   ├── ScoreCalculator.kt
│   │   └── GameOverDetector.kt
│   └── history/
│       ├── MoveHistory.kt     # Stack of GameState snapshots
│       └── UndoManager.kt
└── src/test/kotlin/territories/engine/
    ├── CaptureDetectorTest.kt
    ├── LegalMoveCheckerTest.kt
    ├── ScoreCalculatorTest.kt
    └── GameEngineIntegrationTest.kt
```

---

## Step-by-Step Implementation Tasks

### TASK E-01 — Define Core Data Models
**File:** `model/`

```kotlin
enum class Player { A, B, NONE }

data class Coord(val col: Int, val row: Int)

data class Cell(
    val dot: Player,           // who placed a dot here (NONE = empty)
    val territory: Player      // which player owns this territory (NONE = unclaimed)
)

// Board is immutable; return new Board on each mutation
class Board(val cols: Int, val rows: Int) {
    fun get(coord: Coord): Cell
    fun place(coord: Coord, player: Player): Board
    fun claimTerritory(cells: Set<Coord>, owner: Player): Board
    fun isOnBoard(coord: Coord): Boolean
    fun isBorderCell(coord: Coord): Boolean
}

data class GameState(
    val board: Board,
    val currentPlayer: Player,
    val territories: List<Territory>,
    val moveCount: Int,
    val isGameOver: Boolean,
    val winner: Player,       // NONE = draw or in progress
    val config: GameConfig
)

data class GameConfig(
    val cols: Int = 61,
    val rows: Int = 41,
    val scoringVariant: ScoringVariant = ScoringVariant.TERRITORY_AREA,
    val firstPlayer: Player = Player.A
)

enum class ScoringVariant { TERRITORY_AREA, CAPTURED_DOTS }
```

**Acceptance criteria:**
- [ ] All models are immutable data classes
- [ ] Board has no mutable state
- [ ] Unit tests for Board.place(), Board.claimTerritory(), Board.isBorderCell()

---

### TASK E-02 — Legal Move Checker
**File:** `engine/LegalMoveChecker.kt`

```kotlin
class LegalMoveChecker {
    fun isLegal(coord: Coord, state: GameState): Boolean
    fun allLegalMoves(state: GameState): List<Coord>
    fun hasLegalMoves(state: GameState): Boolean
}
```

An intersection is legal if:
- `cell.dot == Player.NONE` AND
- `cell.territory == Player.NONE`

**Acceptance criteria:**
- [ ] Returns false for any occupied cell
- [ ] Returns false for any cell inside any territory
- [ ] Returns all empty, non-territory cells as legal
- [ ] Performance: O(cols × rows) at worst

---

### TASK E-03 — Capture Detector (Core Algorithm)
**File:** `engine/CaptureDetector.kt`

This is the most complex and critical piece. Use flood-fill.

```kotlin
class CaptureDetector {
    // Returns list of territories captured by `player` after placing dot at `coord`
    fun detectCaptures(coord: Coord, player: Player, board: Board): List<CapturedRegion>
}

data class CapturedRegion(
    val cells: Set<Coord>,         // all cells inside the captured area
    val capturedDots: Set<Coord>,  // opponent dots within
    val capturedTerritories: List<Territory>  // previously captured regions now absorbed
)
```

**Algorithm:**

```
1. Get all 8-directional neighbors of `coord` that are NOT owned by `player`
2. For each neighbor that is:
     - an opponent dot, OR
     - an empty cell, OR
     - an opponent's territory cell:
   
   → Start a flood-fill from that neighbor
   → Flood-fill expands 4-directionally (or 8? see note below)
     through: empty cells, opponent dots, opponent territory cells
   → Flood-fill is BLOCKED by: player's own dots, player's own territory cells, board edge
   
3. If the flood-fill does NOT reach the board border:
   → All cells in the flood-fill region are CAPTURED
   → Create a CapturedRegion from these cells

4. If the flood-fill DOES reach the board border:
   → Not captured
```

> **Implementation Note on Flood-Fill Expansion Direction:**
> The original rules define the encirclement loop using 8-directional adjacency for the RING of dots.
> The interior flood-fill should use 4-directional expansion to correctly detect enclosed regions.
> Test thoroughly with diagonal-chain edge cases.

**Acceptance criteria:**
- [ ] Simple square encirclement detected correctly
- [ ] L-shaped encirclement detected correctly
- [ ] Dots touching the border are NOT captured
- [ ] Re-capture (opponent's territory enclosed) detected correctly
- [ ] Multiple captures from one placement all detected
- [ ] No false positives when chain has a gap
- [ ] Diagonal staircase chain correctly blocks (see rules §7.1)

---

### TASK E-04 — Game Engine (Main API)
**File:** `engine/GameEngine.kt`

```kotlin
class GameEngine(config: GameConfig) {

    val initialState: GameState

    // Apply a move; returns new GameState (immutable)
    fun applyMove(state: GameState, coord: Coord): Result<GameState>

    // Surrender: ends game, opponent wins
    fun surrender(state: GameState): GameState

    // Check for game over (no legal moves)
    fun checkGameOver(state: GameState): GameState
}
```

**Turn flow:**
```
1. Validate move is legal → error if not
2. Place dot on board → new Board
3. Run CaptureDetector → list of CapturedRegions
4. Apply captures to board (mark territory cells)
5. Recalculate scores via ScoreCalculator
6. Check game over via GameOverDetector
7. Toggle currentPlayer
8. Return new GameState
```

**Acceptance criteria:**
- [ ] Illegal moves return `Result.failure` with clear error message
- [ ] State is immutable — original state unchanged after move
- [ ] Full game playthrough test (script a known game, verify final state)

---

### TASK E-05 — Score Calculator
**File:** `engine/ScoreCalculator.kt`

```kotlin
data class Score(val playerA: Int, val playerB: Int)

class ScoreCalculator {
    fun calculate(state: GameState): Score
}
```

**Variant A (Territory Area):**
```
score(player) = sum of all cells where cell.territory == player
```

**Variant B (Captured Dots):**
```
score(player) = sum of all cells where cell.territory == player AND cell.dot == opponent
```

**Acceptance criteria:**
- [ ] Both variants calculate correctly
- [ ] Score is 0–0 on empty board
- [ ] Score updates incrementally after each capture

---

### TASK E-06 — Game Over Detector
**File:** `engine/GameOverDetector.kt`

```kotlin
class GameOverDetector {
    fun isGameOver(state: GameState): Boolean
    fun determineWinner(state: GameState): Player  // NONE = draw
}
```

Game is over when `LegalMoveChecker.hasLegalMoves()` returns false for the current player (or both players — check both).

**Acceptance criteria:**
- [ ] Returns false mid-game when moves exist
- [ ] Returns true when board is full
- [ ] Correctly identifies winner by active scoring variant
- [ ] Handles draw correctly

---

### TASK E-07 — Undo / Move History
**File:** `history/`

```kotlin
class MoveHistory {
    fun push(state: GameState)
    fun pop(): GameState?       // returns previous state or null
    fun peek(): GameState?
    val size: Int
    fun clear()
}
```

Store full GameState snapshots (immutable, so memory-safe).
Limit history depth to last 50 moves by default (configurable).

**Acceptance criteria:**
- [ ] Undo reverts to exact previous state
- [ ] Cannot undo past initial state
- [ ] History cleared on new game

---

### TASK E-08 — Unit Test Suite
**Coverage targets:**

| Component | Target Coverage |
|-----------|----------------|
| CaptureDetector | >95% |
| LegalMoveChecker | 100% |
| ScoreCalculator | 100% |
| GameEngine (integration) | >85% |
| GameOverDetector | 100% |

**Mandatory test cases for CaptureDetector:**
- [ ] 2×2 square encirclement
- [ ] U-shape (not closed — should NOT capture)
- [ ] Staircase chain encirclement
- [ ] Encirclement touching border (should NOT capture)
- [ ] Encirclement of opponent's existing territory (re-capture)
- [ ] Two simultaneous captures from one move
- [ ] Large irregular region
- [ ] Empty enclosed area with no opponent dots (should NOT capture — rule §4.1)

---

## Dependencies

```kotlin
// build.gradle.kts (engine module)
plugins {
    kotlin("jvm")  // or kotlin("multiplatform") for KMP path
}
dependencies {
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.x")
}
```

No Android, no UI, no coroutines required for core engine.
Coroutines can be added in TASK E-09 if AI computation needs to be async.

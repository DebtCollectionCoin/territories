# Data Models & State Representation

> **Agent Instructions:** This document defines all data structures used across the entire project.
> All layers (engine, UI, persistence, AI) must use these models or explicit mappers to/from them.

---

## 1. Core Enums

```kotlin
// engine/model/Player.kt
enum class Player {
    A,     // Human or AI — Player 1
    B,     // Human or AI — Player 2
    NONE   // Used for empty/unclaimed cells
}

fun Player.opponent(): Player = when (this) {
    Player.A -> Player.B
    Player.B -> Player.A
    Player.NONE -> Player.NONE
}

// engine/model/ScoringVariant.kt
enum class ScoringVariant {
    TERRITORY_AREA,   // Count all enclosed cells
    CAPTURED_DOTS     // Count only enclosed opponent dots
}

// engine/model/GamePhase.kt
enum class GamePhase {
    IN_PROGRESS,
    GAME_OVER,
    SURRENDERED
}
```

---

## 2. Coordinate System

```kotlin
// engine/model/Coord.kt
data class Coord(val col: Int, val row: Int) {
    // 4-directional neighbors (orthogonal)
    fun neighbors4(): List<Coord> = listOf(
        Coord(col - 1, row), Coord(col + 1, row),
        Coord(col, row - 1), Coord(col, row + 1)
    )
    // 8-directional neighbors (Moore neighborhood)
    fun neighbors8(): List<Coord> = listOf(
        Coord(col - 1, row - 1), Coord(col, row - 1), Coord(col + 1, row - 1),
        Coord(col - 1, row),                           Coord(col + 1, row),
        Coord(col - 1, row + 1), Coord(col, row + 1), Coord(col + 1, row + 1)
    )
}
```

**Coordinate convention:**
- `col` = 0 is the **left** edge
- `row` = 0 is the **top** edge
- `Coord(0, 0)` = top-left corner (border intersection)
- `Coord(cols-1, rows-1)` = bottom-right corner (border intersection)

---

## 3. Cell

```kotlin
// engine/model/Cell.kt
data class Cell(
    val dot: Player = Player.NONE,           // Who placed a dot here
    val territory: Player = Player.NONE,     // Which player owns this as territory
) {
    val isEmpty: Boolean get() = dot == Player.NONE && territory == Player.NONE
    val isLegal: Boolean get() = isEmpty  // Legal to place a dot here
}
```

**Cell states matrix:**

| dot   | territory | Meaning |
|-------|-----------|---------|
| NONE  | NONE      | Empty — legal to place |
| A     | NONE      | Player A dot, not yet in territory |
| B     | NONE      | Player B dot, not yet in territory |
| NONE  | A         | Empty cell inside Player A's territory |
| NONE  | B         | Empty cell inside Player B's territory |
| A     | A         | Player A dot inside A's own territory |
| B     | A         | Player B dot captured inside A's territory |
| A     | B         | Player A dot captured inside B's territory |
| B     | B         | Player B dot inside B's own territory |

---

## 4. Board

```kotlin
// engine/model/Board.kt
class Board private constructor(
    val cols: Int,
    val rows: Int,
    private val cells: Array<Array<Cell>>  // [col][row]
) {
    companion object {
        fun empty(cols: Int, rows: Int): Board
        fun from(cols: Int, rows: Int, cells: Array<Array<Cell>>): Board
    }

    fun get(coord: Coord): Cell
    fun isOnBoard(coord: Coord): Boolean
    fun isBorder(coord: Coord): Boolean =
        coord.col == 0 || coord.row == 0 ||
        coord.col == cols - 1 || coord.row == rows - 1

    // Returns a NEW Board with the dot placed
    fun withDot(coord: Coord, player: Player): Board

    // Returns a NEW Board with territory claimed for all coords in the set
    fun withTerritory(coords: Set<Coord>, owner: Player): Board

    // Iterate all cells
    fun allCoords(): Sequence<Coord>
    fun cellsOf(player: Player): List<Coord>  // All coords where dot == player
    fun territoryCells(owner: Player): List<Coord>  // All coords where territory == owner
}
```

---

## 5. Territory

```kotlin
// engine/model/Territory.kt
data class Territory(
    val id: String,                    // UUID generated at capture time
    val owner: Player,
    val cells: Set<Coord>,             // All enclosed cells
    val capturedDots: Set<Coord>,      // Subset of cells where a dot exists
    val capturedAt: Int,               // Move number when territory was formed
    val absorbedTerritoryIds: List<String>  // IDs of territories this re-captured
) {
    val area: Int get() = cells.size
    val capturedDotCount: Int get() = capturedDots.size
}
```

---

## 6. Game State

```kotlin
// engine/model/GameState.kt
data class GameState(
    val board: Board,
    val currentPlayer: Player,
    val phase: GamePhase,
    val territories: List<Territory>,
    val moveCount: Int,
    val lastMove: Coord?,
    val winner: Player,          // NONE if in progress or draw
    val config: GameConfig,
    val score: Score             // Computed score based on config.scoringVariant
) {
    val isGameOver: Boolean get() = phase != GamePhase.IN_PROGRESS
}
```

---

## 7. Game Config

```kotlin
// engine/model/GameConfig.kt
data class GameConfig(
    val cols: Int = 61,                          // Number of intersection columns
    val rows: Int = 41,                          // Number of intersection rows
    val scoringVariant: ScoringVariant = ScoringVariant.TERRITORY_AREA,
    val firstPlayer: Player = Player.A,
    val playerAType: PlayerType = PlayerType.HUMAN,
    val playerBType: PlayerType = PlayerType.HUMAN,
    val undoLimit: Int = 50,
    val allowSelfCapture: Boolean = false
) {
    companion object {
        val SMALL = GameConfig(cols = 21, rows = 16)
        val MEDIUM = GameConfig(cols = 31, rows = 21)
        val LARGE = GameConfig(cols = 61, rows = 41)
    }
}

enum class PlayerType { HUMAN, AI_EASY, AI_MEDIUM, AI_HARD }
```

---

## 8. Score

```kotlin
// engine/model/Score.kt
data class Score(
    val playerA: Int,
    val playerB: Int
) {
    val leader: Player get() = when {
        playerA > playerB -> Player.A
        playerB > playerA -> Player.B
        else -> Player.NONE
    }
    val difference: Int get() = kotlin.math.abs(playerA - playerB)
}
```

---

## 9. Move

```kotlin
// engine/model/Move.kt
sealed class Move {
    data class PlaceDot(val coord: Coord, val player: Player, val moveNumber: Int) : Move()
    data class Surrender(val player: Player, val moveNumber: Int) : Move()
}
```

---

## 10. Persistence Models (Room / SQLite)

> **Used only by the Android/persistence layer — not the engine.**

```kotlin
// persistence/entity/GameEntity.kt
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,       // UUID
    val startedAt: Long,              // timestamp ms
    val finishedAt: Long?,
    val configJson: String,           // serialized GameConfig
    val winner: String,               // "A", "B", "NONE"
    val finalScoreA: Int,
    val finalScoreB: Int,
    val totalMoves: Int
)

// persistence/entity/MoveEntity.kt
@Entity(tableName = "moves", foreignKeys = [...])
data class MoveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String,
    val moveNumber: Int,
    val col: Int,
    val row: Int,
    val player: String,               // "A" or "B"
    val isSurrender: Boolean
)
```

Replay any saved game by re-applying all MoveEntity records through the GameEngine.

---

## 11. UI State Models (ViewModel layer)

```kotlin
// ui/state/BoardUiState.kt
data class BoardUiState(
    val cells: List<List<CellUiState>>,  // [col][row]
    val highlightedCells: Set<Coord>,    // Last move, hints
    val isInteractionEnabled: Boolean,
    val currentPlayer: Player
)

data class CellUiState(
    val dot: Player,
    val territory: Player,
    val isLastMove: Boolean,
    val isHint: Boolean
)

// ui/state/GameUiState.kt
data class GameUiState(
    val board: BoardUiState,
    val score: Score,
    val currentPlayer: Player,
    val phase: GamePhase,
    val canUndo: Boolean,
    val moveCount: Int
)
```

---

## Serialization

Use **`kotlinx.serialization`** for JSON (config, game export, future online play):

```kotlin
@Serializable
data class GameConfig(...)  // Add @Serializable annotation

// Serialize for storage:
val json = Json.encodeToString(GameConfig.serializer(), config)
// Deserialize:
val config = Json.decodeFromString(GameConfig.serializer(), json)
```

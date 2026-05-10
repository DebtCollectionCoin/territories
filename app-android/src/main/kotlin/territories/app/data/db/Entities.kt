package territories.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val configJson: String,        // kotlinx.serialization JSON of GameConfig
    val winner: String,            // "A", "B", "C", "D", or "NONE" (draw / in-progress)
    val finalScoreA: Int,
    val finalScoreB: Int,
    val finalScoreC: Int = 0,
    val finalScoreD: Int = 0,
    val totalMoves: Int
)

@Entity(
    tableName = "moves",
    foreignKeys = [
        ForeignKey(
            entity        = GameEntity::class,
            parentColumns = ["id"],
            childColumns  = ["gameId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gameId")]
)
data class MoveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String,
    val moveNumber: Int,
    val col: Int,
    val row: Int,
    val player: String,           // "A" or "B"
    val isSurrender: Boolean
)

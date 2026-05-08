package territories.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGame(id: String): GameEntity?

    /** Returns all finished games ordered newest-first. */
    @Query("SELECT * FROM games WHERE winner != 'NONE' ORDER BY finishedAt DESC")
    fun observeFinishedGames(): Flow<List<GameEntity>>

    /** Returns the single unfinished game, if any (there can only be one at a time). */
    @Query("SELECT * FROM games WHERE winner = 'NONE' LIMIT 1")
    suspend fun getInProgressGame(): GameEntity?

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGame(id: String)

    /** Wipe all unfinished games (called when starting a fresh game without resuming). */
    @Query("DELETE FROM games WHERE winner = 'NONE'")
    suspend fun deleteAllInProgress()
}

@Dao
interface MoveDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMove(move: MoveEntity)

    @Query("SELECT * FROM moves WHERE gameId = :gameId ORDER BY moveNumber ASC")
    suspend fun getMovesForGame(gameId: String): List<MoveEntity>

    @Query("DELETE FROM moves WHERE gameId = :gameId")
    suspend fun deleteMovesForGame(gameId: String)
}

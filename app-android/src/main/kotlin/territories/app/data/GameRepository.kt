package territories.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import territories.app.data.db.GameDao
import territories.app.data.db.GameEntity
import territories.app.data.db.MoveDao
import territories.app.data.db.MoveEntity
import territories.engine.model.Coord
import territories.engine.model.GameConfig
import territories.engine.model.GameState
import territories.engine.model.Move
import territories.engine.model.Player
import java.util.UUID
import javax.inject.Inject

class GameRepository @Inject constructor(
    private val gameDao: GameDao,
    private val moveDao: MoveDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Create a new game record and return its id. */
    suspend fun createGame(config: GameConfig): String {
        val id = UUID.randomUUID().toString()
        gameDao.insertGame(
            GameEntity(
                id          = id,
                startedAt   = System.currentTimeMillis(),
                finishedAt  = null,
                configJson  = json.encodeToString(config),
                winner      = "NONE",
                finalScoreA = 0,
                finalScoreB = 0,
                totalMoves  = 0
            )
        )
        return id
    }

    /** Append a move to a running game. */
    suspend fun recordMove(gameId: String, move: Move) {
        when (move) {
            is Move.PlaceDot -> moveDao.insertMove(
                MoveEntity(
                    gameId      = gameId,
                    moveNumber  = move.moveNumber,
                    col         = move.coord.col,
                    row         = move.coord.row,
                    player      = move.player.name,
                    isSurrender = false
                )
            )
            is Move.Surrender -> moveDao.insertMove(
                MoveEntity(
                    gameId      = gameId,
                    moveNumber  = move.moveNumber,
                    col         = -1,
                    row         = -1,
                    player      = move.player.name,
                    isSurrender = true
                )
            )
        }
    }

    /** Finalize a game after it ends. */
    suspend fun finalizeGame(gameId: String, state: GameState) {
        val existing = gameDao.getGame(gameId) ?: return
        gameDao.updateGame(
            existing.copy(
                finishedAt  = System.currentTimeMillis(),
                winner      = state.winner.name,
                finalScoreA = state.score.playerA,
                finalScoreB = state.score.playerB,
                totalMoves  = state.moveCount
            )
        )
    }

    /** Observe finished games (for a future history screen). */
    fun observeFinishedGames(): Flow<List<GameEntity>> =
        gameDao.observeFinishedGames()

    /** Get the single in-progress game, if one exists. */
    suspend fun getInProgressGame(): GameEntity? = gameDao.getInProgressGame()

    /** Load all moves of a game for replay. */
    suspend fun loadMoves(gameId: String): List<Move> =
        moveDao.getMovesForGame(gameId).map { entity ->
            val player = Player.valueOf(entity.player)
            if (entity.isSurrender) {
                Move.Surrender(player, entity.moveNumber)
            } else {
                Move.PlaceDot(Coord(entity.col, entity.row), player, entity.moveNumber)
            }
        }

    suspend fun deleteGame(gameId: String) {
        gameDao.deleteGame(gameId)
    }

    /** Remove any games that were never finished (called before starting a fresh game). */
    suspend fun deleteAllInProgress() {
        gameDao.deleteAllInProgress()
    }

    /** Decode the GameConfig saved with a game (used by resume flow). */
    fun decodeConfig(entity: GameEntity): GameConfig =
        json.decodeFromString(GameConfig.serializer(), entity.configJson)
}

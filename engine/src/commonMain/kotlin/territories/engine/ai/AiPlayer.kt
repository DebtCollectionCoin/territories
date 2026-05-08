package territories.engine.ai

import territories.engine.model.Coord
import territories.engine.model.GameState

interface AiPlayer {
    suspend fun selectMove(state: GameState): Coord
}

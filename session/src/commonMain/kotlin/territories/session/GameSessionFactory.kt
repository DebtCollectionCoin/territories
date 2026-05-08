package territories.session

import territories.engine.engine.GameEngine
import territories.engine.model.GameConfig

object GameSessionFactory {
    fun createLocal(config: GameConfig): LocalGameSession {
        val engine = GameEngine(config)
        return LocalGameSession(engine)
    }
}

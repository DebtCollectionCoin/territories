package territories.app.data

import territories.engine.model.GameConfig

/**
 * Simple singleton holding the GameConfig that was last configured on the Setup screen.
 * Provided via Hilt @Singleton so it survives across ViewModels in the same process.
 */
class GameConfigHolder {
    var current: GameConfig = GameConfig.MEDIUM
    /** If non-null, the next GameViewModel.newGame() should resume this game id instead. */
    var resumeGameId: String? = null
}

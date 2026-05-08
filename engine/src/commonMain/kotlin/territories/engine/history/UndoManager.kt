package territories.engine.history

import territories.engine.model.GameState

class UndoManager(limit: Int = 50) {

    private val history = MoveHistory(limit)

    /** Record state BEFORE applying a move so it can be restored. */
    fun recordMove(stateBefore: GameState) = history.push(stateBefore)

    /** Returns the state before the last move, or null if nothing to undo. */
    fun undo(): GameState? = history.pop()

    fun canUndo(): Boolean = !history.isEmpty()

    fun clear() = history.clear()
}

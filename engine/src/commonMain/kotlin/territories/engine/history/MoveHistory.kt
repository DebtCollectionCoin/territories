package territories.engine.history

import territories.engine.model.GameState

class MoveHistory(private val limit: Int = 50) {

    private val stack = ArrayDeque<GameState>()

    fun push(state: GameState) {
        stack.addLast(state)
        if (stack.size > limit) {
            stack.removeFirst()
        }
    }

    fun pop(): GameState? = if (stack.isEmpty()) null else stack.removeLast()

    fun peek(): GameState? = stack.lastOrNull()

    fun isEmpty(): Boolean = stack.isEmpty()

    fun size(): Int = stack.size

    fun clear() = stack.clear()
}

package territories.sharedui

import territories.engine.engine.GameEngine
import territories.engine.model.GameConfig
import kotlin.test.Test
import kotlin.test.assertTrue

class BoardSemanticsTest {

    @Test
    fun describesInitialState() {
        val state = GameEngine(GameConfig.SMALL).initialState()
        val desc = buildBoardDescription(state)
        assertTrue(desc.contains("game board"), "should mention 'game board': $desc")
        assertTrue(desc.contains("Blue 0"), "should mention Blue 0: $desc")
        assertTrue(desc.contains("Red 0"), "should mention Red 0: $desc")
        assertTrue(desc.contains("Blue's turn"), "should be Blue's turn: $desc")
    }
}

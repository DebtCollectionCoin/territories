package territories.ranking

import territories.engine.engine.GameEngine
import territories.engine.model.Coord
import territories.engine.model.GameConfig
import territories.engine.model.Player
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchOutcomeTest {

    @Test
    fun rankings_twoPlayer_byScore() = runBlocking {
        // Manually craft a finished 2-player game by surrender.
        val engine = GameEngine(GameConfig(cols = 5, rows = 5))
        val state = engine.initialState()
        val finished = engine.surrender(state, Player.A)
        val ranks = MatchOutcome.rankings(finished)
        assertEquals(1, ranks[Player.B])
        assertEquals(2, ranks[Player.A])
    }

    @Test
    fun rankings_threePlayer_eliminatedSeatsLast() = runBlocking {
        val engine = GameEngine(GameConfig(cols = 5, rows = 5, playerCount = 3))
        var state = engine.initialState()
        // A surrenders, B surrenders → C wins, A and B eliminated.
        state = engine.surrender(state, Player.A)
        state = engine.surrender(state, Player.B)
        assertTrue(state.isGameOver)
        val ranks = MatchOutcome.rankings(state)
        assertEquals(1, ranks[Player.C], "C should be ranked first")
        // A and B are both eliminated with score 0; tie at rank 2.
        assertEquals(2, ranks[Player.A])
        assertEquals(2, ranks[Player.B])
    }

    @Test
    fun entries_buildsPairsForAllSeats() = runBlocking {
        val engine = GameEngine(GameConfig(cols = 5, rows = 5))
        val state = engine.initialState()
        val finished = engine.surrender(state, Player.A)
        val entries = MatchOutcome.entries(finished)
        assertEquals(2, entries.size)
        assertEquals(Player.A, entries[0].first)
        assertEquals(Player.B, entries[1].first)
        assertEquals(Rating.DEFAULT, entries[0].second.rating)
    }

    @Test
    fun rankings_failsForUnfinishedGame() {
        val engine = GameEngine(GameConfig(cols = 5, rows = 5))
        val state = engine.initialState()
        try {
            MatchOutcome.rankings(state)
            error("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}

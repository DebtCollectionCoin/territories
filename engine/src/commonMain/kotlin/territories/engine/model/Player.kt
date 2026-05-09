package territories.engine.model

import kotlinx.serialization.Serializable

/**
 * Seats in a game of Territories.
 *
 * 2-player games use [A] and [B]; 3- and 4-player free-for-all games
 * additionally use [C] and [D]. [NONE] represents the empty owner.
 */
@Serializable
enum class Player {
    A, B, C, D, NONE
}

/**
 * Returns the opposite seat in a two-player game.
 *
 * Defined only for 2-player games. Returns [Player.NONE] for [Player.NONE],
 * [Player.C], [Player.D] — callers handling free-for-all should iterate
 * `state.players` instead.
 */
fun Player.opponent(): Player = when (this) {
    Player.A -> Player.B
    Player.B -> Player.A
    else     -> Player.NONE
}

/** All seat values in canonical seat order (excluding NONE). */
val ALL_SEATS: List<Player> = listOf(Player.A, Player.B, Player.C, Player.D)

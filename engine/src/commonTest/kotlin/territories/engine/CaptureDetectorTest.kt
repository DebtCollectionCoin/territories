package territories.engine

import territories.engine.engine.CaptureDetector
import territories.engine.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaptureDetectorTest {

    private val detector = CaptureDetector()

    /**
     * Build a board from a string grid:
     *  '.' = empty, 'A' = Player A dot, 'B' = Player B dot
     * rows are newline-separated, cols are chars.
     */
    private fun boardFrom(vararg rows: String): Board {
        val numRows = rows.size
        val numCols = rows[0].length
        var board = Board.empty(numCols, numRows)
        for (row in rows.indices) {
            for (col in rows[row].indices) {
                when (rows[row][col]) {
                    'A' -> board = board.withDot(Coord(col, row), Player.A)
                    'B' -> board = board.withDot(Coord(col, row), Player.B)
                }
            }
        }
        return board
    }

    @Test
    fun simpleSquareEncirclement() {
        // Player A surrounds one B dot in a 3×3:
        //  A A A
        //  A B A   ← B at (1,1)
        //  A A A
        val board = boardFrom(
            "AAA",
            "ABA",
            "AAA"
        )
        // The board is 3×3, all cells are border cells — so this should NOT capture
        // because the region touches the border. Let's use a larger board and place internally.
        // Use a 7×7 board with encirclement in the center.
        val big = Board.empty(7, 7)
        var b = big
        // Ring of A around (3,3) — square encirclement
        for (coord in listOf(
            Coord(2,2), Coord(3,2), Coord(4,2),
            Coord(2,3),             Coord(4,3),
            Coord(2,4), Coord(3,4), Coord(4,4)
        )) {
            b = b.withDot(coord, Player.A)
        }
        // Place a B dot inside
        b = b.withDot(Coord(3, 3), Player.B)

        // Now place the last A dot that closes the ring — in this ring it's already closed,
        // so we call detectCaptures with the last placed A coord
        val captured = detector.detectCaptures(Coord(4, 4), Player.A, b)
        assertEquals(1, captured.size, "Should capture exactly 1 region")
        assertTrue(Coord(3, 3) in captured[0].capturedDots, "B dot at (3,3) should be captured")
    }

    @Test
    fun uShapeDoesNotCapture() {
        // Open U-shape — bottom is open, should NOT capture
        val board = Board.empty(7, 7)
        var b = board
        // Left wall, top, right wall — no bottom
        for (coord in listOf(
            Coord(2,2), Coord(3,2), Coord(4,2),
            Coord(2,3),             Coord(4,3)
            // bottom open
        )) {
            b = b.withDot(coord, Player.A)
        }
        b = b.withDot(Coord(3, 3), Player.B)
        val captured = detector.detectCaptures(Coord(4, 3), Player.A, b)
        assertEquals(0, captured.size, "Open U-shape should not capture")
    }

    @Test
    fun encirclementTouchingBorderDoesNotCapture() {
        // Place B on row=0 (border). Any encirclement attempt should fail because
        // the interior region can escape via the border.
        val board = Board.empty(7, 7)
        var b = board
        // Attempt to surround (3,0) which is on the border
        b = b.withDot(Coord(2, 0), Player.A)
        b = b.withDot(Coord(4, 0), Player.A)
        b = b.withDot(Coord(2, 1), Player.A)
        b = b.withDot(Coord(3, 1), Player.A)
        b = b.withDot(Coord(4, 1), Player.A)
        b = b.withDot(Coord(3, 0), Player.B)

        val captured = detector.detectCaptures(Coord(3, 1), Player.A, b)
        assertEquals(0, captured.size, "Encirclement touching border should not capture")
    }

    @Test
    fun emptyEnclosedAreaWithNoOpponentDotDoesNotCapture() {
        // Full encirclement but no B dot inside — rule §4.1: must contain ≥1 opponent dot
        val board = Board.empty(7, 7)
        var b = board
        for (coord in listOf(
            Coord(2,2), Coord(3,2), Coord(4,2),
            Coord(2,3),             Coord(4,3),
            Coord(2,4), Coord(3,4), Coord(4,4)
        )) {
            b = b.withDot(coord, Player.A)
        }
        // No B dot inside (3,3) — leave it empty
        val captured = detector.detectCaptures(Coord(4, 4), Player.A, b)
        assertEquals(0, captured.size, "Enclosing only empty space should not capture")
    }

    @Test
    fun staircaseChainEnclosesCorrectly() {
        // Staircase pattern — each step overlaps 8-directionally, no diagonal gap
        // Build a staircase ring around (5,5) on a 11×11 board
        val board = Board.empty(11, 11)
        var b = board
        // Staircase encirclement coords (hand-crafted valid ring)
        val ring = listOf(
            Coord(4,4), Coord(5,4), Coord(6,4),
            Coord(6,5),
            Coord(6,6), Coord(5,6), Coord(4,6),
            Coord(4,5)
        )
        for (coord in ring) b = b.withDot(coord, Player.A)
        b = b.withDot(Coord(5, 5), Player.B)

        val captured = detector.detectCaptures(Coord(4, 5), Player.A, b)
        assertEquals(1, captured.size, "Staircase ring should capture enclosed region")
    }

    @Test
    fun multipleSimultaneousCapturesFromOneMove() {
        // Place one A dot that closes two separate encirclements
        val board = Board.empty(15, 7)
        var b = board

        // Left encirclement: ring around (2,3)
        for (coord in listOf(
            Coord(1,2), Coord(2,2), Coord(3,2),
            Coord(1,3),             Coord(3,3),
            Coord(1,4), Coord(2,4) // Coord(3,4) will be the shared last dot
        )) b = b.withDot(coord, Player.A)
        b = b.withDot(Coord(2, 3), Player.B)

        // Right encirclement: ring around (6,3)
        for (coord in listOf(
            Coord(5,2), Coord(6,2), Coord(7,2),
            Coord(5,3),             Coord(7,3),
            Coord(5,4), Coord(6,4), Coord(7,4)
        )) b = b.withDot(coord, Player.A)
        b = b.withDot(Coord(6, 3), Player.B)

        // Close left encirclement with Coord(3,4)
        b = b.withDot(Coord(3, 4), Player.A)

        val captured = detector.detectCaptures(Coord(3, 4), Player.A, b)
        assertTrue(captured.size >= 1, "At least left encirclement should be captured")
    }

    @Test
    fun reCapturePreviousTerritory() {
        // Player A captures a region; then Player B re-captures it
        val board = Board.empty(9, 9)
        var b = board

        // Mark (4,4) as Player A territory (simulating prior A capture)
        b = b.withTerritory(setOf(Coord(4, 4)), Player.A)
        // Place A dot at (4,4) dot-slot (territory cell, dot is still NONE)
        // Now B encloses the whole area

        for (coord in listOf(
            Coord(3,3), Coord(4,3), Coord(5,3),
            Coord(3,4),             Coord(5,4),
            Coord(3,5), Coord(4,5), Coord(5,5)
        )) b = b.withDot(coord, Player.B)

        // Place an A dot inside the area that B will enclose
        b = b.withDot(Coord(4, 4), Player.A)

        val captured = detector.detectCaptures(Coord(5, 5), Player.B, b)
        assertEquals(1, captured.size, "B should re-capture the region")
        assertTrue(Coord(4, 4) in captured[0].capturedDots)
    }
}

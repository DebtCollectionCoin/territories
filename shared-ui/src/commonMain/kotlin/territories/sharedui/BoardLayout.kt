package territories.sharedui

import territories.engine.model.Coord

/**
 * Pure layout math shared by the Android and Desktop board canvases.
 *
 * Both apps draw the board on a Compose `Canvas` and need the same
 * conversion between screen pixels and grid coordinates. The math is
 * pure (no Compose / no platform types) so it lives in `commonMain`
 * and can be reused on JVM, Android, and any future target.
 *
 * The board is drawn with cells of equal pixel size [cellSize], with
 * the top-left dot positioned at ([offsetX], [offsetY]) on screen. Dots
 * are drawn at integer grid coordinates `(col, row)` with screen
 * position `(offsetX + col*cellSize, offsetY + row*cellSize)`.
 */
data class BoardLayout(
    val cellSize: Float,
    val offsetX: Float,
    val offsetY: Float,
) {
    /** Screen-X for [col]. */
    fun xFor(col: Int): Float = offsetX + col * cellSize

    /** Screen-Y for [row]. */
    fun yFor(row: Int): Float = offsetY + row * cellSize
}

/**
 * Computes the [BoardLayout] that fits a [cols] × [rows] grid into a
 * canvas of size [width] × [height], leaving one cell of padding on
 * each side. Matches the Android and Desktop draw code byte-for-byte.
 */
fun computeBoardLayout(width: Float, height: Float, cols: Int, rows: Int): BoardLayout {
    val cellSize = minOf(width / (cols + 1), height / (rows + 1))
    val offsetX = (width - cellSize * (cols - 1)) / 2f
    val offsetY = (height - cellSize * (rows - 1)) / 2f
    return BoardLayout(cellSize, offsetX, offsetY)
}

/**
 * Converts a tap at screen ([x], [y]) into the closest grid [Coord].
 * Returns the snapped coordinate even if it is off-board; callers
 * should validate against `Board.isOnBoard` if needed.
 */
fun BoardLayout.screenToCell(x: Float, y: Float): Coord {
    val col = ((x - offsetX) / cellSize + 0.5f).toInt()
    val row = ((y - offsetY) / cellSize + 0.5f).toInt()
    return Coord(col, row)
}

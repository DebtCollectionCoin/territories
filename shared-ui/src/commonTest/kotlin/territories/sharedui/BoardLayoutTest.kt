package territories.sharedui

import kotlin.test.Test
import kotlin.test.assertEquals

class BoardLayoutTest {

    @Test
    fun `compute layout fits cols + 1 cells horizontally and rows + 1 vertically`() {
        // 31×21 board on a 1000×800 canvas.
        val l = computeBoardLayout(width = 1000f, height = 800f, cols = 31, rows = 21)
        // limited by width: 1000 / (31+1) = 31.25 < 800 / (21+1) ≈ 36.36
        assertEquals(1000f / 32f, l.cellSize, 0.001f)
        // grid is centred horizontally and vertically
        val gridW = l.cellSize * (31 - 1)
        val gridH = l.cellSize * (21 - 1)
        assertEquals((1000f - gridW) / 2f, l.offsetX, 0.001f)
        assertEquals((800f  - gridH) / 2f, l.offsetY, 0.001f)
    }

    @Test
    fun `screenToCell snaps to nearest dot`() {
        val l = BoardLayout(cellSize = 10f, offsetX = 5f, offsetY = 5f)
        // exact dot
        assertEquals(0, l.screenToCell(5f, 5f).col)
        assertEquals(0, l.screenToCell(5f, 5f).row)
        // half-cell to the right of (1,0) — should round to (2,0) per the +0.5 rule
        // pixel x=20 → (20-5)/10 + 0.5 = 2.0 → 2
        assertEquals(2, l.screenToCell(20f, 5f).col)
        // pixel x=14 → (14-5)/10 + 0.5 = 1.4 → 1
        assertEquals(1, l.screenToCell(14f, 5f).col)
    }

    @Test
    fun `xFor and yFor invert screenToCell on integer dots`() {
        val l = BoardLayout(cellSize = 12f, offsetX = 6f, offsetY = 6f)
        for (c in 0..5) for (r in 0..5) {
            val px = l.xFor(c); val py = l.yFor(r)
            val coord = l.screenToCell(px, py)
            assertEquals(c, coord.col)
            assertEquals(r, coord.row)
        }
    }
}

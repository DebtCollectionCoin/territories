package territories.web

import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import territories.engine.model.*
import kotlin.math.PI

/**
 * Paper-on-table board renderer.
 *
 *  • Dark "table" surface (subtle linear gradient).
 *  • Cream/white paper sheet with soft drop shadow + slight rotation-free skew via
 *    a vignette to suggest physical depth.
 *  • Dotted grid: a small ink dot at every intersection (slightly bolder every 5).
 *  • Stones drawn ink-style: solid color with a thin pen-stroke outline and
 *    subtle drop shadow.
 *  • Territories: very soft watercolor washes.
 *  • Last-move + capture animations preserved.
 */
class BoardRenderer(private val canvas: HTMLCanvasElement) {

    private val ctx: CanvasRenderingContext2D
        get() = canvas.getContext("2d") as CanvasRenderingContext2D

    private var cellSize: Double = 0.0
    private var offsetX: Double = 0.0
    private var offsetY: Double = 0.0

    private var pulseStart: Double = jsNow()
    private var rippleStart: Double = -1.0
    private var rippleAt: Coord? = null
    private var rippleColor: String = "#000"

    // ── Player palette (slightly desaturated to read like ink on paper) ──
    private val playerA          = "#2f3a8a"           // deep indigo ink
    private val playerAOutline   = "#1d2566"
    private val territoryA       = "rgba(57,73,171,0.16)"
    private val playerB          = "#b83a3a"           // deep coral ink
    private val playerBOutline   = "#7d2222"
    private val territoryB       = "rgba(216,74,74,0.16)"
    private val playerC          = "#2e7d32"           // deep green ink
    private val playerCOutline   = "#1b5e20"
    private val territoryC       = "rgba(46,125,50,0.16)"
    private val playerD          = "#c58a00"           // burnt amber ink
    private val playerDOutline   = "#8a6100"
    private val territoryD       = "rgba(249,168,37,0.18)"

    private fun stoneFill(p: Player): String = when (p) {
        Player.A -> playerA; Player.B -> playerB
        Player.C -> playerC; Player.D -> playerD
        else -> playerA
    }
    private fun stoneOutline(p: Player): String = when (p) {
        Player.A -> playerAOutline; Player.B -> playerBOutline
        Player.C -> playerCOutline; Player.D -> playerDOutline
        else -> playerAOutline
    }
    private fun territoryFill(p: Player): String = when (p) {
        Player.A -> territoryA; Player.B -> territoryB
        Player.C -> territoryC; Player.D -> territoryD
        else -> territoryA
    }

    // ── Table & paper colors ──
    private val tableTop         = "#1a1c22"
    private val tableBottom      = "#0c0d12"
    private val paperFill        = "#f6f1e2"           // cream
    private val paperFillEdge    = "#ece5d2"
    private val paperBorder      = "rgba(0,0,0,0.18)"
    private val paperShadow      = "rgba(0,0,0,0.55)"
    private val gridCross        = "#1c1c20"           // black ink cross
    private val gridCrossMajor   = "#000000"
    private val gridDotEmpty     = "#fbfaf5"           // tiny white dot on cross
    private val lastMoveRing     = "#c98a00"           // muted amber on cream

    fun triggerCaptureRipple(coord: Coord, player: Player) {
        rippleAt = coord
        rippleColor = stoneFill(player)
        rippleStart = jsNow()
    }

    fun hasActiveAnimation(state: GameState): Boolean {
        if (Settings.pulseEnabled && state.lastMove != null && !state.isGameOver) return true
        val r = rippleStart
        if (r > 0 && jsNow() - r < 700.0) return true
        return false
    }

    fun resize(cols: Int, rows: Int) {
        val availW = canvas.clientWidth.toDouble()
        val availH = canvas.clientHeight.toDouble()
        if (availW <= 0 || availH <= 0) return

        // Reserve some margin for the paper to "float" off the table edges.
        val pad = 22.0
        cellSize = minOf((availW - 2 * pad) / (cols + 1), (availH - 2 * pad) / (rows + 1))

        val dpr = (js("window.devicePixelRatio") as? Double) ?: 1.0
        canvas.width  = (canvas.clientWidth  * dpr).toInt()
        canvas.height = (canvas.clientHeight * dpr).toInt()
        ctx.setTransform(dpr, 0.0, 0.0, dpr, 0.0, 0.0)

        offsetX = (canvas.clientWidth  - cellSize * (cols - 1)) / 2
        offsetY = (canvas.clientHeight - cellSize * (rows - 1)) / 2
    }

    fun render(state: GameState) {
        val board = state.board
        resize(board.cols, board.rows)

        val cw = canvas.clientWidth.toDouble()
        val ch = canvas.clientHeight.toDouble()

        // 1. Table background (subtle gradient)
        run {
            val grad = ctx.createLinearGradient(0.0, 0.0, 0.0, ch)
            grad.addColorStop(0.0, tableTop)
            grad.addColorStop(1.0, tableBottom)
            ctx.fillStyle = grad
            ctx.fillRect(0.0, 0.0, cw, ch)
        }

        val paperPad = cellSize * 0.55
        val paperLeft   = offsetX - cellSize / 2 - paperPad
        val paperTop    = offsetY - cellSize / 2 - paperPad
        val paperRight  = offsetX + (board.cols - 1) * cellSize + cellSize / 2 + paperPad
        val paperBottom = offsetY + (board.rows - 1) * cellSize + cellSize / 2 + paperPad
        val paperW = paperRight - paperLeft
        val paperH = paperBottom - paperTop

        // 2. Paper drop shadow (soft, offset)
        ctx.save()
        ctx.shadowColor = paperShadow
        ctx.shadowBlur = 26.0
        ctx.shadowOffsetX = 0.0
        ctx.shadowOffsetY = 8.0
        // Paper fill with subtle radial vignette
        val paperGrad = ctx.createRadialGradient(
            paperLeft + paperW * 0.5, paperTop + paperH * 0.45, paperW * 0.1,
            paperLeft + paperW * 0.5, paperTop + paperH * 0.5,  paperW * 0.75
        )
        paperGrad.addColorStop(0.0, paperFill)
        paperGrad.addColorStop(1.0, paperFillEdge)
        ctx.fillStyle = paperGrad
        roundRect(paperLeft, paperTop, paperW, paperH, 6.0)
        ctx.fill()
        ctx.restore()

        // 3. Hairline border
        ctx.strokeStyle = paperBorder
        ctx.lineWidth = 1.0
        roundRect(paperLeft, paperTop, paperW, paperH, 6.0)
        ctx.stroke()

        // 4. Territory washes (soft watercolor squares around enclosed cells)
        for (coord in board.allCoords()) {
            val cell = board.get(coord)
            if (cell.territory != Player.NONE) {
                ctx.fillStyle = territoryFill(cell.territory)
                ctx.fillRect(
                    colToX(coord.col) - cellSize / 2,
                    rowToY(coord.row) - cellSize / 2,
                    cellSize, cellSize
                )
            }
        }

        // 5. Grid: black "+" cross at every empty intersection, with a small
        //    white dot on top so the player sees a clickable target. When a
        //    stone is placed, it covers this entirely.
        val crossArm   = (cellSize * 0.18).coerceIn(3.0, 8.0)
        val crossWidth = (cellSize * 0.06).coerceIn(0.8, 2.0)
        val whiteDotR  = (cellSize * 0.08).coerceIn(1.2, 2.6)
        val majorBoost = 1.35
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                val cell = board.get(Coord(col, row))
                if (cell.dot != Player.NONE) continue  // stone will cover the cross
                val cx = colToX(col)
                val cy = rowToY(row)
                val isMajor = (col % 5 == 0) && (row % 5 == 0)
                val arm = if (isMajor) crossArm * majorBoost else crossArm

                ctx.strokeStyle = if (isMajor) gridCrossMajor else gridCross
                ctx.lineWidth = if (isMajor) crossWidth * 1.25 else crossWidth
                ctx.asDynamic().lineCap = "round"
                // Horizontal stroke
                ctx.beginPath()
                ctx.moveTo(cx - arm, cy)
                ctx.lineTo(cx + arm, cy)
                ctx.stroke()
                // Vertical stroke
                ctx.beginPath()
                ctx.moveTo(cx, cy - arm)
                ctx.lineTo(cx, cy + arm)
                ctx.stroke()

                // White dot on top of cross center
                ctx.fillStyle = gridDotEmpty
                ctx.beginPath()
                ctx.arc(cx, cy, whiteDotR, 0.0, 2 * PI)
                ctx.fill()
            }
        }

        // 6. Stones (ink-style: filled disc + pen outline + subtle shadow)
        val dotR = cellSize * 0.34
        for (coord in board.allCoords()) {
            val cell = board.get(coord)
            if (cell.dot != Player.NONE) drawStone(coord, cell.dot, dotR)
        }

        // 7. Last-move marker (muted amber underline ring)
        if (Settings.pulseEnabled && !state.isGameOver) {
            state.lastMove?.let { last ->
                val cx = colToX(last.col)
                val cy = rowToY(last.row)
                val now = jsNow()
                val phase = ((now - pulseStart) % 1400.0) / 1400.0
                val pulseR = (dotR + 4.0) + phase * cellSize * 0.30
                val pulseAlpha = (1.0 - phase).coerceIn(0.0, 1.0)

                ctx.strokeStyle = "rgba(201,138,0,${pulseAlpha * 0.7})"
                ctx.lineWidth = 2.0
                ctx.beginPath()
                ctx.arc(cx, cy, pulseR, 0.0, 2 * PI)
                ctx.stroke()

                ctx.strokeStyle = lastMoveRing
                ctx.lineWidth = 1.6
                ctx.beginPath()
                ctx.arc(cx, cy, dotR + 3.0, 0.0, 2 * PI)
                ctx.stroke()
            }
        }

        // 8. Capture ripple
        val r = rippleStart
        if (r > 0) {
            val elapsed = jsNow() - r
            if (elapsed in 0.0..700.0) {
                rippleAt?.let { c ->
                    val t = elapsed / 700.0
                    val maxR = cellSize * 6.0
                    val rad = dotR + t * maxR
                    val alpha = (1.0 - t).coerceIn(0.0, 1.0)
                    ctx.strokeStyle = withAlpha(rippleColor, alpha * 0.6)
                    ctx.lineWidth = 2.5
                    ctx.beginPath()
                    ctx.arc(colToX(c.col), rowToY(c.row), rad, 0.0, 2 * PI)
                    ctx.stroke()
                }
            } else {
                rippleStart = -1.0
            }
        }
    }

    private fun drawStone(coord: Coord, owner: Player, radius: Double) {
        val cx = colToX(coord.col)
        val cy = rowToY(coord.row)
        val fill    = stoneFill(owner)
        val outline = stoneOutline(owner)

        // Soft ink shadow
        ctx.save()
        ctx.shadowColor = "rgba(0,0,0,0.30)"
        ctx.shadowBlur = 4.0
        ctx.shadowOffsetX = 0.5
        ctx.shadowOffsetY = 1.5
        ctx.fillStyle = fill
        ctx.beginPath()
        ctx.arc(cx, cy, radius, 0.0, 2 * PI)
        ctx.fill()
        ctx.restore()

        // Pen outline
        ctx.strokeStyle = outline
        ctx.lineWidth = 1.0
        ctx.beginPath()
        ctx.arc(cx, cy, radius, 0.0, 2 * PI)
        ctx.stroke()

        // Tiny pen highlight (gives the ink dot a hand-drawn feel)
        ctx.fillStyle = "rgba(255,255,255,0.18)"
        ctx.beginPath()
        ctx.arc(cx - radius * 0.30, cy - radius * 0.35, radius * 0.22, 0.0, 2 * PI)
        ctx.fill()
    }

    fun canvasToCoord(board: Board, canvasX: Double, canvasY: Double): Coord? {
        if (cellSize == 0.0) return null
        val col = ((canvasX - offsetX) / cellSize + 0.5).toInt()
        val row = ((canvasY - offsetY) / cellSize + 0.5).toInt()
        val coord = Coord(col, row)
        return if (board.isOnBoard(coord)) coord else null
    }

    private fun colToX(col: Int): Double = offsetX + col * cellSize
    private fun rowToY(row: Int): Double = offsetY + row * cellSize

    private fun jsNow(): Double = js("Date.now()") as Double

    private fun withAlpha(hex: String, alpha: Double): String {
        val h = hex.removePrefix("#")
        val r = h.substring(0, 2).toInt(16)
        val g = h.substring(2, 4).toInt(16)
        val b = h.substring(4, 6).toInt(16)
        return "rgba($r,$g,$b,$alpha)"
    }

    private fun roundRect(x: Double, y: Double, w: Double, h: Double, r: Double) {
        ctx.beginPath()
        ctx.moveTo(x + r, y)
        ctx.lineTo(x + w - r, y)
        ctx.quadraticCurveTo(x + w, y, x + w, y + r)
        ctx.lineTo(x + w, y + h - r)
        ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
        ctx.lineTo(x + r, y + h)
        ctx.quadraticCurveTo(x, y + h, x, y + h - r)
        ctx.lineTo(x, y + r)
        ctx.quadraticCurveTo(x, y, x + r, y)
        ctx.closePath()
    }
}

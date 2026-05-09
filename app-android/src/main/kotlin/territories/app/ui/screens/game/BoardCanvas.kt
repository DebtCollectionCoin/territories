package territories.app.ui.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import territories.engine.model.Coord
import territories.engine.model.GameState
import territories.engine.model.Player

// ── Refined palette ────────────────────────────────────────────────
internal val PlayerAColor          = Color(0xFF3949AB)   // Indigo 600
internal val PlayerAHighlight      = Color(0xFF7986CB)   // Indigo 300 (top-left of stone)
internal val PlayerATerritory      = Color(0x553949AB)
internal val PlayerBColor          = Color(0xFFD84A4A)   // Warm coral red
internal val PlayerBHighlight      = Color(0xFFE57373)
internal val PlayerBTerritory      = Color(0x55D84A4A)
private val GridColorMinor         = Color(0x1F000000)
private val GridColorMajor         = Color(0x33000000)
private val BoardBgTop             = Color(0xFFFBF7EC)   // Warm cream
private val BoardBgBottom          = Color(0xFFF1EAD8)
private val BoardBorder            = Color(0x33000000)
private val DotShadow              = Color(0x55000000)
private val LastMoveRingColor      = Color(0xFFFFB300)   // Amber 600

@Composable
fun BoardCanvas(
    state: GameState,
    onCoordClick: (Coord) -> Unit,
    modifier: Modifier = Modifier,
    colorBlindMode: Boolean = false
) {
    data class Layout(val cellSize: Float, val offsetX: Float, val offsetY: Float)

    var layout by remember { mutableStateOf<Layout?>(null) }
    val board = state.board

    // Pan / zoom (per-board lifecycle)
    var scale   by remember(board.cols, board.rows) { mutableStateOf(1f) }
    var panX    by remember(board.cols, board.rows) { mutableStateOf(0f) }
    var panY    by remember(board.cols, board.rows) { mutableStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        panX += panChange.x
        panY += panChange.y
    }

    // New-dot pop animation
    val dotScale = remember { Animatable(1f) }
    LaunchedEffect(state.lastMove) {
        if (state.lastMove != null) {
            dotScale.snapTo(0f)
            dotScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium
                )
            )
        }
    }

    // Territory fade-in on capture
    val territoryCount = board.allCoords().count { board.get(it).territory != Player.NONE }
    val territoryAlpha = remember { Animatable(1f) }
    LaunchedEffect(territoryCount) {
        territoryAlpha.snapTo(0.2f)
        territoryAlpha.animateTo(1f, animationSpec = tween(durationMillis = 450))
    }

    // Capture ripple: triggers a one-shot expanding ring centered on lastMove
    // when territory count increases (i.e. after a successful capture).
    var prevTerritoryCount by remember { mutableStateOf(territoryCount) }
    val ripple = remember { Animatable(0f) }
    LaunchedEffect(territoryCount) {
        if (territoryCount > prevTerritoryCount && state.lastMove != null) {
            ripple.snapTo(0f)
            ripple.animateTo(1f, animationSpec = tween(durationMillis = 700))
        }
        prevTerritoryCount = territoryCount
    }
    val rippleColor = when (state.lastMove?.let { board.get(it).dot }) {
        Player.A -> PlayerAColor
        Player.B -> PlayerBColor
        else     -> PlayerAColor
    }

    // Pulsing last-move ring
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulsePhase by pulse.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = buildBoardDescription(state)
                liveRegion = LiveRegionMode.Polite
            }
            .transformable(state = transformState)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = panX,
                translationY = panY
            )
            .pointerInput(board.cols, board.rows) {
                detectTapGestures { offset ->
                    layout?.let { l ->
                        val col = ((offset.x - l.offsetX) / l.cellSize + 0.5f).toInt()
                        val row = ((offset.y - l.offsetY) / l.cellSize + 0.5f).toInt()
                        val coord = Coord(col, row)
                        if (board.isOnBoard(coord)) onCoordClick(coord)
                    }
                }
            }
    ) {
        val cols = board.cols
        val rows = board.rows
        val cs   = minOf(size.width / (cols + 1), size.height / (rows + 1))
        val ox   = (size.width  - cs * (cols - 1)) / 2f
        val oy   = (size.height - cs * (rows - 1)) / 2f
        layout = Layout(cs, ox, oy)

        // 1. Outer surface: warm gradient background
        drawRect(
            brush = Brush.verticalGradient(listOf(BoardBgTop, BoardBgBottom)),
            size  = size
        )

        // 2. Board surface frame: a soft inset rect just outside the play area
        val boardLeft   = ox - cs * 0.5f
        val boardTop    = oy - cs * 0.5f
        val boardRight  = ox + (cols - 1) * cs + cs * 0.5f
        val boardBottom = oy + (rows - 1) * cs + cs * 0.5f
        drawRect(
            color   = BoardBorder,
            topLeft = Offset(boardLeft, boardTop),
            size    = Size(boardRight - boardLeft, boardBottom - boardTop),
            style   = Stroke(width = 1.5f)
        )

        // 3. Territory fills with soft radial fade
        for (coord in board.allCoords()) {
            val cell = board.get(coord)
            if (cell.territory != Player.NONE) {
                val base = if (cell.territory == Player.A) PlayerATerritory else PlayerBTerritory
                val color = base.copy(alpha = base.alpha * territoryAlpha.value)
                drawRect(
                    color   = color,
                    topLeft = Offset(ox + coord.col * cs - cs / 2f, oy + coord.row * cs - cs / 2f),
                    size    = Size(cs, cs)
                )
            }
        }

        // 4. Grid lines: minor every cell, slightly stronger every 5 cells
        for (col in 0 until cols) {
            val x = ox + col * cs
            val isMajor = col % 5 == 0
            drawLine(
                color     = if (isMajor) GridColorMajor else GridColorMinor,
                start     = Offset(x, boardTop),
                end       = Offset(x, boardBottom),
                strokeWidth = if (isMajor) 1.2f else 0.8f
            )
        }
        for (row in 0 until rows) {
            val y = oy + row * cs
            val isMajor = row % 5 == 0
            drawLine(
                color     = if (isMajor) GridColorMajor else GridColorMinor,
                start     = Offset(boardLeft, y),
                end       = Offset(boardRight, y),
                strokeWidth = if (isMajor) 1.2f else 0.8f
            )
        }

        // 5. Dots — stone style (drop shadow + radial highlight).
        // In colorBlindMode, Player B is rendered as a rounded square so
        // shape encodes the player identity in addition to color.
        val dotRadius = cs * 0.36f
        for (coord in board.allCoords()) {
            val cell = board.get(coord)
            if (cell.dot != Player.NONE) {
                val isLast = coord == state.lastMove
                val r = if (isLast) dotRadius * dotScale.value else dotRadius
                if (r <= 0f) continue
                val cx = ox + coord.col * cs
                val cy = oy + coord.row * cs
                val baseColor = if (cell.dot == Player.A) PlayerAColor else PlayerBColor
                val highlight = if (cell.dot == Player.A) PlayerAHighlight else PlayerBHighlight
                val asSquare = colorBlindMode && cell.dot == Player.B

                if (asSquare) {
                    val side = r * 1.78f  // visually similar area to a circle of radius r
                    val half = side / 2f
                    // Drop shadow
                    drawRoundRect(
                        color   = DotShadow,
                        topLeft = Offset(cx - half + 1.5f, cy - half + 2f),
                        size    = Size(side, side),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(side * 0.18f)
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(highlight, baseColor),
                            start  = Offset(cx - half, cy - half),
                            end    = Offset(cx + half, cy + half)
                        ),
                        topLeft = Offset(cx - half, cy - half),
                        size    = Size(side, side),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(side * 0.18f)
                    )
                } else {
                    // Drop shadow (offset down-right)
                    drawCircle(
                        color  = DotShadow,
                        radius = r,
                        center = Offset(cx + 1.5f, cy + 2f)
                    )

                    // Stone gradient: lighter on top-left, darker bottom-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(highlight, baseColor),
                            center = Offset(cx - r * 0.35f, cy - r * 0.35f),
                            radius = r * 1.4f
                        ),
                        radius = r,
                        center = Offset(cx, cy)
                    )
                }
            }
        }

        // 6. Last-move pulsing ring
        state.lastMove?.let { last ->
            if (board.isOnBoard(last)) {
                val cx = ox + last.col * cs
                val cy = oy + last.row * cs
                val pulseRadius = (dotRadius + 4f) + pulsePhase * cs * 0.25f
                val pulseAlpha = (1f - pulsePhase).coerceIn(0f, 1f)
                drawCircle(
                    color  = LastMoveRingColor.copy(alpha = pulseAlpha * 0.85f),
                    radius = pulseRadius * dotScale.value,
                    center = Offset(cx, cy),
                    style  = Stroke(width = 2.5f)
                )
                // Solid inner ring (always visible)
                drawCircle(
                    color  = LastMoveRingColor,
                    radius = (dotRadius + 3f) * dotScale.value,
                    center = Offset(cx, cy),
                    style  = Stroke(width = 2f)
                )
            }
        }

        // 7. Capture ripple — expanding ring after a successful capture
        if (ripple.value > 0f && ripple.value < 1f) {
            state.lastMove?.let { last ->
                if (board.isOnBoard(last)) {
                    val cx = ox + last.col * cs
                    val cy = oy + last.row * cs
                    val maxR = cs * 6f
                    val r = ripple.value * maxR
                    val a = (1f - ripple.value).coerceIn(0f, 1f)
                    drawCircle(
                        color  = rippleColor.copy(alpha = a * 0.5f),
                        radius = r,
                        center = Offset(cx, cy),
                        style  = Stroke(width = (3f * a).coerceAtLeast(1f))
                    )
                }
            }
        }
    }
}

private fun buildBoardDescription(state: GameState): String {
    val board = state.board
    val a = state.score.playerA
    val b = state.score.playerB
    val turn = if (state.isGameOver) "game over"
        else if (state.currentPlayer == Player.A) "Blue's turn" else "Red's turn"
    val last = state.lastMove?.let { coord ->
        val who = when (board.get(coord).dot) {
            Player.A -> "Blue"
            Player.B -> "Red"
            else -> "Last"
        }
        ", $who placed at column ${coord.col + 1} row ${coord.row + 1}"
    } ?: ""
    return "${board.cols} by ${board.rows} game board. Blue $a, Red $b. $turn$last"
}

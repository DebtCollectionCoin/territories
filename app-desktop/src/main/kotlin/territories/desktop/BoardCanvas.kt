package territories.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import territories.engine.model.Coord
import territories.engine.model.GameState
import territories.engine.model.Player
import territories.sharedui.BoardLayout
import territories.sharedui.computeBoardLayout
import territories.sharedui.screenToCell

@Composable
fun BoardCanvas(
    state: GameState,
    onCoordClick: (Coord) -> Unit,
    modifier: Modifier = Modifier
) {
    var layout by remember { mutableStateOf<BoardLayout?>(null) }
    val board = state.board

    val dotScale = remember { Animatable(1f) }
    LaunchedEffect(state.lastMove) {
        if (state.lastMove != null) {
            dotScale.snapTo(0f)
            dotScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
    }

    val territoryCount = board.allCoords().count { board.get(it).territory != Player.NONE }
    val territoryAlpha = remember { Animatable(1f) }
    LaunchedEffect(territoryCount) {
        territoryAlpha.snapTo(0.2f)
        territoryAlpha.animateTo(1f, tween(450))
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulsePhase by pulse.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(board.cols, board.rows) {
                detectTapGestures { offset ->
                    layout?.let { l ->
                        val coord = l.screenToCell(offset.x, offset.y)
                        if (board.isOnBoard(coord)) onCoordClick(coord)
                    }
                }
            }
    ) {
        val cols = board.cols
        val rows = board.rows
        val l = computeBoardLayout(size.width, size.height, cols, rows)
        val cs = l.cellSize
        val ox = l.offsetX
        val oy = l.offsetY
        layout = l

        // Surface gradient
        drawRect(
            brush = Brush.verticalGradient(listOf(AppColors.BoardBgTop, AppColors.BoardBgBottom)),
            size  = size
        )

        val boardLeft   = ox - cs * 0.5f
        val boardTop    = oy - cs * 0.5f
        val boardRight  = ox + (cols - 1) * cs + cs * 0.5f
        val boardBottom = oy + (rows - 1) * cs + cs * 0.5f

        drawRect(
            color   = AppColors.BoardBorder,
            topLeft = Offset(boardLeft, boardTop),
            size    = Size(boardRight - boardLeft, boardBottom - boardTop),
            style   = Stroke(width = 1.5f)
        )

        // Territory fills
        for (coord in board.allCoords()) {
            val cell = board.get(coord)
            if (cell.territory != Player.NONE) {
                val base = AppColors.territoryFor(cell.territory)
                drawRect(
                    color   = base.copy(alpha = base.alpha * territoryAlpha.value),
                    topLeft = Offset(ox + coord.col * cs - cs / 2f, oy + coord.row * cs - cs / 2f),
                    size    = Size(cs, cs)
                )
            }
        }

        // Grid (minor + major every 5)
        for (col in 0 until cols) {
            val x = ox + col * cs
            val isMajor = col % 5 == 0
            drawLine(
                color = if (isMajor) AppColors.GridMajor else AppColors.GridMinor,
                start = Offset(x, boardTop), end = Offset(x, boardBottom),
                strokeWidth = if (isMajor) 1.2f else 0.8f
            )
        }
        for (row in 0 until rows) {
            val y = oy + row * cs
            val isMajor = row % 5 == 0
            drawLine(
                color = if (isMajor) AppColors.GridMajor else AppColors.GridMinor,
                start = Offset(boardLeft, y), end = Offset(boardRight, y),
                strokeWidth = if (isMajor) 1.2f else 0.8f
            )
        }

        // Stones
        val dotRadius = cs * 0.36f
        for (coord in board.allCoords()) {
            val cell = board.get(coord)
            if (cell.dot != Player.NONE) {
                val isLast = coord == state.lastMove
                val r = if (isLast) dotRadius * dotScale.value else dotRadius
                if (r <= 0f) continue
                val cx = ox + coord.col * cs
                val cy = oy + coord.row * cs
                drawCircle(AppColors.DotShadow, radius = r, center = Offset(cx + 1.5f, cy + 2f))
                val baseColor = AppColors.forPlayer(cell.dot)
                val highlight = AppColors.highlightFor(cell.dot)
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

        // Last-move pulsing ring
        state.lastMove?.let { last ->
            if (board.isOnBoard(last)) {
                val cx = ox + last.col * cs
                val cy = oy + last.row * cs
                val pulseRadius = (dotRadius + 4f) + pulsePhase * cs * 0.25f
                val pulseAlpha = (1f - pulsePhase).coerceIn(0f, 1f)
                drawCircle(
                    color  = AppColors.LastMoveRing.copy(alpha = pulseAlpha * 0.85f),
                    radius = pulseRadius * dotScale.value,
                    center = Offset(cx, cy),
                    style  = Stroke(width = 2.5f)
                )
                drawCircle(
                    color  = AppColors.LastMoveRing,
                    radius = (dotRadius + 3f) * dotScale.value,
                    center = Offset(cx, cy),
                    style  = Stroke(width = 2f)
                )
            }
        }
    }
}

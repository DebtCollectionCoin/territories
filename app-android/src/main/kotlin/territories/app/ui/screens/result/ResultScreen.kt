package territories.app.ui.screens.result

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import territories.app.ui.screens.game.PlayerAColor
import territories.app.ui.screens.game.PlayerBColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random



@Composable
fun ResultScreen(
    winner:      String,
    scoreA:      Int,
    scoreB:      Int,
    scoreC:      Int = 0,
    scoreD:      Int = 0,
    moves:       Int,
    onPlayAgain: () -> Unit,
    onNewGame:   () -> Unit,
    onHome:      () -> Unit
) {
    val winnerColor = when (winner) {
        "Blue"   -> PlayerAColor
        "Red"    -> PlayerBColor
        "Green"  -> territories.app.ui.screens.game.PlayerCColor
        "Yellow" -> territories.app.ui.screens.game.PlayerDColor
        else     -> MaterialTheme.colorScheme.onBackground
    }

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Confetti behind content (only if there's a winner)
            if (winner != "Draw") {
                ConfettiBackdrop(primaryColor = winnerColor)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text  = "Game Over",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = when (winner) {
                        "Draw" -> "It's a Draw"
                        else   -> "$winner Wins!"
                    },
                    style      = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color      = winnerColor
                )

                Spacer(Modifier.height(36.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth(0.6f))
                Spacer(Modifier.height(20.dp))

                Text(
                    text  = "Final Score",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))

                ScoreRow(label = "Blue", score = scoreA, color = PlayerAColor, isWinner = winner == "Blue")
                Spacer(Modifier.height(8.dp))
                ScoreRow(label = "Red",  score = scoreB, color = PlayerBColor, isWinner = winner == "Red")
                if (scoreC > 0 || scoreD > 0) {
                    Spacer(Modifier.height(8.dp))
                    ScoreRow(label = "Green",  score = scoreC,
                        color = territories.app.ui.screens.game.PlayerCColor,
                        isWinner = winner == "Green")
                    if (scoreD > 0) {
                        Spacer(Modifier.height(8.dp))
                        ScoreRow(label = "Yellow", score = scoreD,
                            color = territories.app.ui.screens.game.PlayerDColor,
                            isWinner = winner == "Yellow")
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text  = "$moves moves",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )

                Spacer(Modifier.height(48.dp))

                Button(
                    onClick  = onPlayAgain,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Play Again", style = MaterialTheme.typography.titleMedium) }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick  = onNewGame,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("New Game") }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick  = onHome,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Home") }
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Int, color: Color, isWinner: Boolean) {
    Row(
        modifier            = Modifier
            .fillMaxWidth(0.7f)
            .background(
                color = if (isWinner) color.copy(alpha = 0.10f) else Color.Transparent
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment   = Alignment.CenterVertically
    ) {
        Text(
            text       = if (isWinner) "$label  ★" else label,
            style      = MaterialTheme.typography.titleMedium,
            color      = color,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.SemiBold
        )
        Text(
            text       = score.toString(),
            style      = MaterialTheme.typography.headlineSmall,
            color      = color,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** Lightweight confetti: animated falling colored streaks. */
@Composable
private fun ConfettiBackdrop(primaryColor: Color) {
    // Generate 60 confetti pieces with stable random offsets
    data class Piece(val x: Float, val phase: Float, val speed: Float, val color: Color, val w: Float, val h: Float)
    val pieces = remember {
        val palette = listOf(
            primaryColor,
            primaryColor.copy(alpha = 0.7f),
            Color(0xFFFFB300),
            Color(0xFFFFD54F),
            Color.White.copy(alpha = 0.9f)
        )
        val rng = Random(42)
        List(60) {
            Piece(
                x      = rng.nextFloat(),
                phase  = rng.nextFloat(),
                speed  = 0.6f + rng.nextFloat() * 0.8f,
                color  = palette[rng.nextInt(palette.size)],
                w      = 6f + rng.nextFloat() * 6f,
                h      = 10f + rng.nextFloat() * 14f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti-t"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (p in pieces) {
            val progress = ((t * p.speed) + p.phase) % 1f
            val y = progress * (h + 40f) - 20f
            // Sway horizontally
            val sway = sin((progress * 4f * PI).toFloat() + p.phase * 6f) * 18f
            val cx = p.x * w + sway
            val rotation = (progress * 360f * 2f) * (if (p.phase > 0.5f) 1f else -1f)
            rotate(degrees = rotation, pivot = Offset(cx, y)) {
                drawRect(
                    brush   = Brush.linearGradient(
                        colors = listOf(p.color, p.color.copy(alpha = 0.4f))
                    ),
                    topLeft = Offset(cx - p.w / 2, y - p.h / 2),
                    size    = androidx.compose.ui.geometry.Size(p.w, p.h)
                )
            }
        }
    }
}
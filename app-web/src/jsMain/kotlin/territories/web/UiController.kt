package territories.web

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import territories.engine.model.*
import kotlin.random.Random

/**
 * Bridges the DOM with [GameController] and [BoardRenderer].
 * Manages overlays, settings, and confetti.
 */
class UiController(
    private val game: GameController,
    private val renderer: BoardRenderer
) {
    // ── HUD ───────────────────────────
    private val scoreAVal by lazy { document.getElementById("score-a-val") as HTMLElement }
    private val scoreBVal by lazy { document.getElementById("score-b-val") as HTMLElement }
    private val turnLabel by lazy { document.getElementById("turn-label") as HTMLElement }
    private val canvas by lazy { document.getElementById("board") as HTMLCanvasElement }

    private val btnUndo by lazy { document.getElementById("btn-undo") as HTMLButtonElement }
    private val btnSurrender by lazy { document.getElementById("btn-surrender") as HTMLButtonElement }
    private val btnMenu by lazy { document.getElementById("btn-menu") as HTMLButtonElement }

    // ── Overlays ──────────────────────
    private val overlayHome by lazy { document.getElementById("overlay-home") as HTMLElement }
    private val overlaySetup by lazy { document.getElementById("overlay-setup") as HTMLElement }
    private val overlayResult by lazy { document.getElementById("overlay-result") as HTMLElement }
    private val overlaySettings by lazy { document.getElementById("overlay-settings") as HTMLElement }
    private val overlayAbout by lazy { document.getElementById("overlay-about") as HTMLElement }
    private val overlayMenu by lazy { document.getElementById("overlay-menu") as HTMLElement }
    private val overlayAi by lazy { document.getElementById("overlay-ai") as HTMLElement }

    // Home buttons
    private val btnResume by lazy { document.getElementById("btn-resume") as HTMLButtonElement }
    private val btnNew by lazy { document.getElementById("btn-new") as HTMLButtonElement }
    private val btnSettings by lazy { document.getElementById("btn-settings") as HTMLButtonElement }
    private val btnAbout by lazy { document.getElementById("btn-about") as HTMLButtonElement }

    // Setup
    private val btnStart by lazy { document.getElementById("btn-start") as HTMLButtonElement }
    private val btnSetupBack by lazy { document.getElementById("btn-setup-back") as HTMLButtonElement }

    // Result
    private val resultTitle by lazy { document.getElementById("result-title") as HTMLElement }
    private val resultMeta by lazy { document.getElementById("result-meta") as HTMLElement }
    private val resultScoreA by lazy { document.getElementById("result-score-a") as HTMLElement }
    private val resultScoreB by lazy { document.getElementById("result-score-b") as HTMLElement }
    private val confettiLayer by lazy { document.getElementById("confetti") as HTMLElement }
    private val btnPlayAgain by lazy { document.getElementById("btn-play-again") as HTMLButtonElement }
    private val btnNewGame by lazy { document.getElementById("btn-new-game") as HTMLButtonElement }
    private val btnResultHome by lazy { document.getElementById("btn-result-home") as HTMLButtonElement }

    // Settings
    private val toggleSound by lazy { document.getElementById("toggle-sound") as HTMLInputElement }
    private val togglePulse by lazy { document.getElementById("toggle-pulse") as HTMLInputElement }
    private val btnClearSaved by lazy { document.getElementById("btn-clear-saved") as HTMLButtonElement }
    private val btnSettingsBack by lazy { document.getElementById("btn-settings-back") as HTMLButtonElement }

    // About
    private val btnAboutBack by lazy { document.getElementById("btn-about-back") as HTMLButtonElement }

    // In-game menu
    private val btnMenuResume by lazy { document.getElementById("btn-menu-resume") as HTMLButtonElement }
    private val btnMenuShare by lazy { document.getElementById("btn-menu-share") as HTMLButtonElement }
    private val btnMenuSettings by lazy { document.getElementById("btn-menu-settings") as HTMLButtonElement }
    private val btnMenuNewgame by lazy { document.getElementById("btn-menu-newgame") as HTMLButtonElement }
    private val btnMenuHome by lazy { document.getElementById("btn-menu-home") as HTMLButtonElement }

    // ── State ─────────────────────────
    private val choices: MutableMap<String, String> = mutableMapOf(
        "size"     to "medium",
        "scoring"  to "territory",
        "opponent" to "medium",
        "first"    to "a"
    )
    private var lastReportedGameOver: Boolean = false
    private var lastSettingsBackTarget: HTMLElement? = null

    fun init() {
        // Game callbacks
        game.onStateChanged = { state -> render(state) }
        game.onAiThinking = { thinking ->
            overlayAi.style.display = if (thinking) "flex" else "none"
        }
        game.onMovePlaced = { captured ->
            if (captured) {
                SoundFx.capture()
                game.currentState?.lastMove?.let { coord ->
                    val justMoved = if (game.currentState?.currentPlayer == Player.A) Player.B else Player.A
                    renderer.triggerCaptureRipple(coord, justMoved)
                }
            } else {
                SoundFx.place()
            }
        }

        // Init settings UI
        toggleSound.checked = Settings.soundEnabled
        togglePulse.checked = Settings.pulseEnabled
        toggleSound.addEventListener("change", {
            Settings.soundEnabled = toggleSound.checked
            if (toggleSound.checked) SoundFx.click()
        })
        togglePulse.addEventListener("change", {
            Settings.pulseEnabled = togglePulse.checked
            game.currentState?.let { renderer.render(it) }
        })

        // Choice cards
        document.querySelectorAll(".choice-grid").asList().forEach { gridNode ->
            val grid = gridNode as HTMLElement
            val group = grid.dataset["group"] ?: return@forEach
            grid.querySelectorAll(".choice").asList().forEach { choiceNode ->
                val btn = choiceNode as HTMLElement
                btn.addEventListener("click", {
                    SoundFx.click()
                    grid.querySelectorAll(".choice").asList().forEach { c ->
                        (c as HTMLElement).classList.remove("selected")
                    }
                    btn.classList.add("selected")
                    val v = btn.dataset["value"] ?: return@addEventListener
                    choices[group] = v
                })
            }
        }

        // Canvas input
        canvas.addEventListener("pointerdown", { event ->
            val e = event as MouseEvent
            event.preventDefault()
            SoundFx.ensureContext() // user gesture unlocks audio
            val rect = canvas.getBoundingClientRect()
            val x = e.clientX.toDouble() - rect.left
            val y = e.clientY.toDouble() - rect.top
            val state = game.currentState ?: return@addEventListener
            val coord = renderer.canvasToCoord(state.board, x, y) ?: return@addEventListener
            game.humanMove(coord)
        })

        // HUD controls
        btnUndo.addEventListener("click", { SoundFx.click(); game.undo() })
        btnSurrender.addEventListener("click", {
            if (window.confirm("Surrender this game?")) {
                SoundFx.click(); game.surrender()
            }
        })
        btnMenu.addEventListener("click", { SoundFx.click(); show(overlayMenu) })

        // Home
        btnResume.addEventListener("click", {
            SoundFx.click()
            val saved = SavedGameStore.load()
            if (saved != null && game.resumeGame(saved)) {
                hideAll()
            } else {
                btnResume.style.display = "none"
                showSetup()
            }
        })
        btnNew.addEventListener("click", { SoundFx.click(); showSetup() })
        btnSettings.addEventListener("click", {
            SoundFx.click()
            lastSettingsBackTarget = overlayHome
            show(overlaySettings)
        })
        btnAbout.addEventListener("click", { SoundFx.click(); show(overlayAbout) })

        // Setup
        btnSetupBack.addEventListener("click", { SoundFx.click(); show(overlayHome) })
        btnStart.addEventListener("click", {
            SoundFx.click()
            val config = buildConfig()
            val pBType = parseOpponent(choices["opponent"] ?: "medium")
            game.startGame(config, PlayerType.HUMAN, pBType)
            hideAll()
        })

        // Result
        btnPlayAgain.addEventListener("click", {
            SoundFx.click()
            val config = buildConfig()
            val pBType = parseOpponent(choices["opponent"] ?: "medium")
            game.startGame(config, PlayerType.HUMAN, pBType)
            hideAll()
            stopConfetti()
        })
        btnNewGame.addEventListener("click", {
            SoundFx.click()
            stopConfetti()
            game.quit()
            showSetup()
        })
        btnResultHome.addEventListener("click", {
            SoundFx.click()
            stopConfetti()
            game.quit()
            showHome()
        })

        // Settings
        btnSettingsBack.addEventListener("click", {
            SoundFx.click()
            show(lastSettingsBackTarget ?: overlayHome)
        })
        btnClearSaved.addEventListener("click", {
            SoundFx.click()
            SavedGameStore.clear()
            btnResume.style.display = "none"
            btnClearSaved.textContent = "✓ Cleared"
            window.setTimeout({ btnClearSaved.textContent = "Clear Saved Game" }, 1500)
        })

        // About
        btnAboutBack.addEventListener("click", { SoundFx.click(); show(overlayHome) })

        // In-game menu
        btnMenuResume.addEventListener("click", { SoundFx.click(); hide(overlayMenu) })
        btnMenuShare.addEventListener("click", {
            SoundFx.click()
            val saved = game.currentSavedGame()
            if (saved == null) {
                btnMenuShare.textContent = "No game to share"
            } else {
                val url = SavedGameStore.buildShareUrl(saved)
                copyToClipboard(url) { ok ->
                    btnMenuShare.textContent = if (ok) "✓ Link copied" else "Copy failed"
                }
            }
            window.setTimeout({ btnMenuShare.textContent = "Copy share link" }, 1500)
        })
        btnMenuSettings.addEventListener("click", {
            SoundFx.click()
            lastSettingsBackTarget = overlayMenu
            show(overlaySettings)
        })
        btnMenuNewgame.addEventListener("click", {
            SoundFx.click()
            game.quit()
            showSetup()
        })
        btnMenuHome.addEventListener("click", {
            SoundFx.click()
            game.quit()
            showHome()
        })

        // Keyboard
        document.addEventListener("keydown", { event ->
            val e = event as KeyboardEvent
            when {
                e.key == "Escape" -> {
                    if (overlayMenu.style.display == "flex") hide(overlayMenu)
                    else if (game.isGameActive) show(overlayMenu)
                }
                (e.ctrlKey || e.metaKey) && e.key.lowercase() == "z" -> {
                    e.preventDefault(); game.undo()
                }
            }
        })

        // Window resize
        window.addEventListener("resize", { game.currentState?.let { renderer.render(it) } })

        // Render loop — only runs when something is animating
        startRenderLoop()

        // Show home; offer Resume if saved game exists
        if (SavedGameStore.hasSaved()) btnResume.style.display = ""

        // Auto-resume from #g=... share link if present (preferred over localStorage).
        val shared = SavedGameStore.consumeFragment()
        if (shared != null && game.resumeGame(shared)) {
            hideAll()
        } else {
            showHome()
        }
    }

    // ── Render loop (paused when nothing animating) ─────
    private fun startRenderLoop() {
        var last = 0.0
        fun loop() {
            val state = game.currentState
            if (state != null && renderer.hasActiveAnimation(state)) {
                val now = js("Date.now()") as Double
                if (now - last > 30.0) {  // ~33fps
                    renderer.render(state)
                    last = now
                }
            }
            window.requestAnimationFrame { loop() }
        }
        loop()
    }

    private fun render(state: GameState) {
        renderer.render(state)

        scoreAVal.textContent = state.score.playerA.toString()
        scoreBVal.textContent = state.score.playerB.toString()

        turnLabel.classList.remove("player-a", "player-b")
        when {
            state.isGameOver -> turnLabel.textContent = "Game Over"
            state.currentPlayer == Player.A -> {
                turnLabel.textContent = "Blue's turn"
                turnLabel.classList.add("player-a")
            }
            else -> {
                turnLabel.textContent = "Red's turn"
                turnLabel.classList.add("player-b")
            }
        }

        btnUndo.disabled = !game.canUndo()

        if (state.isGameOver && !lastReportedGameOver) {
            lastReportedGameOver = true
            showResult(state)
            SoundFx.win()
        }
        if (!state.isGameOver) lastReportedGameOver = false
    }

    private fun showResult(state: GameState) {
        SavedGameStore.clear()
        resultScoreA.textContent = state.score.playerA.toString()
        resultScoreB.textContent = state.score.playerB.toString()
        resultMeta.textContent = "${state.moveCount} moves"

        resultTitle.classList.remove("player-a", "player-b", "draw")
        resultTitle.textContent = when (state.winner) {
            Player.A -> "Blue Wins!"
            Player.B -> "Red Wins!"
            Player.NONE -> "It's a Draw"
        }
        when (state.winner) {
            Player.A -> resultTitle.classList.add("player-a")
            Player.B -> resultTitle.classList.add("player-b")
            Player.NONE -> resultTitle.classList.add("draw")
        }

        if (state.winner != Player.NONE) {
            spawnConfetti(state.winner)
        }
        show(overlayResult)
    }

    // ── Confetti ────────────────────
    private fun spawnConfetti(winner: Player) {
        stopConfetti()
        val baseColor = if (winner == Player.A) "#3949ab" else "#d84a4a"
        val accentColor = if (winner == Player.A) "#7986cb" else "#e57373"
        val palette = listOf(baseColor, accentColor, "#ffb300", "#ffd54f", "#ffffff")
        repeat(80) {
            val piece = document.createElement("div") as HTMLElement
            piece.className = "confetti-piece"
            val left = Random.nextDouble(0.0, 100.0)
            val dx = Random.nextDouble(-150.0, 150.0)
            val duration = Random.nextDouble(2.4, 4.2)
            val delay = Random.nextDouble(0.0, 1.0)
            val color = palette[Random.nextInt(palette.size)]
            val width = Random.nextDouble(6.0, 12.0)
            val height = Random.nextDouble(10.0, 18.0)
            piece.setAttribute(
                "style",
                "left:${left}%;" +
                "background:$color;" +
                "width:${width}px;height:${height}px;" +
                "animation-duration:${duration}s;" +
                "animation-delay:${delay}s;" +
                "--dx:${dx}px;"
            )
            confettiLayer.appendChild(piece)
        }
    }

    private fun stopConfetti() {
        while (confettiLayer.firstChild != null) {
            confettiLayer.removeChild(confettiLayer.firstChild!!)
        }
    }

    // ── Overlay helpers ─────────────
    private fun show(target: HTMLElement) {
        hideAll()
        target.style.display = "flex"
    }
    private fun hide(target: HTMLElement) {
        target.style.display = "none"
    }
    private fun hideAll() {
        listOf(overlayHome, overlaySetup, overlayResult, overlaySettings, overlayAbout, overlayMenu)
            .forEach { it.style.display = "none" }
    }
    private fun showHome() {
        if (SavedGameStore.hasSaved()) btnResume.style.display = "" else btnResume.style.display = "none"
        show(overlayHome)
    }
    private fun showSetup() {
        // Sync UI selection to current choices map
        choices.forEach { (group, value) ->
            val grid = document.querySelector(".choice-grid[data-group='$group']") as? HTMLElement ?: return@forEach
            grid.querySelectorAll(".choice").asList().forEach { node ->
                val btn = node as HTMLElement
                if (btn.dataset["value"] == value) btn.classList.add("selected")
                else btn.classList.remove("selected")
            }
        }
        show(overlaySetup)
    }

    // ── Helpers ─────────────────────
    private fun buildConfig(): GameConfig {
        val preset = when (choices["size"]) {
            "small" -> GameConfig.SMALL
            "large" -> GameConfig.LARGE
            else    -> GameConfig.MEDIUM
        }
        val scoring = if (choices["scoring"] == "captured")
            ScoringVariant.CAPTURED_DOTS else ScoringVariant.TERRITORY_AREA
        val firstPlayer = when (choices["first"]) {
            "b" -> Player.B
            "random" -> if ((0..1).random() == 0) Player.A else Player.B
            else -> Player.A
        }
        return preset.copy(scoringVariant = scoring, firstPlayer = firstPlayer)
    }

    private fun parseOpponent(value: String): PlayerType = when (value) {
        "easy" -> PlayerType.AI_EASY
        "medium" -> PlayerType.AI_MEDIUM
        "hard" -> PlayerType.AI_HARD
        else -> PlayerType.HUMAN
    }
}

private fun NodeList.asList(): List<Node> {
    val out = ArrayList<Node>(length)
    for (i in 0 until length) out.add(item(i)!!)
    return out
}

/** Best-effort clipboard write that falls back to a hidden `<textarea>` + `document.execCommand`. */
private fun copyToClipboard(text: String, callback: (Boolean) -> Unit) {
    try {
        val nav = kotlinx.browser.window.navigator.asDynamic()
        val cb = nav.clipboard
        if (cb != null) {
            val promise = cb.writeText(text)
            promise.then({ callback(true) }, { _: Any? -> fallbackCopy(text, callback) })
            return
        }
    } catch (_: Throwable) { /* fall through */ }
    fallbackCopy(text, callback)
}

private fun fallbackCopy(text: String, callback: (Boolean) -> Unit) {
    try {
        val ta = kotlinx.browser.document.createElement("textarea") as HTMLTextAreaElement
        ta.value = text
        ta.style.position = "fixed"
        ta.style.opacity = "0"
        kotlinx.browser.document.body?.appendChild(ta)
        ta.select()
        val ok = kotlinx.browser.document.asDynamic().execCommand("copy") as? Boolean ?: false
        kotlinx.browser.document.body?.removeChild(ta)
        callback(ok)
    } catch (_: Throwable) {
        callback(false)
    }
}

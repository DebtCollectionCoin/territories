package territories.web

import kotlinx.browser.window

/**
 * Tiny synthesizer for game sound effects using the WebAudio API.
 * No external assets — generates short tones with envelopes.
 */
object SoundFx {
    private var ctx: dynamic = null
    private var ready: Boolean = false

    fun ensureContext() {
        if (ctx != null) return
        try {
            val AudioCtx: dynamic = js("window.AudioContext || window.webkitAudioContext")
            ctx = js("new AudioCtx()")
            ready = true
        } catch (_: Throwable) {
            ready = false
        }
    }

    private fun beep(freq: Double, durationMs: Int = 80, type: String = "sine", volume: Double = 0.10) {
        if (!Settings.soundEnabled) return
        ensureContext()
        val ac = ctx ?: return
        try {
            val osc = ac.createOscillator()
            val gain = ac.createGain()
            osc.type = type
            osc.frequency.value = freq
            val now = ac.currentTime as Double
            gain.gain.setValueAtTime(0.0, now)
            gain.gain.linearRampToValueAtTime(volume, now + 0.005)
            gain.gain.exponentialRampToValueAtTime(0.0001, now + durationMs / 1000.0)
            osc.connect(gain)
            gain.connect(ac.destination)
            osc.start()
            osc.stop(now + durationMs / 1000.0)
        } catch (_: Throwable) { /* ignore */ }
    }

    fun place()    { beep(freq = 520.0, durationMs = 60, type = "triangle", volume = 0.08) }
    fun capture()  {
        beep(freq = 660.0, durationMs = 90, type = "sine", volume = 0.10)
        window.setTimeout({ beep(freq = 880.0, durationMs = 110, type = "sine", volume = 0.10) }, 70)
    }
    fun win()      {
        beep(freq = 523.25, durationMs = 140, type = "triangle", volume = 0.12)
        window.setTimeout({ beep(freq = 659.25, durationMs = 140, type = "triangle", volume = 0.12) }, 130)
        window.setTimeout({ beep(freq = 783.99, durationMs = 220, type = "triangle", volume = 0.12) }, 260)
    }
    fun click()    { beep(freq = 320.0, durationMs = 35, type = "square", volume = 0.05) }
}

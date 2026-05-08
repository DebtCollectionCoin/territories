package territories.engine.ai

/**
 * Lightweight diagnostic logging for AI decision making.
 * Implementations forward to the platform's natural log channel
 * (console.log on JS, println on JVM, android.util.Log on Android).
 *
 * Toggle via [AiLog.enabled]. Default ON to make AI behaviour easy
 * to inspect during development.
 */
object AiLog {
    var enabled: Boolean = true

    fun log(msg: String) {
        if (enabled) platformLog(msg)
    }
}

internal expect fun platformLog(msg: String)

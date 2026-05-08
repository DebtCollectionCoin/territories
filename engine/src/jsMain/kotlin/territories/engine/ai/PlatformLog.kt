package territories.engine.ai

internal actual fun platformLog(msg: String) {
    js("console.log('[AI]', msg)")
}

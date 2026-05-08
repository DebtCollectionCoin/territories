package territories.engine.ai

actual fun currentTimeMs(): Long = (js("Date.now()") as Double).toLong()

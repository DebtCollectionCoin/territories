package territories.web

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

fun main() {
    window.onload = {
        val canvas = document.getElementById("board") as HTMLCanvasElement
        val gameController = GameController()
        val boardRenderer = BoardRenderer(canvas)
        val uiController = UiController(gameController, boardRenderer)
        uiController.init()
    }
}

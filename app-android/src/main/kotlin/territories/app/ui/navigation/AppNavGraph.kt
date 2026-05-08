package territories.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import territories.app.ui.screens.game.GameScreen
import territories.app.ui.screens.history.HistoryScreen
import territories.app.ui.screens.home.HomeScreen
import territories.app.ui.screens.howtoplay.HowToPlayScreen
import territories.app.ui.screens.result.ResultScreen
import territories.app.ui.screens.settings.SettingsScreen
import territories.app.ui.screens.setup.SetupScreen

sealed class Screen(val route: String) {
    object Home      : Screen("home")
    object Setup     : Screen("setup")
    object Game      : Screen("game")
    object HowToPlay : Screen("how_to_play")
    object History   : Screen("history")
    object Settings  : Screen("settings")
    object Result    : Screen("result/{winner}/{scoreA}/{scoreB}/{moves}") {
        fun withArgs(winner: String, scoreA: Int, scoreB: Int, moves: Int) =
            "result/$winner/$scoreA/$scoreB/$moves"
    }
}

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onNewGame    = { nav.navigate(Screen.Setup.route) },
                onHowToPlay  = { nav.navigate(Screen.HowToPlay.route) },
                onHistory    = { nav.navigate(Screen.History.route) },
                onSettings   = { nav.navigate(Screen.Settings.route) },
                onResume     = { nav.navigate(Screen.Game.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.History.route) {
            HistoryScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.HowToPlay.route) {
            HowToPlayScreen(onBack = { nav.popBackStack() })
        }

        composable(Screen.Setup.route) {
            SetupScreen(
                onBack  = { nav.popBackStack() },
                onStart = { nav.navigate(Screen.Game.route) {
                    popUpTo(Screen.Home.route)
                }}
            )
        }

        composable(Screen.Game.route) {
            GameScreen(
                onGameOver = { winner, scoreA, scoreB, moves ->
                    nav.navigate(Screen.Result.withArgs(winner, scoreA, scoreB, moves)) {
                        popUpTo(Screen.Game.route) { inclusive = true }
                    }
                },
                onNavigateHome = {
                    nav.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route     = Screen.Result.route,
            arguments = listOf(
                navArgument("winner") { type = NavType.StringType },
                navArgument("scoreA") { type = NavType.IntType },
                navArgument("scoreB") { type = NavType.IntType },
                navArgument("moves")  { type = NavType.IntType },
            )
        ) { back ->
            val winner = back.arguments?.getString("winner") ?: "Draw"
            val scoreA = back.arguments?.getInt("scoreA") ?: 0
            val scoreB = back.arguments?.getInt("scoreB") ?: 0
            val moves  = back.arguments?.getInt("moves")  ?: 0
            ResultScreen(
                winner    = winner,
                scoreA    = scoreA,
                scoreB    = scoreB,
                moves     = moves,
                onPlayAgain = { nav.navigate(Screen.Game.route) {
                    popUpTo(Screen.Home.route)
                }},
                onNewGame   = { nav.navigate(Screen.Setup.route) {
                    popUpTo(Screen.Home.route)
                }},
                onHome      = { nav.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }}
            )
        }
    }
}

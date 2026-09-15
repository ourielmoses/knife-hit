package com.knifehit.game.ui

sealed interface Screen {
    data object Home : Screen
    data object Playing : Screen
    data object GameOver : Screen
    data object Settings : Screen
    data object Shop : Screen
    data object Locker : Screen
    data object Worlds : Screen
    data object Pause : Screen
    data object Admin : Screen
}

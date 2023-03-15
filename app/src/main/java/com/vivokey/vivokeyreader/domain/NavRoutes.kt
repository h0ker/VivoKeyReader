package com.vivokey.vivokeyreader.domain

sealed class NavRoutes(val route: String) {
    object MainScreen: NavRoutes("main_screen")
}

package com.example.ocr_poc.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.ocr_poc.screens.ScanDocumentScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("main") { MainScreen(navController) }
        composable("scroll_screen") { ScrollScreenWithTabs(  onBackClick = { navController.popBackStack()}) }
        composable("scan_document") {
            ScanDocumentScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

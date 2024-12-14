@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ocr_poc.screens
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import com.example.ocr_poc.R
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.graphics.Brush

@Composable
fun ScrollScreenWithTabs(onBackClick: () -> Unit) {
    val navController = rememberNavController()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(
        TabItem("Scan & Edit", R.drawable.scanner),
        TabItem("Past Invoices", R.drawable.invoice)
    ) // Tabs with titles and drawable resources

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Invoices",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(start = 85.dp, top = 15.dp)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF055492)
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                modifier = Modifier.height(90.dp)
            )
        }
    ) { paddingValues ->
        // Apply gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF5F5F5), Color(0xFFB0BEC5)) // Gradient colors
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                // Rounded Tab Row
                Row(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5))
                ) {
                    tabs.forEachIndexed { index, tabItem ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                navController.navigate(tabItem.title) // Navigate dynamically
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = tabItem.drawable),
                                        contentDescription = "${tabItem.title} Icon",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .padding(end = 8.dp)
                                    )
                                    Text(
                                        tabItem.title,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            modifier = Modifier
                                .background(if (selectedTabIndex == index) Color(0xFFFFCC00) else Color.Transparent)
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }

                // Content Area
                NavHost(
                    navController = navController,
                    startDestination = tabs[0].title, // Use the first tab as the start destination
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    tabs.forEach { tab ->
                        composable(tab.title) { // Use dynamic routes based on tab names
                            when (tab.title) {
                                "Scan & Edit" -> ScanDocumentScreen(onBackClick = { navController.popBackStack() })
                                "Past Invoices" -> MainScreen(navController)
                                else -> Text("Unknown tab") // Fallback for unexpected cases
                            }
                        }
                    }
                }
            }
        }
    }
}

data class TabItem(val title: String, val drawable: Int)
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ocr_poc.screens

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
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun ScrollScreenWithTabs(onBackClick: () -> Unit) {
    val navController = rememberNavController()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Shipping", "Payment")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Invoice Screen",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(
                            horizontal = 30.dp,
                            vertical = 10.dp
                        ) // Padding around text
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(

                    containerColor = Color.Black, // No background color
//                    scrolledContainerColor = Color.Transparent, // Ensure transparency
//                    navigationIconContentColor = Color.Black

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
                modifier = Modifier
                    .height(50.dp), //custom height
                scrollBehavior = null
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Rounded Tab Row
            Row(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp)) // Rounded corners for TabRow
                    .background(Color(0xFFF5F5F5)) // Light gray background
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            navController.navigate(title)
                        },
                        text = {
                            Text(
                                title,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier
                            .background(if (selectedTabIndex == index) Color(0xFFFFCC00) else Color.Transparent)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp)) // Rounded tab
                    )
                }
            }

            // Content Area
            NavHost(
                navController = navController,
                startDestination = tabs[0],
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                composable("Shipping") { MainScreen(navController) }
                composable("Payment") {
                    ScanDocumentScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}


package com.example.ocr_poc.screens

import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.example.ocr_poc.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieCompositionSpec
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MainScreen(navController: NavController) {
    // Load the Lottie composition using the correct spec
    val composition =
        rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.firstscreen)) // animation.json in res/raw

    // Gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF90CAF9), // Light blue
                        Color(0xFF1E88E5)  // Deeper blue
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lottie Animation
            composition.value?.let {
                LottieAnimation(
                    composition = it,
                    iterations = Int.MAX_VALUE,
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 16.dp)
                )
            }

            Text(
                text = "Scan your bills,receipts and other documents in a better and easy way!",
                color = Color.White,
                lineHeight = 20.sp,
                fontWeight = FontWeight.W700,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 15.sp),
                modifier = Modifier
                    .padding(horizontal = 55.dp)
            )

            // Button with orange color
            Button(
                onClick = {
                    navController.navigate("scroll_screen")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726)), // Orange button
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Go To Next Page", fontSize = 16.sp, color = Color.White)
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = "Go to next page",
                        tint = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    MainScreen(navController = navController)
}

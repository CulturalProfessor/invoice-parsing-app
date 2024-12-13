package com.example.ocr_poc.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieCompositionSpec
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import com.example.ocr_poc.R


@Composable
fun SplashScreen(navController: NavHostController) {
    // Load the Lottie composition using the correct spec
    val composition = rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.billscan))

    // Column to arrange text and animation vertically
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Centers the content vertically
    ) {
        // "Snap" and "Invoice" text with different colors and padding
        Row(
            modifier = Modifier.padding(top = 70.dp) // Controls vertical positioning of the text
        ) {
            Text(
                text = "Snap",
                color = Color.Black,
                style = MaterialTheme.typography.headlineLarge, // Adjust font size here
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Invoice",
                color = Color(0xFF003365), // Dark blue color
                style = MaterialTheme.typography.headlineLarge, // Adjust font size here
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp) // Adds space between the two texts
            )
        }

        Text(
            text = "Scan, Save & Simplify",
            color = Color.Black, // Color for the new text
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp), // Adjust font size here
            modifier = Modifier.padding(top = 4.dp) // Reduced space from previous text
        )

        // Show Lottie animation when it's ready
        composition.value?.let {
            LottieAnimation(
                composition = it,
                modifier = Modifier
                    .width(350.dp) // Set width to control animation size
                    .height(350.dp) // Set height to control animation size
                    .padding(bottom = 5.dp)// Reduced the space between animation and text
            )
        }
    }

    // Delay of 2 seconds before navigating to the main screen
    LaunchedEffect(Unit) {
        delay(2000)  // 2 seconds
        navController.navigate("main") {
            // Avoid having multiple splash screens in the back stack
            popUpTo("splash") { inclusive = true }
        }
    }
}

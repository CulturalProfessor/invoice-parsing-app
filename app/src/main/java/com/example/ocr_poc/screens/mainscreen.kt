package com.example.ocr_poc.screens


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.example.ocr_poc.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.TextUnit

@Composable
fun MainScreen(navController: NavController) {
    // Load the Lottie composition using the correct spec
    val composition = rememberLottieComposition(spec = LottieCompositionSpec.RawRes(R.raw.firstscreen)) // animation.json in the res/raw folder

    // Column to arrange text and animation vertically
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
                    .padding(bottom = 16.dp) // Adjust the space between animation and text
            )
        }

        // Text below the animation
        Text(
            text = "Scan your bills,receipts and other documents in a better and easy way!",
            color = Color.Black,
            lineHeight = 20.sp,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 15.sp),
            modifier = Modifier
                .padding( start =40.dp, end =40.dp ),
        )

        // Button
        Button(
            onClick = {
                // Navigate to the NextPage
                navController.navigate("scan_document")
            },
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(0.8f)
        ) {
            Text("Go to Next Page", color = Color.White)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    MainScreen(navController = navController)
}

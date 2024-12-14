package com.example.ocr_poc

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.compose.setContent
import com.example.ocr_poc.screens.AppNavigation
import com.example.ocr_poc.ui.theme.OCR_POCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Set the status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        setContent {
            OCR_POCTheme {
                AppNavigation()
            }
        }
    }
}

package com.example.ocr_poc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ocr_poc.screens.AppNavigation
import com.example.ocr_poc.ui.theme.OCR_POCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OCR_POCTheme {
                AppNavigation()
            }
        }
    }
}

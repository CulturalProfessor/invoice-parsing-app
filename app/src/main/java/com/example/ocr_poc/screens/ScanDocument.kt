package com.example.ocr_poc.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocr_poc.EditTagsActivity
import com.example.ocr_poc.R
import com.example.ocr_poc.models.Invoice
import com.example.ocr_poc.models.InvoiceStorage
import com.example.ocr_poc.models.TextEntity
import com.example.ocr_poc.utils.Extraction
import com.example.ocr_poc.utils.TFLiteInterpreter
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDocumentScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope() // Coroutine scope for Compose

    val snackbarHostState = remember { SnackbarHostState() }
    val extraction = remember { Extraction<Any>(context) }

    val word2index = loadWord2Index(context, "word2index.json")
    val tfliteInterpreter = TFLiteInterpreter(context, "bilstm_crf_ner.tflite")
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val editTagsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedEntities =
                result.data?.getParcelableArrayListExtra<TextEntity>("UPDATED_ENTITIES")

            if (updatedEntities != null) {
                coroutineScope.launch {
                    // Save the updated entities to Codestore preferences
                    InvoiceStorage.addInvoice(context, Invoice(updatedEntities))
                }
            }
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
                .fromActivityResultIntent(result.data)
            val uri = scanResult?.pages?.firstOrNull()?.imageUri

            uri?.let {
                coroutineScope.launch {
                    val entities = extraction.processImageForText(
                        uri, textRecognizer, tfliteInterpreter, word2index
                    )
                    // Save entities to Codestore preferences
                    InvoiceStorage.addInvoice(context, Invoice(entities))
                    val intent = Intent(context, EditTagsActivity::class.java).apply {
                        putParcelableArrayListExtra("RECOGNIZED_ENTITIES", ArrayList(entities))
                    }
                    editTagsLauncher.launch(intent)
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF90CAF9), Color(0xFF1E88E5)) // New vibrant gradient
                        )
                    )
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Scanner Icon with subtle rounded background
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.scanner),
                            contentDescription = "Scanner Icon",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(100.dp)
                        )
                    }

                    // Title Text
                    Text(
                        text = "Scan Invoices to Extract Text",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    // Subtitle Text
                    Text(
                        text = "Click below to start scanning your documents",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Scan Document Button with elevated styling
                    Button(
                        onClick = {
                            GmsDocumentScanning.getClient(
                                GmsDocumentScannerOptions.Builder().build()
                            ).getStartScanIntent(activity!!).addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(
                                    androidx.activity.result.IntentSenderRequest.Builder(
                                        intentSender
                                    ).build()
                                )
                            }
                        },
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726)),
                        elevation = ButtonDefaults.buttonElevation(8.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Scan Document",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    )
}

private fun loadWord2Index(context: Context, fileName: String): Map<String, Int> {
    val inputStream = context.assets.open(fileName)
    val jsonString = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
    val jsonObject = JSONObject(jsonString)
    return jsonObject.keys().asSequence().associateWith { jsonObject.getInt(it) }
}
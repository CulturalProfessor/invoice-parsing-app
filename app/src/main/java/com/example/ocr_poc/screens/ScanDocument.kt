package com.example.ocr_poc.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
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

    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val extraction = remember { Extraction(context) }

    val word2index = loadWord2Index(context, "word2index.json")
    val tfliteInterpreter = TFLiteInterpreter(context, "bilstm_crf_ner.tflite")
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val editTagsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedEntities =
                result.data?.getParcelableArrayListExtra<TextEntity>("UPDATED_ENTITIES")
            val imageUri = imageUris.lastOrNull() // Retrieve the last scanned image URI

            if (updatedEntities != null && imageUri != null) {
                // Pass both imageUri and updatedEntities
                InvoiceStorage.addInvoice(imageUri, updatedEntities)
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
                imageUris = imageUris + it
                coroutineScope.launch {
                    val entities = extraction.processImageForText(
                        uri, textRecognizer, tfliteInterpreter, word2index
                    )
                    // Pass BOTH imageUri and entities to addInvoice
                    InvoiceStorage.addInvoice(it, entities)
                    val intent = Intent(context, EditTagsActivity::class.java).apply {
                        putParcelableArrayListExtra("RECOGNIZED_ENTITIES", ArrayList(entities))
                    }
                    editTagsLauncher.launch(intent)
                }
            }

        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = {
                padding->    Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFF5F5F5), Color(0xFFB0BEC5))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.scanner),
                    contentDescription = "Scanner Icon",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(120.dp)
                )
                Text(
                    text = "Scan invoices to extract text",
                    fontSize = 18.sp,
                    color = Color(0xFF455A64),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
                )
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(top = 8.dp)
                ) {
                    Text("Scan Document", color = Color.White, fontSize = 16.sp)
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
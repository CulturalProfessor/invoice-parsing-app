package com.example.ocr_poc

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.ocr_poc.models.TextEntity
import com.example.ocr_poc.ui.theme.OCR_POCTheme
import com.example.ocr_poc.utils.Extraction
import com.example.ocr_poc.utils.TFLiteInterpreter
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader


class MainActivity : ComponentActivity() {
    private lateinit var extraction: Extraction
    private var isTextExtracted by mutableStateOf(false)
    private lateinit var tfliteInterpreter: TFLiteInterpreter
    private lateinit var word2index: Map<String, Int>

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            word2index = loadWord2Index(this, "word2index.json")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to load word2index mapping: ${e.message}")
            return
        }

        try {
            tfliteInterpreter = TFLiteInterpreter(this, "bilstm_crf_ner.tflite")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to load model: ${e.message}")
            return
        }
        extraction = Extraction(applicationContext)

        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(5)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        setContent {
            OCR_POCTheme {
                var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                var recognizedEntities by remember { mutableStateOf<List<TextEntity>>(emptyList()) }
                var displayResults by remember { mutableStateOf(false) }
                val snackbarHostState = remember { SnackbarHostState() }

                // Launcher to receive results from EditTagsActivity
                val editTagsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        val updatedEntities =
                            result.data?.getParcelableArrayListExtra<TextEntity>("UPDATED_ENTITIES")
                        if (updatedEntities != null) {
                            recognizedEntities = updatedEntities
                            displayResults = true // Display results after editing
                        }
                    }
                }

                val scannerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = { result ->
                        if (result.resultCode == RESULT_OK) {
                            val scanResult =
                                GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                            imageUris =
                                imageUris + (scanResult?.pages?.map { page -> page.imageUri }
                                    ?: emptyList())
                            scanResult?.pdf?.let { pdf ->
                                val fos = FileOutputStream(File(filesDir, "scanned.pdf"))
                                contentResolver.openInputStream(pdf.uri)?.use { it.copyTo(fos) }
                            }

                            lifecycleScope.launch {
                                if (imageUris.isNotEmpty()) {
                                    val entities = extraction.processImageForText(
                                        imageUris.last(),
                                        textRecognizer,
                                        tfliteInterpreter,
                                        word2index
                                    )
                                    recognizedEntities = entities

                                    // Navigate to EditTagsActivity for editing
                                    val intent =
                                        Intent(this@MainActivity, EditTagsActivity::class.java)
                                    intent.putParcelableArrayListExtra(
                                        "RECOGNIZED_ENTITIES",
                                        ArrayList(entities)
                                    )
                                    editTagsLauncher.launch(intent)
                                }
                            }
                        }
                    }
                )

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Document Scanner") },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    content = { padding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!displayResults) {
                                Text(
                                    text = "Scan invoices to extract text",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    scanner.getStartScanIntent(this@MainActivity)
                                        .addOnSuccessListener {
                                            scannerLauncher.launch(
                                                IntentSenderRequest.Builder(it).build()
                                            )
                                        }
                                        .addOnFailureListener {
                                            lifecycleScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    it.message ?: "Error occurred"
                                                )
                                            }
                                        }
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .padding(8.dp)
                            ) {
                                Text(text = "Scan Documents", fontSize = 16.sp)
                            }

                            if (imageUris.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        imageUris = emptyList()
                                        recognizedEntities = emptyList()
                                        displayResults = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .padding(8.dp)
                                ) {
                                    Text(text = "Clear Invoices", fontSize = 16.sp)
                                }
                            }
                            if (displayResults) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Final Recognized Entities:",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        recognizedEntities.forEach { entity ->
                                            Text(
                                                text = "${entity.label}: ${entity.text}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))


                        }
                    }
                )
            }
        }
    }

    private fun loadWord2Index(context: Context, fileName: String): Map<String, Int> {
        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = bufferedReader.use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        val word2index = mutableMapOf<String, Int>()
        jsonObject.keys().forEach {
            word2index[it] = jsonObject.getInt(it)
        }
        return word2index
    }

    override fun onDestroy() {
        super.onDestroy()
        tfliteInterpreter.close()
    }
}

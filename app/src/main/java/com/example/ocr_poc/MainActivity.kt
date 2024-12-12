package com.example.ocr_poc

import android.content.Context
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
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
    private var recognizedText by mutableStateOf("")
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
                var isLoading by remember { mutableStateOf(false) }
                var recognizedEntities by remember { mutableStateOf<List<TextEntity>>(emptyList()) }
                val snackbarHostState = remember { SnackbarHostState() }

                val scannerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = {
                        isLoading = false
                        if (it.resultCode == RESULT_OK) {
                            val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                            imageUris = imageUris + (result?.pages?.map { page -> page.imageUri }
                                ?: emptyList())
                            result?.pdf?.let { pdf ->
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
                                    isTextExtracted = true
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
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item {
                                if (imageUris.isEmpty()) {
                                    Text(
                                        text = "No Documents Scanned",
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }

                            items(imageUris) { uri ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Scanned Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1.5f)
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        isLoading = true
                                        scanner.getStartScanIntent(this@MainActivity)
                                            .addOnSuccessListener {
                                                scannerLauncher.launch(
                                                    IntentSenderRequest.Builder(it).build()
                                                )
                                            }
                                            .addOnFailureListener {
                                                isLoading = false
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
                                    Text(text = "Scan More Documents", fontSize = 16.sp)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        imageUris = emptyList(); recognizedEntities = emptyList()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .padding(8.dp)
                                ) {
                                    Text(text = "Clear Scanned Images", fontSize = 16.sp)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "Recognized Text Entities:",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }

                            if (recognizedEntities.isNotEmpty()) {
                                items(recognizedEntities) { entity ->
                                    Text(
                                        text = entity.label,
                                        fontSize = 16.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Text(
                                        text = entity.text,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }

                            if (isLoading) {
                                item {
                                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                }
                            }
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

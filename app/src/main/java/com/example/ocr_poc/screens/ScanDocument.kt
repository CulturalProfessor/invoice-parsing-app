package com.example.ocr_poc.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.ocr_poc.EditTagsActivity
import com.example.ocr_poc.models.TextEntity
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDocumentScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var recognizedEntities by remember { mutableStateOf<List<TextEntity>>(emptyList()) }
    var displayResults by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val extraction = remember { Extraction(context) }

    val word2index: Map<String, Int> = try {
        loadWord2Index(context, "word2index.json")
    } catch (e: Exception) {
        Log.e("ScanDocumentScreen", "Failed to load word2index mapping: ${e.message}")
        emptyMap()
    }

    val tfliteInterpreter: TFLiteInterpreter = try {
        TFLiteInterpreter(context, "bilstm_crf_ner.tflite")
    } catch (e: Exception) {
        Log.e("ScanDocumentScreen", "Failed to load model: ${e.message}")
        throw e
    }

    val options = GmsDocumentScannerOptions.Builder()
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setGalleryImportAllowed(true)
        .setPageLimit(5)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .build()

    val scanner = GmsDocumentScanning.getClient(options)
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    val editTagsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedEntities =
                result.data?.getParcelableArrayListExtra<TextEntity>("UPDATED_ENTITIES")
            if (updatedEntities != null) {
                recognizedEntities = updatedEntities
                displayResults = true
            }
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult =
                GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            imageUris =
                imageUris + (scanResult?.pages?.map { page -> page.imageUri } ?: emptyList())
            scanResult?.pdf?.let { pdf ->
                val fos = FileOutputStream(File(context.filesDir, "scanned.pdf"))
                context.contentResolver.openInputStream(pdf.uri)?.use { it.copyTo(fos) }
            }

            lifecycleOwner.lifecycleScope.launch {
                if (imageUris.isNotEmpty()) {
                    val entities = extraction.processImageForText(
                        imageUris.last(),
                        textRecognizer,
                        tfliteInterpreter,
                        word2index
                    )
                    recognizedEntities = entities

                    val intent = Intent(context, EditTagsActivity::class.java)
                    intent.putParcelableArrayListExtra(
                        "RECOGNIZED_ENTITIES",
                        ArrayList(entities)
                    )
                    editTagsLauncher.launch(intent)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Document Scanner") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
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
                        activity?.let {
                            scanner.getStartScanIntent(it)
                                .addOnSuccessListener { intentSender ->
                                    scannerLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                                .addOnFailureListener {
                                    lifecycleOwner.lifecycleScope.launch {
                                        snackbarHostState.showSnackbar(
                                            it.message ?: "Error occurred"
                                        )
                                    }
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

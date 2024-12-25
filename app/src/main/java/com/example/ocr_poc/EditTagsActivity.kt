package com.example.ocr_poc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ocr_poc.models.TextEntity
import com.example.ocr_poc.ui.theme.OCR_POCTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete


class EditTagsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val recognizedEntities =
            intent.getParcelableArrayListExtra<TextEntity>("RECOGNIZED_ENTITIES") ?: emptyList()

        setContent {
            OCR_POCTheme {
                Surface(color = Color.White) {
                    EditTagsScreen(recognizedEntities) { updatedEntities ->
                        val resultIntent = Intent().apply {
                            putParcelableArrayListExtra("UPDATED_ENTITIES", ArrayList(updatedEntities))
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun EditTagsScreen(
    recognizedEntities: List<TextEntity>,
    onSave: (List<TextEntity>) -> Unit
) {
    var entities by remember { mutableStateOf(recognizedEntities) }
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A57D6))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Edit Recognized Tags",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entities.size) { index ->
                val entity = entities[index]

                // Box with Delete Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color(0xFFDADADA),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detail ${index + 1}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        // Delete Button
                        IconButton(onClick = {
                            entities = entities.toMutableList().apply { removeAt(index) }
                            Toast.makeText(context, "Item Deleted", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red
                            )
                        }
                    }

                    Text(
                        text = "Label:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = entity.label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Text:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        BasicTextField(
                            value = entity.text,
                            onValueChange = { newValue ->
                                entities = entities.toMutableList().apply {
                                    this[index] = this[index].copy(text = newValue)
                                }
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Row for Save Button and FAB
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Save Button
            Button(
                onClick = { onSave(entities) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Save Changes",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Floating Action Button
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFF0A57D6),
                contentColor = Color.White,
                modifier = Modifier.size(56.dp)
            ) {
                Text("+", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }

    // Show AddDetailsDialog when triggered
    if (showDialog) {
        AddDetailsDialog(
            onDismiss = { showDialog = false },
            onAdd = { label, text ->
                entities = entities + TextEntity(label = label, text = text)
                showDialog = false
                Toast.makeText(context, "Added: $label - $text", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddDetailsDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Details") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(label, text) }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



//
//package com.example.ocr_poc
//
//import android.content.Intent
//import android.net.Uri
//import android.os.Bundle
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.BasicTextField
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import com.example.ocr_poc.models.Invoice
//import com.example.ocr_poc.models.InvoiceStorage
//import com.example.ocr_poc.models.TextEntity
//import com.example.ocr_poc.ui.theme.OCR_POCTheme
//import kotlinx.coroutines.launch
//
//class EditTagsActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val imageUri = intent.getParcelableExtra<Uri>("IMAGE_URI")
//        val recognizedEntities = intent.getParcelableArrayListExtra<TextEntity>("RECOGNIZED_ENTITIES") ?: emptyList()
//
//        setContent {
//            OCR_POCTheme {
//                Surface(color = Color.White) {
//                    EditTagsScreen(imageUri, recognizedEntities) { updatedEntities ->
//                        val resultIntent = Intent().apply {
//                            putParcelableArrayListExtra("UPDATED_ENTITIES", ArrayList(updatedEntities))
//                        }
//                        setResult(RESULT_OK, resultIntent)
//                        finish()
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun EditTagsScreen(
//    imageUri: Uri?,
//    recognizedEntities: List<TextEntity>,
//    onSave: (List<TextEntity>) -> Unit
//) {
//    var entities by remember { mutableStateOf(recognizedEntities) }
//    var showDialog by remember { mutableStateOf(false) }
//    val context = LocalContext.current
//    val coroutineScope = rememberCoroutineScope()
//
//    Column(modifier = Modifier.fillMaxSize()) {
//        // Top Header Bar
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(Color(0xFF0A57D6))
//                .padding(16.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                text = "Edit Recognized Tags",
//                style = MaterialTheme.typography.titleLarge.copy(
//                    color = Color.White,
//                    fontWeight = FontWeight.Bold
//                )
//            )
//        }
//
//        // Content
//        LazyColumn(
//            modifier = Modifier
//                .weight(1f)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            items(entities.size) { index ->
//                val entity = entities[index]
//
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .border(1.dp, Color(0xFFDADADA), RoundedCornerShape(8.dp))
//                        .padding(12.dp)
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(bottom = 8.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = "Detail ${index + 1}",
//                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
//                        )
//                        IconButton(onClick = {
//                            entities = entities.toMutableList().apply { removeAt(index) }
//                            Toast.makeText(context, "Item Deleted", Toast.LENGTH_SHORT).show()
//                        }) {
//                            Icon(
//                                imageVector = Icons.Default.Delete,
//                                contentDescription = "Delete",
//                                tint = Color.Red
//                            )
//                        }
//                    }
//
//                    Text(text = "Label:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
//                    Text(entity.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
//
//                    Text(text = "Text:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(4.dp))
//                            .padding(8.dp)
//                    ) {
//                        BasicTextField(
//                            value = entity.text,
//                            onValueChange = { newValue ->
//                                entities = entities.toMutableList().apply {
//                                    this[index] = this[index].copy(text = newValue)
//                                }
//                            },
//                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Black),
//                            modifier = Modifier.fillMaxWidth()
//                        )
//                    }
//                }
//            }
//        }
//
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Button(
//                onClick = {
//                    if (imageUri != null) {
//                        coroutineScope.launch {
//                            InvoiceStorage.addOrUpdateInvoice(
//                                context,
//                                Invoice(imageUri, entities)
//                            )
//                            Toast.makeText(context, "Changes Saved", Toast.LENGTH_SHORT).show()
//                            onSave(entities)
//                        }
//                    } else {
//                        Toast.makeText(context, "Invalid Image URI", Toast.LENGTH_SHORT).show()
//                    }
//                },
//                modifier = Modifier
//                    .weight(1f)
//                    .height(48.dp),
//                shape = RoundedCornerShape(24.dp)
//            ) {
//                Text(
//                    text = "Save Changes",
//                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
//                )
//            }
//        }
//    }
//
//    if (showDialog) {
//        AddDetailsDialog(
//            onDismiss = { showDialog = false },
//            onAdd = { label, text ->
//                entities = entities + TextEntity(label = label, text = text)
//                showDialog = false
//                Toast.makeText(context, "Added: $label - $text", Toast.LENGTH_SHORT).show()
//            }
//        )
//    }
//}
//
//@Composable
//fun AddDetailsDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
//    var label by remember { mutableStateOf("") }
//    var text by remember { mutableStateOf("") }
//
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text(text = "Add Details") },
//        text = {
//            Column {
//                OutlinedTextField(
//                    value = label,
//                    onValueChange = { label = it },
//                    label = { Text("Label") },
//                    modifier = Modifier.fillMaxWidth()
//                )
//                Spacer(modifier = Modifier.height(8.dp))
//                OutlinedTextField(
//                    value = text,
//                    onValueChange = { text = it },
//                    label = { Text("Text") },
//                    modifier = Modifier.fillMaxWidth()
//                )
//            }
//        },
//        confirmButton = {
//            Button(onClick = { onAdd(label, text) }) {
//                Text("Add")
//            }
//        },
//        dismissButton = {
//            Button(onClick = onDismiss) {
//                Text("Cancel")
//            }
//        }
//    )
//}
//

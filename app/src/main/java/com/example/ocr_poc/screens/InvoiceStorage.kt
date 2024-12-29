package com.example.ocr_poc.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ocr_poc.EditTagsActivity
import com.example.ocr_poc.models.Invoice
import com.example.ocr_poc.models.InvoiceStorage
import com.example.ocr_poc.models.TextEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceStorageScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val invoices = remember { mutableStateListOf<Invoice>() }
    var selectedInvoice by remember { mutableStateOf<Invoice?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Invoice?>(null) }

    // Collect invoices from the DataStore
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                InvoiceStorage.getInvoices(context).collect { loadedInvoices ->
                    invoices.clear()
                    invoices.addAll(loadedInvoices)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load invoices: ${e.message}"
            }
        }
    }

    // Launcher for EditTagsActivity
    val editLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedEntities =
                result.data?.getParcelableArrayListExtra<TextEntity>("UPDATED_ENTITIES")
            val invoiceToUpdate = selectedInvoice
            if (updatedEntities != null && invoiceToUpdate != null) {
                coroutineScope.launch {
                    // Update the invoice in storage
                    InvoiceStorage.updateInvoice(context, invoiceToUpdate.copy(entities = updatedEntities), updatedEntities)

                    // Update the invoice in the UI
                    val index = invoices.indexOfFirst { it == invoiceToUpdate }
                    if (index != -1) {
                        invoices[index] = invoices[index].copy(entities = updatedEntities)
                    }
                    selectedInvoice = null
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (invoices.isEmpty()) {
                Text(
                    text = "No Saved Invoices",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    color = Color.Gray
                )
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    itemsIndexed(invoices) { index, invoice ->
                        ElevatedCard(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0xFF90CAF9), Color(0xFF1E88E5))
                                        )
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(16.dp)
                                        .clickable(enabled = true, onClick = {
                                            if (selectedInvoice == null) {
                                                selectedInvoice = invoice
                                            }
                                        })
                                ) {
                                    Text(
                                        text = "Invoice ${index + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Details = ${invoice.entities.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }

                                // Edit and Delete Buttons
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(end = 8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedInvoice = invoice
                                            val intent = Intent(context, EditTagsActivity::class.java)
                                            intent.putParcelableArrayListExtra(
                                                "RECOGNIZED_ENTITIES", ArrayList(invoice.entities)
                                            )
                                            editLauncher.launch(intent)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color(0xFFFFA726)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            showDeleteDialog = invoice
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Show Invoice Details Dialog
    selectedInvoice?.let { invoice ->
        InvoiceDetailDialog(invoice = invoice) { selectedInvoice = null }
    }

    // Show Delete Confirmation Dialog
    showDeleteDialog?.let { invoice ->
        DeleteConfirmationDialog(
            onConfirm = {
                coroutineScope.launch {
                    try {
                        InvoiceStorage.deleteInvoice(context, invoice)
                        invoices.remove(invoice)
                        showDeleteDialog = null
                    } catch (e: Exception) {
                        errorMessage = "Failed to delete invoice: ${e.message}"
                        showDeleteDialog = null
                    }
                }
            },
            onDismiss = { showDeleteDialog = null }
        )
    }
}

@Composable
fun InvoiceDetailDialog(invoice: Invoice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = {
            Text(
                "Invoice Details",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                invoice.entities.forEach { entity ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = entity.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = entity.text,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.DarkGray
                            )
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete Invoice") },
        text = { Text(text = "Are you sure you want to delete this invoice?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726))
            ) { Text("Yes", color = Color.White) }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726))
            ) { Text("No", color = Color.White) }
        }
    )
}

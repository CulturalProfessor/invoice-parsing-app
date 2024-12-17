package com.example.ocr_poc.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.example.ocr_poc.EditTagsActivity
import com.example.ocr_poc.models.Invoice
import com.example.ocr_poc.models.InvoiceStorage
import com.example.ocr_poc.models.TextEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceStorageScreen(onBackClick: () -> Unit) {
    val invoices = remember { mutableStateListOf<Invoice>().apply { addAll(InvoiceStorage.getInvoices()) } }
    val context = LocalContext.current
    var selectedInvoice by remember { mutableStateOf<Invoice?>(null) }

    // Launcher for EditTagsActivity
    val editLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedEntities =
                result.data?.getParcelableArrayListExtra<TextEntity>("UPDATED_ENTITIES")
            val invoiceToUpdate = selectedInvoice
            if (updatedEntities != null && invoiceToUpdate != null) {
                // Update Invoice in Storage
                InvoiceStorage.updateInvoice(invoiceToUpdate, updatedEntities)

                // Update Invoice in UI
                val index = invoices.indexOfFirst { it.imageUri == invoiceToUpdate.imageUri }
                if (index != -1) {
                    invoices[index] = invoices[index].copy(entities = updatedEntities)
                }
                selectedInvoice = null // Reset selected invoice
            }
        }
    }

    Scaffold(
containerColor = Color.Transparent
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (invoices.isEmpty()) {
                Text(
                    text = "No Saved Invoices",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    color = Color.Gray
                )
            } else {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    itemsIndexed(invoices) { index, invoice -> // Use itemsIndexed for numbering
                        ElevatedCard(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    // Open detailed dialog only when clicking outside the buttons
                                    selectedInvoice = invoice
                                },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0xFF90CAF9), Color(0xFF1E88E5)) // New vibrant gradient
                                        )
                                    ))
                          {
                                // Invoice Content
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.TopStart),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = invoice.imageUri,
                                        contentDescription = "Invoice Image",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    Column {
                                        Text(
                                            text = "Invoice ${index + 1}", // Dynamic title
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                              color = Color.White
                                        )
                                        Text(
                                            text = "Total Items: ${invoice.entities.size}",
                                            color = Color.White
                                        )
                                    }
                                }

                                // Edit and Delete Buttons at Top-Right Corner
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp), // Align to top-right
                                    horizontalArrangement = Arrangement.spacedBy(4.dp) // Reduced spacing
                                ) {
                                    IconButton(
                                        onClick = {
                                            selectedInvoice = invoice
                                            val intent = Intent(context, EditTagsActivity::class.java)
                                            intent.putParcelableArrayListExtra(
                                                "RECOGNIZED_ENTITIES", ArrayList(invoice.entities)
                                            )
                                            editLauncher.launch(intent)
                                        },
                                        modifier = Modifier.size(24.dp) // Smaller icon
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint =  Color(0xFFFFA726),
                                            modifier = Modifier.size(18.dp) // Adjust icon size
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            invoices.remove(invoice)
                                            InvoiceStorage.deleteInvoice(invoice)
                                        },
                                        modifier = Modifier.size(25.dp) // Smaller icon
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red,
                                            modifier = Modifier.size(35.dp) // Adjust icon size
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
}

@Composable
fun InvoiceDetailDialog(invoice: Invoice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Invoice Details", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                invoice.entities.forEach { entity ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = entity.label, // Label Text
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = entity.text, // Value Text
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

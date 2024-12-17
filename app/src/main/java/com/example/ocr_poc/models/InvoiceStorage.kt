package com.example.ocr_poc.models

import android.net.Uri

data class Invoice(
    val imageUri: Uri,
    val entities: List<TextEntity>
)

object InvoiceStorage {
    private val invoices = mutableListOf<Invoice>() // List of Invoice objects

    // Add an invoice with image URI and entities
    fun addInvoice(imageUri: Uri, entities: List<TextEntity>) {
        if (invoices.none { it.imageUri == imageUri }) { // Prevent duplicates
            invoices.add(Invoice(imageUri, entities))
        }
    }

    fun updateInvoice(invoiceToUpdate: Invoice, updatedEntities: List<TextEntity>) {
        val index = invoices.indexOfFirst { it.imageUri == invoiceToUpdate.imageUri }
        if (index != -1) {
            invoices[index] = invoices[index].copy(entities = updatedEntities)
        }
    }

    // Return the list of invoices
    fun getInvoices(): List<Invoice> = invoices

    fun deleteInvoice(invoice: Invoice) {
        invoices.remove(invoice)
    }

}
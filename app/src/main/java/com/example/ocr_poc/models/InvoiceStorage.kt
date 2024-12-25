//package com.example.ocr_poc.models
//
//import android.net.Uri
//
//data class Invoice(
//    val imageUri: Uri,
//    val entities: List<TextEntity>
//)
//
//object InvoiceStorage {
//    private val invoices = mutableListOf<Invoice>() // List of Invoice objects
//
//    // Add an invoice with image URI and entities
//    fun addInvoice(imageUri: Uri, entities: List<TextEntity>) {
//        if (invoices.none { it.imageUri == imageUri }) { // Prevent duplicates
//            invoices.add(Invoice(imageUri, entities))
//        }
//    }
//
//    fun updateInvoice(invoiceToUpdate: Invoice, updatedEntities: List<TextEntity>) {
//        val index = invoices.indexOfFirst { it.imageUri == invoiceToUpdate.imageUri }
//        if (index != -1) {
//            invoices[index] = invoices[index].copy(entities = updatedEntities)
//        }
//    }
//
//    // Return the list of invoices
//    fun getInvoices(): List<Invoice> = invoices
//
//    fun deleteInvoice(invoice: Invoice) {
//        invoices.remove(invoice)
//    }
//
//}

package com.example.ocr_poc.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ocr_poc.models.TextEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "invoice_storage")

data class Invoice(val entities: List<TextEntity>)

class InvoiceStorage(private val context: Context) {
    private val gson = Gson()
    private val invoicesKey = stringSetPreferencesKey("invoices")

    // Add an invoice
    suspend fun addInvoice(invoice: Invoice) {
        val jsonInvoice = gson.toJson(invoice)
        context.dataStore.edit { preferences ->
            val currentInvoices = preferences[invoicesKey] ?: emptySet()
            preferences[invoicesKey] = currentInvoices + jsonInvoice
        }
    }

    // Update an invoice
    suspend fun updateInvoice(oldInvoice: Invoice, updatedInvoice: Invoice) {
        val oldJson = gson.toJson(oldInvoice)
        val newJson = gson.toJson(updatedInvoice)
        context.dataStore.edit { preferences ->
            val currentInvoices = preferences[invoicesKey] ?: emptySet()
            preferences[invoicesKey] = currentInvoices - oldJson + newJson
        }
    }

    // Get all invoices
    fun getInvoices(): Flow<List<Invoice>> {
        return context.dataStore.data.map { preferences ->
            val invoiceJsonSet = preferences[invoicesKey] ?: emptySet()
            invoiceJsonSet.mapNotNull { json ->
                try {
                    gson.fromJson(json, Invoice::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    // Delete an invoice
    suspend fun deleteInvoice(invoice: Invoice) {
        val jsonInvoice = gson.toJson(invoice)
        context.dataStore.edit { preferences ->
            val currentInvoices = preferences[invoicesKey] ?: emptySet()
            preferences[invoicesKey] = currentInvoices - jsonInvoice
        }
    }
}

//
//package com.example.ocr_poc.models
//import android.net.Uri
//import android.content.Context
//import androidx.datastore.core.DataStore
//import androidx.datastore.preferences.core.Preferences
//import androidx.datastore.preferences.core.edit
//import androidx.datastore.preferences.core.stringPreferencesKey
//import androidx.datastore.preferences.preferencesDataStore
//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.firstOrNull
//import kotlinx.coroutines.flow.map
//
//private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("invoice_data_store")
//
//object InvoiceStorage {
//
//    private val gson = Gson()
//    private val INVOICES_KEY = stringPreferencesKey("invoices")
//
//    // Function to add or update an invoice
//    suspend fun addOrUpdateInvoice(context: Context, invoice: Invoice) {
//        context.dataStore.edit { preferences ->
//            val currentInvoices = getInvoices(context).firstOrNull() ?: emptyList()
//            val updatedInvoices = currentInvoices.toMutableList()
//
//            val index = updatedInvoices.indexOfFirst { it.imageUri == invoice.imageUri }
//            if (index != -1) {
//                updatedInvoices[index] = invoice // Update existing
//            } else {
//                updatedInvoices.add(invoice) // Add new
//            }
//
//            preferences[INVOICES_KEY] = gson.toJson(updatedInvoices)
//        }
//    }
//
//    // Function to retrieve all invoices as a Flow
//    fun getInvoices(context: Context): Flow<List<Invoice>> {
//        return context.dataStore.data.map { preferences ->
//            val json = preferences[INVOICES_KEY] ?: "[]"
//            val type = object : TypeToken<List<Invoice>>() {}.type
//            gson.fromJson(json, type)
//        }
//    }
//
//    // Function to delete an invoice
//    suspend fun deleteInvoice(context: Context, invoice: Invoice) {
//        context.dataStore.edit { preferences ->
//            val currentInvoices = getInvoices(context).firstOrNull() ?: emptyList()
//            val updatedInvoices = currentInvoices.filterNot { it.imageUri == invoice.imageUri }
//            preferences[INVOICES_KEY] = gson.toJson(updatedInvoices)
//        }
//    }
//}
//
//
//
//

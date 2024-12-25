//package com.example.ocr_poc.repository
//
//import android.content.Context
//import androidx.datastore.preferences.core.Preferences
//import androidx.datastore.preferences.core.edit
//import androidx.datastore.preferences.core.stringSetPreferencesKey
//import com.example.ocr_poc.datastore.InvoicePreferencesKeys
//import com.example.ocr_poc.datastore.invoicePreferencesDataStore
//import com.example.ocr_poc.models.Invoice
//import com.google.gson.Gson
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.map
//
//class InvoiceRepository(context: Context) {
//    private val dataStore = context.invoicePreferencesDataStore
//    private val gson = Gson()
//
//    // Fetch invoices
//    val invoices: Flow<List<Invoice>> = dataStore.data.map { preferences ->
//        preferences[InvoicePreferencesKeys.INVOICES]?.mapNotNull {
//            gson.fromJson(it, Invoice::class.java)
//        } ?: emptyList()
//    }
//
//    // Add invoice
//    suspend fun addInvoice(invoice: Invoice) {
//        dataStore.edit { preferences ->
//            val currentInvoices = preferences[InvoicePreferencesKeys.INVOICES]?.toMutableSet() ?: mutableSetOf()
//            currentInvoices.add(gson.toJson(invoice))
//            preferences[InvoicePreferencesKeys.INVOICES] = currentInvoices
//        }
//    }
//
//    // Update invoice
//    suspend fun updateInvoice(updatedInvoice: Invoice) {
//        dataStore.edit { preferences ->
//            val currentInvoices = preferences[InvoicePreferencesKeys.INVOICES]?.toMutableSet() ?: mutableSetOf()
//            currentInvoices.removeIf {
//                gson.fromJson(it, Invoice::class.java).imageUri == updatedInvoice.imageUri
//            }
//            currentInvoices.add(gson.toJson(updatedInvoice))
//            preferences[InvoicePreferencesKeys.INVOICES] = currentInvoices
//        }
//    }
//
//    // Delete invoice
//    suspend fun deleteInvoice(invoice: Invoice) {
//        dataStore.edit { preferences ->
//            val currentInvoices = preferences[InvoicePreferencesKeys.INVOICES]?.toMutableSet() ?: mutableSetOf()
//            currentInvoices.removeIf {
//                gson.fromJson(it, Invoice::class.java).imageUri == invoice.imageUri
//            }
//            preferences[InvoicePreferencesKeys.INVOICES] = currentInvoices
//        }
//    }
//}

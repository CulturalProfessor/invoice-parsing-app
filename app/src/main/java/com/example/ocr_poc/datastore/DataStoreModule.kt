package com.example.ocr_poc.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson


val Context.invoicePreferencesDataStore by preferencesDataStore(name = "invoices_preferences")

// Preference keys
object InvoicePreferencesKeys {
    val INVOICES = stringSetPreferencesKey("invoices_key")
}

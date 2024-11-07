package com.example.ocr_poc.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.ocr_poc.models.TextEntity
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityAnnotation
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractor
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

class Extraction(private val context: Context) {

    suspend fun processImageForText(uri: Uri, textRecognizer: TextRecognizer): List<TextEntity> {
        return withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val image = InputImage.fromBitmap(bitmap, 0)
                try {
                    val result = textRecognizer.process(image).awaitResult()
                    if (result == null || result.textBlocks.isEmpty()) {
                        Log.e("MLKit OCR", "No text found in the image.")
                        return@withContext emptyList()
                    }

                    val tolerance = 5
                    val rows: MutableMap<Int, MutableList<String>> = mutableMapOf()

                    for (block in result.textBlocks) {
                        for (line in block.lines) {
                            val topValue = line.boundingBox?.top ?: continue

                            var foundRow: MutableList<String>? = null
                            for ((rowTop, rowTexts) in rows) {
                                if (abs(rowTop - topValue) <= tolerance) {
                                    foundRow = rowTexts
                                    break
                                }
                            }

                            if (foundRow == null) {
                                foundRow = mutableListOf()
                                rows[topValue] = foundRow
                            }

                            foundRow.add(line.text)
                        }
                    }

                    val extractedTextEntities = mutableListOf<TextEntity>()
                    for ((_, rowTexts) in rows) {
                        val combinedRowText = rowTexts.joinToString(" ")

                        val entities = extractEntitiesFromLine(combinedRowText)
                        if (entities.isNotEmpty()) {
                            val entityDescriptions = mutableListOf<String>()
                            for (entityAnnotation in entities) {
                                for (entity in entityAnnotation.entities) {
                                    val entityType = getEntityTypeName(entity)
                                    entityDescriptions.add("$entityType: ${entityAnnotation.annotatedText}")
                                }
                            }
                            if (entityDescriptions.isNotEmpty()) {
                                extractedTextEntities.add(
                                    TextEntity(
                                        label = "Entities found: ${entityDescriptions.joinToString(", ")}",
                                        text = combinedRowText
                                    )
                                )
                            }
                        } else {
                            // Add only the plain text when no entities are found
                            extractedTextEntities.add(TextEntity("Text", combinedRowText))
                        }
                    }

                    extractedTextEntities
                } catch (e: Exception) {
                    Log.e("MLKit OCR", "Text recognition failed: ${e.message}")
                    emptyList()
                }
            } else {
                Log.e("MLKit OCR", "Failed to load bitmap")
                emptyList()
            }
        }
    }

    private suspend fun extractEntitiesFromLine(lineText: String): List<EntityAnnotation> {
        val entityExtractor: EntityExtractor? = try {
            EntityExtraction.getClient(
                EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
            )
        } catch (e: Exception) {
            Log.e("MLKit OCR", "Failed to initialize EntityExtractor: ${e.message}")
            null
        }

        if (entityExtractor == null) {
            Log.e("MLKit OCR", "EntityExtractor initialization returned null.")
            return emptyList()
        }

        val isModelDownloaded = entityExtractor.isModelDownloaded().awaitResult()
        if (!isModelDownloaded) {
            try {
                downloadModel(entityExtractor)
                Log.d("MLKit OCR", "Model downloaded successfully.")
            } catch (e: Exception) {
                Log.e("MLKit OCR", "Model download failed: ${e.message}")
                return emptyList()
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val params = EntityExtractionParams.Builder(lineText).build()
            entityExtractor.annotate(params).addOnSuccessListener { entityAnnotations ->
                    continuation.resume(entityAnnotations)
                }.addOnFailureListener { e ->
                    Log.e("MLKit OCR", "Entity extraction failed: ${e.message}")
                    continuation.resumeWithException(e)
                }
        }
    }

    private suspend fun downloadModel(entityExtractor: EntityExtractor) {
        return suspendCancellableCoroutine { continuation ->
            entityExtractor.downloadModelIfNeeded().addOnSuccessListener {
                    continuation.resume(Unit)
                }.addOnFailureListener { e ->
                    Log.e("MLKit OCR", "Model download failed: ${e.message}")
                    continuation.resumeWithException(e)
                }
        }
    }

    private fun getEntityTypeName(entity: Entity): String {
        return when (entity.type) {
            Entity.TYPE_ADDRESS -> "ADDRESS"
            Entity.TYPE_DATE_TIME -> "DATE_TIME"
            Entity.TYPE_EMAIL -> "EMAIL"
            Entity.TYPE_FLIGHT_NUMBER -> "FLIGHT_NUMBER"
            Entity.TYPE_IBAN -> "IBAN"
            Entity.TYPE_ISBN -> "ISBN"
            Entity.TYPE_MONEY -> "MONEY"
            Entity.TYPE_PAYMENT_CARD -> "PAYMENT_CARD"
            Entity.TYPE_PHONE -> "PHONE"
            Entity.TYPE_TRACKING_NUMBER -> "TRACKING_NUMBER"
            Entity.TYPE_URL -> "URL"
            else -> "UNKNOWN"
        }
    }
}

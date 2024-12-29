package com.example.ocr_poc.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.ocr_poc.models.TextEntity
import com.google.mlkit.nl.entityextraction.Entity
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

class Extraction(private val context: Context) {
    private val index2tag = mapOf(
        0 to "O",
        1 to "B-INVOICE", 2 to "I-INVOICE",
        3 to "B-DATE", 4 to "I-DATE",
        5 to "B-PO", 6 to "I-PO",
        7 to "B-VENDOR", 8 to "I-VENDOR",
        9 to "B-CUSTOMER", 10 to "I-CUSTOMER",
        11 to "B-ADDRESS", 12 to "I-ADDRESS",
        13 to "B-PHONE", 14 to "I-PHONE",
        15 to "B-EMAIL", 16 to "I-EMAIL",
        17 to "B-WEBSITE", 18 to "I-WEBSITE",
        19 to "B-ITEM", 20 to "I-ITEM",
        21 to "B-QUANTITY", 22 to "I-QUANTITY",
        23 to "B-PRICE", 24 to "I-PRICE",
        25 to "B-SUBTOTAL", 26 to "I-SUBTOTAL",
        27 to "B-TAX", 28 to "I-TAX",
        29 to "B-TOTAL", 30 to "I-TOTAL",
        31 to "B-PAYMENT", 32 to "I-PAYMENT",
        33 to "B-BANK", 34 to "I-BANK",
        35 to "B-NOTES", 36 to "I-NOTES",
        37 to "B-GST", 38 to "I-GST",
        39 to "B-TAX-COMPONENT", 40 to "I-TAX-COMPONENT"
    )

    suspend fun processImageForText(
        uri: Uri,
        textRecognizer: TextRecognizer,
        tfliteInterpreter: TFLiteInterpreter,
        word2index: Map<String, Int>
    ): List<TextEntity> {
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

                    val ocrText = result.text
                    val extractedEntities = mutableListOf<TextEntity>()

                    val mlkitEntities = extractMLKitEntities(ocrText)
                    extractedEntities.addAll(mlkitEntities)

                    val bilstmPredictions =
                        runBiLSTMPredictions(ocrText, tfliteInterpreter, word2index)

                    for ((token, tag) in bilstmPredictions) {
                        if (tag != "O" && !isMLKitTag(tag)) {
                            extractedEntities.add(TextEntity(label = tag, text = token))
                        }
                    }

                    val combined = combineEntitiesByTag(extractedEntities)

                    return@withContext applyRegexCorrections(combined)

                } catch (e: Exception) {
                    Log.e("MLKit OCR", "Text recognition failed: ${e.message}")
                    return@withContext emptyList()
                }
            } else {
                Log.e("MLKit OCR", "Failed to load bitmap")
                emptyList()
            }
        }
    }

    private fun combineEntitiesByTag(entities: List<TextEntity>): List<TextEntity> {
        val (mlKitEntities, modelEntities) = entities.partition { isMLKitTag(it.label) }

        val combinedModelEntities = modelEntities
            .groupBy { simplifyTag(it.label) }
            .map { (label, groupedEntities) ->
                TextEntity(
                    label = label,
                    text = groupedEntities.joinToString(" ") { it.text }
                )
            }

        return mlKitEntities + combinedModelEntities
    }

    private fun applyRegexCorrections(entities: List<TextEntity>): List<TextEntity> {
        val phoneRegex = Regex("""^\+?[1-9][0-9]{1,3}[-.\s]?[0-9]{2,4}[-.\s]?[0-9]{4,6}$""")
        val invalidPhoneRegex = Regex("""^\d+\.\d+\.\d+$""")
        val pinRegex = Regex("""^\d{5,6}$""")

        return entities.mapNotNull { entity ->
            val trimmedText = entity.text.trim()
            val matchedPhone = phoneRegex.matches(trimmedText)
            val excludedInvalidPhone = invalidPhoneRegex.matches(trimmedText)
            val matchedPin = pinRegex.matches(trimmedText)

            Log.d(
                "Regex Debug",
                "Text: '$trimmedText', PhoneMatch: $matchedPhone, InvalidPhoneMatch: $excludedInvalidPhone, PinMatch: $matchedPin"
            )

            // Skip invalid phone formats
            if (excludedInvalidPhone) {
                Log.d("Entity Correction", "Excluding invalid phone: $trimmedText")
                return@mapNotNull null
            }

            val correctedLabel = when {
                matchedPhone -> "PHONE"
                matchedPin -> "PINCODE"
                else -> entity.label
            }

            Log.d(
                "Entity Correction",
                "Original label: ${entity.label}, Corrected label: $correctedLabel for text: '$trimmedText'"
            )

            entity.copy(label = correctedLabel)
        }
    }



    private fun simplifyTag(tag: String): String {
        return if (tag.startsWith("B-") || tag.startsWith("I-")) {
            tag.substring(2)
        } else {
            tag
        }
    }


    private suspend fun extractMLKitEntities(text: String): List<TextEntity> {
        val entityExtractor = try {
            EntityExtraction.getClient(
                EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
            )
        } catch (e: Exception) {
            Log.e("MLKit Entity", "Failed to initialize EntityExtractor: ${e.message}")
            null
        }

        if (entityExtractor == null) return emptyList()

        val isModelDownloaded = entityExtractor.isModelDownloaded().awaitResult()
        if (!isModelDownloaded) {
            try {
                downloadModel(entityExtractor)
            } catch (e: Exception) {
                Log.e("MLKit Entity", "Failed to download model: ${e.message}")
                return emptyList()
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val params = EntityExtractionParams.Builder(text).build()
            entityExtractor.annotate(params)
                .addOnSuccessListener { annotations ->
                    val extractedEntities = annotations.flatMap { annotation ->
                        annotation.entities.mapNotNull { entity ->
                            val entityType = getEntityTypeName(entity)
                            if (entityType != "UNKNOWN") {
                                TextEntity(label = entityType, text = annotation.annotatedText)
                            } else null
                        }
                    }
                    continuation.resume(extractedEntities)
                }
                .addOnFailureListener { e ->
                    Log.e("MLKit Entity", "Entity extraction failed: ${e.message}")
                    continuation.resumeWithException(e)
                }
        }
    }

    private fun isMLKitTag(tag: String): Boolean {
        return tag in listOf("ADDRESS", "DATE_TIME", "EMAIL", "PHONE", "URL")
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

    private fun runBiLSTMPredictions(
        ocrText: String,
        tfliteInterpreter: TFLiteInterpreter,
        word2index: Map<String, Int>
    ): Array<Pair<String, String>> {
        return try {
            tfliteInterpreter.predict(ocrText, word2index, index2tag)
        } catch (e: Exception) {
            Log.e("BiLSTM NER", "Prediction failed: ${e.message}")
            emptyArray()
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

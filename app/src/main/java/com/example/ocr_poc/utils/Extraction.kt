package com.example.ocr_poc.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
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
import kotlin.math.abs


class Extraction<Text>(private val context: Context) {
    val index2tag = mapOf(
        0 to "--PADDING--",
        1 to "O",
        2 to "B-INVOICE",
        3 to "I-INVOICE",
        4 to "B-PAN",
        5 to "I-PAN",
        6 to "B-VENDOR",
        7 to "I-VENDOR",
        8 to "B-CUSTOMER",
        9 to "I-CUSTOMER",
        10 to "B-PHONE",
        11 to "I-PHONE",
        12 to "B-EMAIL",
        13 to "I-EMAIL",
        14 to "B-ITEM",
        15 to "I-ITEM",
        16 to "B-QUANTITY",
        17 to "I-QUANTITY",
        18 to "B-PRICE",
        19 to "I-PRICE",
        20 to "B-SUBTOTAL",
        21 to "I-SUBTOTAL",
        22 to "B-TAX",
        23 to "I-TAX",
        24 to "B-TOTAL",
        25 to "I-TOTAL",
        26 to "B-GST",
        27 to "I-GST"
    )

    @Throws(java.lang.Exception::class)
    private fun loadHighResBitmap(uri: Uri): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw java.lang.Exception("Failed to open input stream")

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        options.inScaled = false

        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        return correctOrientation(uri, bitmap)
    }

    @Throws(java.lang.Exception::class)
    private fun correctOrientation(uri: Uri, bitmap: Bitmap?): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw java.lang.Exception("Failed to open input stream for EXIF data")

        val exif: ExifInterface = ExifInterface(inputStream)
        val orientation: Int =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val matrix: Matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
            else -> return bitmap
        }

        return Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun toHighQualityGrayscale(src: Bitmap): Bitmap {
        val bmpGrayscale = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpGrayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.setColorFilter(ColorMatrixColorFilter(colorMatrix))
        canvas.drawBitmap(src, 0f, 0f, paint)
        return bmpGrayscale
    }


    suspend fun processImageForText(
        uri: Uri,
        textRecognizer: TextRecognizer,
        tfliteInterpreter: TFLiteInterpreter,
        word2index: Map<String, Int>
    ): List<TextEntity> {
        return withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = loadHighResBitmap(uri)
            inputStream?.close()

            if (bitmap != null) {
                val grayscaleBitmap = toHighQualityGrayscale(bitmap)
                val image = InputImage.fromBitmap(grayscaleBitmap, 0)

                try {
                    val result = textRecognizer.process(image).awaitResult()
                    if (result == null || result.textBlocks.isEmpty()) {
                        Log.e("MLKit OCR", "No text found in the image.")
                        return@withContext emptyList()
                    }

                    // Format OCR text using bounding boxes
                    val formattedText = formatTextWithBoundingBoxes(result)
                    Log.d("Formatted OCR Text", formattedText)

                    val extractedEntities = mutableListOf<TextEntity>()
                    val mlkitEntities = extractMLKitEntities(formattedText)
                    Log.d("MLKit Entities", mlkitEntities.toString())
                    extractedEntities.addAll(mlkitEntities)
                    val bilstmPredictions =
                        runBiLSTMPredictions(formattedText, tfliteInterpreter, word2index)
                    Log.d("BiLSTM Predictions", bilstmPredictions.contentToString())
                    for ((token, tag) in bilstmPredictions) {
                        if (tag != "O") {
                            extractedEntities.add(TextEntity(label = tag, text = token))
                        }
                    }
                    // adding gstin even if it is not detected by bilstm
                    val missingTags =
                        listOf(
                            "B-TAX",
                            "B-GST",
                            "B-GSTIN",
                            "B-INVOICE",
                        )

                    val proximityEntities = proximityEntities(formattedText, missingTags)
                    extractedEntities.addAll(proximityEntities)

                    val combined = combineEntitiesByTag(extractedEntities)
                    val regexCorrectedEntities = applyRegexCorrections(combined)

                    val uniqueEntities = getUniqueEntities(regexCorrectedEntities)
                    return@withContext uniqueEntities

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

    private fun getUniqueEntities(entities: List<TextEntity>): List<TextEntity> {
        val uniqueEntities = mutableMapOf<String, MutableSet<String>>()

        entities.forEach { entity ->
            val label = entity.label
            val texts = entity.text.split(", ").map { it.trim() }
            if (label !in uniqueEntities) {
                uniqueEntities[label] = mutableSetOf()
            }
            uniqueEntities[label]?.addAll(texts)
        }

        return uniqueEntities.map { (label, texts) ->
            TextEntity(label = label, text = texts.joinToString(", "))
        }
    }

    private fun proximityEntities(
        formattedText: String,
        missingTags: List<String>
    ): List<TextEntity> {
        val lines = formattedText.split("\n")
        val entities = mutableListOf<TextEntity>()

        for (i in lines.indices) {
            val line = lines[i]

            for (tag in missingTags) {
                val simplifiedTag = simplifyTag(tag)
                var valueLine: String? = null

                if (line.contains(simplifiedTag, ignoreCase = true)) {
                    if (simplifiedTag == "INVOICE") {
                        // Special handling for "INVOICE NO" variations
                        valueLine =
                            Regex("(?i)INVOICE\\s+No\\.?\\s*:?\\s*(\\S+)").find(line)?.groupValues?.get(
                                1
                            )?.trim()
                    } else {
                        // General fallback for other tags
                        valueLine =
                            lines.getOrNull(i + 1)?.split(":", limit = 2)?.getOrNull(1)?.trim()
                                ?: lines.getOrNull(i + 1)?.trim()
                    }
                }

                if (!valueLine.isNullOrEmpty()) {
                    // Extract only the first token (before the first space)
                    valueLine = valueLine.split(" ").firstOrNull()
                    valueLine?.let { TextEntity(label = simplifiedTag, text = it) }
                        ?.let { entities.add(it) }
                }
            }
        }

        return entities
    }

    private fun formatTextWithBoundingBoxes(result: com.google.mlkit.vision.text.Text): String {
        val tolerance = 15  // Tolerance for grouping lines into rows
        val rows = mutableMapOf<Int, MutableList<String>>()

        for (block in result.textBlocks) {
            for (line in block.lines) {
                val topPosition = line.boundingBox?.top ?: 0

                // Check if the line fits within an existing row's tolerance range
                val existingRowKey = rows.keys.find { abs(it - topPosition) <= tolerance }

                if (existingRowKey != null) {
                    // Add text to the existing row
                    rows[existingRowKey]?.add(line.text)
                } else {
                    // Create a new row if no suitable row is found
                    rows[topPosition] = mutableListOf(line.text)
                }
            }
        }

        // Sort rows by top position and join the texts to form complete rows
        return rows.entries
            .sortedBy { it.key }
            .joinToString("\n") { (_, texts) -> texts.joinToString(" ") }
    }


    private fun combineEntitiesByTag(entities: List<TextEntity>): List<TextEntity> {
        val combinedEntities = entities
            .groupBy { simplifyTag(it.label) }
            .map { (label, groupedEntities) ->
                TextEntity(
                    label = label,
                    text = groupedEntities.joinToString(" ") { it.text }
                )
            }

        return combinedEntities
    }


    private fun applyRegexCorrections(entities: List<TextEntity>): List<TextEntity> {
        // Define regex patterns for various entities
        val urlRegex = Regex("https?://[\\w.-]+(?:\\.[\\w\\.-]+)+[/\\w\\._%+-]*") // Matches URLs
        val emailRegex =
            Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") // Matches email addresses
        val phoneRegex = Regex("""\b(?:\+?91|0)?[6789]\d{9}\b""") // Matches Indian phone numbers
        val dateRegex =
            Regex("\\b\\d{2}[\\/-]\\d{2}[\\/-]\\d{4}\\b") // Matches dates in DD/MM/YYYY or DD-MM-YYYY
        val amountRegex = Regex("\\b\\d+\\.\\d{2}\\b") // Matches amounts with two decimal places
        val gstTaxRegex = Regex("\\b[0-9A-Z]{15}\\b") // Matches GSTIN format
        val invoiceRegex = Regex("\\b\\d{1,8}\\b") // Matches invoices with 4+ digits
        val customerRegex = Regex("\\b[A-Z][a-z]+\\b") // Matches capitalized words
        val quantityRegex = Regex("\\b\\d{1,2}\\b") // Matches quantities of 1-2 digits

        return entities.mapNotNull { entity ->
            val matchedValues = when (simplifyTag(entity.label)) {
                "URL" -> urlRegex.findAll(entity.text).joinToString(", ") { it.value }
                "EMAIL" -> emailRegex.findAll(entity.text).joinToString(", ") { it.value }
                "PHONE" -> phoneRegex.findAll(entity.text).joinToString(", ") { it.value }
                "DATE" -> dateRegex.findAll(entity.text).joinToString(", ") { it.value }
                "TOTAL", "SUBTOTAL", "PRICE" -> amountRegex.findAll(entity.text)
                    .joinToString(", ") { it.value }

                "TAX", "GST", "GSTIN" -> gstTaxRegex.findAll(entity.text)
                    .joinToString(", ") { it.value }

                "INVOICE" -> invoiceRegex.findAll(entity.text).joinToString(", ") { it.value }
                "CUSTOMER", "VENDOR" -> customerRegex.findAll(entity.text)
                    .joinToString(" ") { it.value }

                "QUANTITY" -> quantityRegex.findAll(entity.text).joinToString(", ") { it.value }
                else -> entity.text // Fallback: Use original text
            }

            // Return updated entity only if matches are found
            if (matchedValues.isNotBlank()) {
                entity.copy(text = matchedValues)
            } else {
                null
            }
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
            Entity.TYPE_DATE_TIME -> "DATE"
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
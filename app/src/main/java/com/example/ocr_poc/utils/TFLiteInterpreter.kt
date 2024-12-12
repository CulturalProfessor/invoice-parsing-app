package com.example.ocr_poc.utils

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteInterpreter(context: Context, modelPath: String) {
    // Load TensorFlow Lite model
    private val interpreter: Interpreter = try {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = assetFileDescriptor.createInputStream()
        val mappedByteBuffer = inputStream.channel.map(
            java.nio.channels.FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
        Interpreter(mappedByteBuffer).also {
            Log.d("TFLiteInterpreter", "Model loaded successfully.")
        }
    } catch (e: FileNotFoundException) {
        Log.e("TFLiteInterpreter", "Model file not found: $modelPath", e)
        throw RuntimeException("Model file not found: $modelPath")
    } catch (e: IOException) {
        Log.e("TFLiteInterpreter", "Error loading model: ${e.message}", e)
        throw RuntimeException("Error loading model: ${e.message}")
    }
    private val inputShape: IntArray
    private val outputShape: IntArray

    init {

        // Retrieve input/output shapes
        try {
            inputShape = interpreter.getInputTensor(0).shape()
            outputShape = interpreter.getOutputTensor(0).shape()
            Log.d("TFLiteInterpreter", "Input Shape: ${inputShape.contentToString()}")
            Log.d("TFLiteInterpreter", "Output Shape: ${outputShape.contentToString()}")
        } catch (e: Exception) {
            Log.e(
                "TFLiteInterpreter",
                "Error retrieving model input/output shapes: ${e.message}",
                e
            )
            throw RuntimeException("Error retrieving model input/output shapes: ${e.message}")
        }

        // Shape validation
        if (inputShape.size != 2 || outputShape.size != 3) {
            throw IllegalStateException("Unexpected model input/output shapes. Input: ${inputShape.contentToString()}, Output: ${outputShape.contentToString()}")
        }
    }

    fun predict(
        text: String,
        word2index: Map<String, Int>,
        index2tag: Map<Int, String>
    ): Array<Pair<String, String>> {
        Log.d("TFLiteInterpreter", "Starting prediction...")

        // Preprocess input text
        val (tokens, inputTensor) = preprocessText(text, word2index)

        if (inputTensor.size != inputShape[1]) {
            throw IllegalArgumentException("Input length ${inputTensor.size} does not match expected length ${inputShape[1]}")
        }

        val inputBuffer = ByteBuffer.allocateDirect(inputTensor.size * 4)
            .order(ByteOrder.nativeOrder())
        inputTensor.forEach { inputBuffer.putInt(it) }
        inputBuffer.rewind()

        val outputBufferSize = outputShape[1] * outputShape[2] * 4
        val outputBuffer = ByteBuffer.allocateDirect(outputBufferSize)
            .order(ByteOrder.nativeOrder())

        return try {
            interpreter.run(inputBuffer, outputBuffer)
            Log.d("TFLiteInterpreter", "Inference completed.")
            val rawPredictions = parseOutput(outputBuffer, tokens, index2tag)
            validatePredictions(rawPredictions)
        } catch (e: Exception) {
            Log.e("TFLiteInterpreter", "Inference error: ${e.message}", e)
            throw RuntimeException("Inference error: ${e.message}")
        }
    }

    private fun validatePredictions(predictions: Array<Pair<String, String>>): Array<Pair<String, String>> {
        return predictions.filter { (token, tag) ->
            when (tag) {
                "B-TOTAL", "I-TOTAL", "B-PRICE", "I-PRICE", "B-QUANTITY", "I-QUANTITY" -> token.isNumeric()
                "B-NAME", "I-NAME" -> token.isName() // Add a custom check for names if needed
                else -> true // Allow other tags without validation
            }
        }.toTypedArray()
    }

    private fun String.isNumeric(): Boolean {
        return this.toDoubleOrNull() != null
    }

    private fun String.isName(): Boolean {
        // Simple heuristic: Names are usually alphabetic and start with an uppercase letter
        return this.matches(Regex("^[A-Z][a-zA-Z]+$"))
    }

    private fun preprocessText(
        text: String,
        word2index: Map<String, Int>
    ): Pair<List<String>, IntArray> {
        val tokens = text.split("\\s+".toRegex())
        val tokenIndices = tokens.map { word2index[it] ?: word2index["--UNKNOWN_WORD--"]!! }
        val sequenceLength = inputShape[1]

        val paddedSequence = if (tokenIndices.size > sequenceLength) {
            tokenIndices.take(sequenceLength)
        } else {
            tokenIndices + List(sequenceLength - tokenIndices.size) { 0 }
        }

        return tokens to paddedSequence.toIntArray()
    }

    private fun parseOutput(
        outputBuffer: ByteBuffer,
        tokens: List<String>,
        index2tag: Map<Int, String>
    ): Array<Pair<String, String>> {
        val sequenceLength = outputShape[1]
        val numClasses = outputShape[2]

        val predictions = IntArray(sequenceLength)
        outputBuffer.rewind()

        for (i in 0 until sequenceLength) {
            val classProbabilities = FloatArray(numClasses) { outputBuffer.getFloat() }
            predictions[i] = classProbabilities.indexOfMax()
        }

        return tokens.zip(predictions.map { index2tag[it] ?: "O" }).toTypedArray()
    }

    private fun FloatArray.indexOfMax(): Int {
        return this.withIndex().maxByOrNull { it.value }?.index ?: -1
    }

    fun close() {
        interpreter.close()
        Log.d("TFLiteInterpreter", "Interpreter closed.")
    }
}

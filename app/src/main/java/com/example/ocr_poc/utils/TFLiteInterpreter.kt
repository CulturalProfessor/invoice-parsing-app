package com.example.ocr_poc.utils

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteInterpreter(context: Context, modelPath: String) {
    private val interpreter: Interpreter
    private val inputShape: IntArray
    private val outputShape: IntArray

    init {
        interpreter = loadModel(context, modelPath)

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

        if (inputShape.size != 2 || outputShape.size != 3) {
            throw IllegalStateException("Unexpected model input/output shapes. Input: ${inputShape.contentToString()}, Output: ${outputShape.contentToString()}")
        }
    }

    private fun loadModel(context: Context, modelPath: String): Interpreter {
        return try {
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
    }

    fun predict(
        text: String,
        word2index: Map<String, Int>,
        index2tag: Map<Int, String>
    ): Array<Pair<String, String>> {
        Log.d("TFLiteInterpreter", "Starting prediction...")

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
                "B-NAME", "I-NAME" -> token.isName()
                else -> true
            }
        }.toTypedArray()
    }

    private fun String.isNumeric(): Boolean {
        return this.toDoubleOrNull() != null
    }

    private fun String.isName(): Boolean {
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


//    Differences
//    Model Loading
//    Python:
//    Uses tf.lite.Interpreter and allocates tensors directly.
//    Android:
//    Maps the model file to a ByteBuffer using the file system and initializes the Interpreter.
//    Input Processing
//    Python:
//    Uses pad_sequences from Keras to handle padding/truncation.
//    Directly works with NumPy arrays and TensorFlow utilities.
//    Android:
//    Performs padding/truncation manually by creating and manipulating lists.
//    Converts data to a ByteBuffer format, which is required for TensorFlow Lite inference on Android.
//    Inference Execution
//    Python:
//    Inference is a single call using interpreter.invoke() after setting the input tensor.
//    Android:
//    Uses the Interpreter.run() method with ByteBuffer input and output.
//    Post-Processing
//    Python:
//    Uses NumPy to handle tensors and process class probabilities (np.argmax).
//    Android:
//    Iterates over a ByteBuffer and computes the class with the highest probability manually.
//    Validation Logic
//    Android Only:
//    Includes validation checks for predictions based on specific tags (e.g., numeric validation for "B-TOTAL").
//    Python implementation does not include this additional layer of validation.
//    Error Handling
//    Android:
//    Includes explicit exception handling (FileNotFoundException, IOException) and logs errors using Log.e.
//    Python:
//    Relies on TensorFlow Lite's internal exception handling and raises errors directly.
//    Key Difference
//    Android's validatePredictions Function:
//    This is a unique feature in the Android implementation. It filters predictions based on domain-specific rules (e.g., checking if tokens for "B-TOTAL" are numeric). This kind of validation is absent in the Python implementation.

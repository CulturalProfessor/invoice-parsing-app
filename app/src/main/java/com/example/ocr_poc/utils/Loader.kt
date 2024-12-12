package com.example.ocr_poc.utils

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.channels.FileChannel

class TFLiteModelLoader(context: Context, modelPath: String) {
    private val interpreter: Interpreter

    init {
        interpreter = loadModel(context, modelPath)
        Log.d("TFLiteModelLoader", "Model loaded successfully.")
        logInputOutputShapes(interpreter)
    }

    private fun loadModel(context: Context, modelPath: String): Interpreter {
        return try {
            val assetFileDescriptor = context.assets.openFd(modelPath)
            val inputStream = assetFileDescriptor.createInputStream()
            val mappedByteBuffer = inputStream.channel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
            Interpreter(mappedByteBuffer)
        } catch (e: FileNotFoundException) {
            Log.e("TFLiteModelLoader", "Model file not found: $modelPath", e)
            throw RuntimeException("Model file not found: $modelPath")
        } catch (e: IOException) {
            Log.e("TFLiteModelLoader", "Error loading model: ${e.message}", e)
            throw RuntimeException("Error loading model: ${e.message}")
        }
    }

    private fun logInputOutputShapes(interpreter: Interpreter) {
        try {
            val inputShape = interpreter.getInputTensor(0).shape()
            val outputShape = interpreter.getOutputTensor(0).shape()
            Log.d("TFLiteModelLoader", "Input Shape: ${inputShape.contentToString()}")
            Log.d("TFLiteModelLoader", "Output Shape: ${outputShape.contentToString()}")
        } catch (e: Exception) {
            Log.e("TFLiteModelLoader", "Error retrieving input/output shapes: ${e.message}", e)
            throw RuntimeException("Error retrieving model input/output shapes: ${e.message}")
        }
    }

    fun close() {
        interpreter.close()
        Log.d("TFLiteModelLoader", "Interpreter closed.")
    }
}

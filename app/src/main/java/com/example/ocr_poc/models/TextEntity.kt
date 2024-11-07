package com.example.ocr_poc.models

import android.graphics.Point
import android.graphics.Rect

data class TextEntity(
    val label: String,
    val text: String,
    val boundingBox: Rect? = null,
    val cornerPoints: Array<Point>? = null
) {}

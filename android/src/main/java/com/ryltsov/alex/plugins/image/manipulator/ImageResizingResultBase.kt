package com.ryltsov.alex.plugins.image.manipulator

import android.graphics.Bitmap

public data class ImageResizingResultBase(
    val scaledBitmap: Bitmap?,
    val originalWidth: Int,
    val originalHeight: Int,
    val resizedWidth: Int,
    val resizedHeight: Int,
    val resized: Boolean
)

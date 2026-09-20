package com.ryltsov.alex.plugins.image.manipulator

public data class ImageResizingResult(
    val originalWidth: Int,
    val originalHeight: Int,
    val resizedWidth: Int,
    val resizedHeight: Int,
    val imagePath: String,
    val webPath: String,
    val resized: Boolean
)

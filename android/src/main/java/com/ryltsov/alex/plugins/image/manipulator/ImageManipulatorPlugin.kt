package com.ryltsov.alex.plugins.image.manipulator

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "ImageManipulator")
public class ImageManipulatorPlugin : Plugin() {
    private lateinit var implementation: ImageManipulator

    override fun load() {
        implementation = ImageManipulator(context, bridge)
    }

    @PluginMethod
    public fun getDimensions(call: PluginCall) {
        val imagePath = call.getString("imagePath")

        if (imagePath.isNullOrEmpty()) {
            call.reject("The imagePath param is null or empty.")
            return
        }

        try {
            val dimensions = implementation.getDimensions(imagePath)
            val ret = JSObject()
            ret.put("width", dimensions.width)
            ret.put("height", dimensions.height)
            call.resolve(ret)
        } catch (ex: ImageManipulatorException) {
            call.reject(ex.toString())
        } catch (ex: Exception) {
            call.reject("An error occurred: ${ex.message}")
        }
    }

    @PluginMethod
    public fun resize(call: PluginCall) {
        val imagePath = call.getString("imagePath")
        if (imagePath.isNullOrEmpty()) {
            call.reject("The imagePath param is null or empty.")
            return
        }

        val folderName = call.getString("folderName", "ResizedImages") ?: "ResizedImages"
        val fileName = call.getString("fileName")
        val quality = call.getInt("quality", 85) ?: 85
        val maxWidth = call.getInt("maxWidth", 0) ?: 0
        val maxHeight = call.getInt("maxHeight", 0) ?: 0
        if (maxWidth <= 0 && maxHeight <= 0) {
            call.reject("Either maxWidth or maxHeight param must be provided and be greater then 0.")
            return
        }
        val fixRotation = call.getBoolean("fixRotation", false) ?: false

        try {
            val result = implementation.resize(imagePath, folderName, fileName, quality, maxWidth, maxHeight, fixRotation)
            val ret = JSObject()
            ret.put("originalWidth", result.originalWidth)
            ret.put("originalHeight", result.originalHeight)
            ret.put("resizedWidth", result.resizedWidth)
            ret.put("resizedHeight", result.resizedHeight)
            ret.put("imagePath", result.imagePath)
            ret.put("webPath", result.webPath)
            ret.put("resized", result.resized)
            call.resolve(ret)
        } catch (ex: ImageManipulatorException) {
            call.reject(ex.toString())
        } catch (ex: Exception) {
            call.reject("An error occurred: ${ex.message}")
        }
    }
}

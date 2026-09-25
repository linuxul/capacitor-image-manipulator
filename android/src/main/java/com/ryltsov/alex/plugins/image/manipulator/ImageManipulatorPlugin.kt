package com.ryltsov.alex.plugins.image.manipulator

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginException
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
            throw PluginException("The imagePath param is null or empty.")
        }

        val dimensions = rejectingFailures { implementation.getDimensions(imagePath) }
        val ret = JSObject()
        ret.put("width", dimensions.width)
        ret.put("height", dimensions.height)
        call.resolve(ret)
    }

    @PluginMethod
    public fun resize(call: PluginCall) {
        val imagePath = call.getString("imagePath")
        if (imagePath.isNullOrEmpty()) {
            throw PluginException("The imagePath param is null or empty.")
        }

        val folderName = call.getString("folderName", "ResizedImages") ?: "ResizedImages"
        val fileName = call.getString("fileName")
        val quality = call.getInt("quality", 85) ?: 85
        val maxWidth = call.getInt("maxWidth", 0) ?: 0
        val maxHeight = call.getInt("maxHeight", 0) ?: 0
        if (maxWidth <= 0 && maxHeight <= 0) {
            throw PluginException("Either maxWidth or maxHeight param must be provided and be greater then 0.")
        }
        val fixRotation = call.getBoolean("fixRotation", false) ?: false

        val result = rejectingFailures { implementation.resize(imagePath, folderName, fileName, quality, maxWidth, maxHeight, fixRotation) }
        val ret = JSObject()
        ret.put("originalWidth", result.originalWidth)
        ret.put("originalHeight", result.originalHeight)
        ret.put("resizedWidth", result.resizedWidth)
        ret.put("resizedHeight", result.resizedHeight)
        ret.put("imagePath", result.imagePath)
        ret.put("webPath", result.webPath)
        ret.put("resized", result.resized)
        call.resolve(ret)
    }

    /**
     * Runs [work], turning its failure into the rejection the methods have always answered: the text of an
     * [ImageManipulatorException], or "An error occurred: " and the message of any other exception.
     */
    private inline fun <T> rejectingFailures(work: () -> T): T = try {
        work()
    } catch (ex: ImageManipulatorException) {
        throw PluginException(ex.toString())
    } catch (ex: Exception) {
        throw PluginException("An error occurred: ${ex.message}")
    }
}

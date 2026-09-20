package com.ryltsov.alex.plugins.image.manipulator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import com.getcapacitor.Bridge
import com.getcapacitor.FileUtils
import com.getcapacitor.Logger
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

public class ImageManipulator internal constructor(private val context: Context, private val bridge: Bridge) {
    /**
     * Method to get image dimensions (height and width)
     *
     * @param imagePath image path
     * @return image dimensions (width and height)
     */
    @Throws(ImageManipulatorException::class)
    public fun getDimensions(imagePath: String?): ImageDimensions {
        var imageStream: InputStream? = null

        try {
            imageStream = FileHelper.getInputStream(context, imagePath)
            val options = BitmapFactory.Options()
            // NOTE: decode with inJustDecodeBounds=true to get dimensions
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(imageStream, null, options)

            return ImageDimensions(options.outWidth, options.outHeight)
        } catch (ex: OutOfMemoryError) {
            Logger.error("ImageManipulator OutOfMemoryError exception occurred", ex)
            throw ImageManipulatorException("Out of memory: ${ex.message}")
        } catch (ex: FileNotFoundException) {
            Logger.error("ImageManipulator FileNotFoundException exception occurred", ex)
            throw ImageManipulatorException("No such image found: ${ex.message}")
        } catch (ex: IOException) {
            Logger.error("ImageManipulator IOException exception occurred", ex)
            throw ImageManipulatorException("Error occurred while reading the image: ${ex.message}")
        } catch (ex: Exception) {
            Logger.error("ImageManipulator exception thrown", ex)
            throw ImageManipulatorException("Unexpected error occurred: ${ex.message}")
        } finally {
            closeQuietly(imageStream)
        }
    }

    /**
     * Method to to resize image
     *
     * @param imagePath path to the image to resize
     * @param folderName directory where to save the resized image
     * @param fileName file name (without extension) to save save the resized image
     * @param quality quality (0-100) for the saved resized image
     * @param maxWidth required image width
     * @param maxHeight required image width
     * @param fixRotation fix rotation based on exif info
     *
     * @return resized image info
     */
    @Throws(ImageManipulatorException::class)
    public fun resize(
        imagePath: String,
        folderName: String,
        fileName: String?,
        quality: Int,
        maxWidth: Int,
        maxHeight: Int,
        fixRotation: Boolean
    ): ImageResizingResult {
        val dimensions = getDimensions(imagePath)

        if ((maxWidth == 0 || maxWidth >= dimensions.width) && (maxHeight == 0 || maxHeight >= dimensions.height)) {
            val webPath = FileUtils.getPortablePath(context, bridge.localUrl, Uri.parse(imagePath))
            return ImageResizingResult(
                dimensions.width,
                dimensions.height,
                dimensions.width,
                dimensions.height,
                imagePath,
                webPath,
                false
            )
        }

        val imageResizingResultBase = decodeScaledBitmapFromUri(imagePath, dimensions.width, dimensions.height, maxWidth, maxHeight)

        var bitmap = imageResizingResultBase.scaledBitmap ?: throw ImageManipulatorException("Error reading the image")

        if (fixRotation) {
            // NOTE: Get the exif rotation in degrees, create a transformation matrix, and rotate the bitmap
            val rotation = getRotationDegrees(getRotation(imagePath))
            val matrix = Matrix()
            if (rotation != 0) {
                matrix.preRotate(rotation.toFloat())
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        val savedScaledFileUri = FileHelper.saveFile(context, bitmap, folderName, fileName, quality)
            ?: throw ImageManipulatorException("Failed to save the resized image")

        val webPath = FileUtils.getPortablePath(context, bridge.localUrl, savedScaledFileUri)
        return ImageResizingResult(
            imageResizingResultBase.originalWidth,
            imageResizingResultBase.originalHeight,
            imageResizingResultBase.resizedWidth,
            imageResizingResultBase.resizedHeight,
            savedScaledFileUri.toString(),
            webPath,
            imageResizingResultBase.resized
        )
    }

    /**
     * Method to load a Bitmap using the provided file uri path and scale it to the required height and width taking into account
     * the original image aspect ratio
     *
     * @param imagePath path to the image to resize
     * @param originalWidth required image width
     * @param originalHeight required image width
     * @param maxWidth required image width
     * @param maxHeight required image width
     *
     * @return resized image info
     */
    @Throws(ImageManipulatorException::class)
    private fun decodeScaledBitmapFromUri(
        imagePath: String,
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): ImageResizingResultBase {
        var imageStreamForUnscaledBitmap: InputStream? = null

        try {
            val resultingImageDimensions = getResultingImageDimensions(originalWidth, originalHeight, maxWidth, maxHeight)

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(
                originalWidth,
                originalHeight,
                resultingImageDimensions.width,
                resultingImageDimensions.height
            )
            imageStreamForUnscaledBitmap = FileHelper.getInputStream(context, imagePath)
            val unscaledBitmap = BitmapFactory.decodeStream(imageStreamForUnscaledBitmap, null, options)
            if (unscaledBitmap == null) {
                Logger.error("ImageManipulator image data could not be decoded")
                // Caught below and reported as an unexpected error, as the Java implementation did.
                throw ImageManipulatorException("Image data could not be decoded")
            }
            val scaledBitmap = Bitmap.createScaledBitmap(
                unscaledBitmap,
                resultingImageDimensions.width,
                resultingImageDimensions.height,
                true
            )
            return ImageResizingResultBase(
                scaledBitmap,
                originalWidth,
                originalHeight,
                resultingImageDimensions.width,
                resultingImageDimensions.height,
                true
            )
        } catch (ex: OutOfMemoryError) {
            Logger.error("ImageManipulator OutOfMemoryError exception occurred", ex)
            throw ImageManipulatorException("Out of memory: ${ex.message}")
        } catch (ex: FileNotFoundException) {
            Logger.error("ImageManipulator FileNotFoundException exception occurred", ex)
            throw ImageManipulatorException("No such image found: ${ex.message}")
        } catch (ex: IOException) {
            Logger.error("ImageManipulator IOException exception occurred", ex)
            throw ImageManipulatorException("Error occurred while reading the image: ${ex.message}")
        } catch (ex: Exception) {
            Logger.error("ImageManipulatorPlugin ImageManipulatorException", ex)
            throw ImageManipulatorException("Unexpected error occurred: ${ex.message}")
        } finally {
            closeQuietly(imageStreamForUnscaledBitmap)
        }
    }

    private fun closeQuietly(stream: InputStream?) {
        try {
            stream?.close()
        } catch (e: IOException) {
            Logger.error(TAG, "UNABLE_TO_PROCESS_IMAGE", e)
        }
    }

    /**
     * Method to calculate the resulting or final width and height while keeping
     * the same aspect ratio to prevent the image to be stretched or squished
     *
     * @param originalWidth original image width
     * @param originalHeight original image height
     * @param maxWidth required image width
     * @param maxHeight required image width
     * @return resulting width and height
     */
    private fun getResultingImageDimensions(originalWidth: Int, originalHeight: Int, maxWidth: Int, maxHeight: Int): ImageDimensions {
        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (finalWidth <= 0 && finalHeight <= 0) {
            // NOTE: if required width and height are not provided we return the original bitmap
            finalWidth = originalWidth
            finalHeight = originalHeight
        } else if (finalWidth > 0 && finalHeight <= 0) {
            // NOTE: when only required width was provided
            finalHeight = ((finalWidth.toFloat() / originalWidth.toFloat()) * originalHeight).toInt()
        } else if (finalWidth <= 0) {
            // NOTE: when only required height was provided
            finalWidth = ((finalHeight.toFloat() / originalHeight.toFloat()) * originalWidth).toInt()
        } else {
            // NOTE: when both required width and height are provided
            val originalAspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
            if (maxWidth / maxHeight.toFloat() > originalAspectRatio) {
                finalWidth = (maxHeight * originalAspectRatio).toInt()
            } else {
                finalHeight = (maxWidth / originalAspectRatio).toInt()
            }
        }

        return ImageDimensions(finalWidth, finalHeight)
    }

    /**
     * Method to calculate the inSampleSize value for the BitmapFactory.Options to load the image with based
     * on the required width and height
     *
     * @param originalWidth original image width
     * @param originalHeight original image height
     * @param maxWidth required image width
     * @param maxHeight required image width
     * @return inSampleSize value
     */
    private fun calculateInSampleSize(originalWidth: Int, originalHeight: Int, maxWidth: Int, maxHeight: Int): Int {
        val originalAspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        val requiredAspectRatio = maxWidth.toFloat() / maxHeight.toFloat()

        return if (originalAspectRatio > requiredAspectRatio) {
            originalWidth / maxWidth
        } else {
            originalHeight / maxHeight
        }
    }

    /**
     * Gets the image rotation from the image EXIF Data
     *
     * @param exifOrientation ExifInterface.ORIENTATION_* representation of the rotation
     * @return the rotation in degrees
     */
    private fun getRotationDegrees(exifOrientation: Int): Int = when (exifOrientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

    /**
     * Gets the image rotation from the image EXIF Data
     *
     * @param imageUri the URI of the image to get the rotation for
     * @return ExifInterface.ORIENTATION_* representation of the rotation
     */
    private fun getRotation(imageUri: String): Int = try {
        ExifInterface(imageUri).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private companion object {
        const val TAG = "ImageManipulator"
    }
}

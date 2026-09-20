package com.ryltsov.alex.plugins.image.manipulator

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

public object FileHelper {
    @Throws(IOException::class)
    public fun getInputStream(context: Context, uriString: String?): InputStream? {
        require(!uriString.isNullOrEmpty()) { "The URI string is null or empty." }

        val uri = Uri.parse(uriString)
        return if ("content".equals(uri.scheme, ignoreCase = true)) {
            context.contentResolver.openInputStream(uri)
        } else {
            // A URI without a path was a NullPointerException in the Java implementation as well.
            FileInputStream(File(uri.path!!))
        }
    }

    @Throws(ImageManipulatorException::class)
    public fun saveFile(context: Context, bitmap: Bitmap, folderName: String, fileName: String?, quality: Int): Uri? {
        val folder = if (folderName.contains("/")) {
            File(folderName.replace("file://", ""))
        } else {
            File(context.filesDir, folderName)
        }

        if (!folder.exists() && !folder.mkdir()) {
            throw ImageManipulatorException("Failed to create folder to save the resized file")
        }

        val name = when {
            fileName == null -> "${System.currentTimeMillis()}.jpg"
            !fileName.endsWith(".jpg") -> "$fileName.jpg"
            else -> fileName
        }
        val file = File(folder, name)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.flush()
            out.close()
        } catch (ex: Exception) {
            throw ImageManipulatorException("Failed to save resized file. $ex")
        }
        return Uri.fromFile(file)
    }
}

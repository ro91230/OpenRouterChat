package com.personal.chat.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

class AssetOverrideManager(private val context: Context) {

    private fun getCustomAssetsDir(): File {
        val customDir = File(context.getExternalFilesDir(null), "custom_assets")
        if (!customDir.exists()) {
            customDir.mkdirs()
        }
        return customDir
    }

    fun getCustomLogoBitmap(): ImageBitmap? {
        val logoFile = File(getCustomAssetsDir(), "logo.png")
        return if (logoFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun getCustomNotificationSoundPath(): String? {
        val soundFile = File(getCustomAssetsDir(), "notification.wav")
        return if (soundFile.exists()) {
            soundFile.absolutePath
        } else {
            null
        }
    }
}

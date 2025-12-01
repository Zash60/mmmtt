package com.nes.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.min

/**
 * Gerencia thumbnails de ROMs.
 */
class ThumbnailManager(val context: Context) {
    
    private val thumbnailDir = File(context.cacheDir, "thumbnails")
    private val defaultThumbnail: Bitmap
    
    init {
        if (!thumbnailDir.exists()) {
            thumbnailDir.mkdirs()
        }
        defaultThumbnail = createDefaultThumbnail()
    }
    
    fun getThumbnail(romFile: File, width: Int = 256, height: Int = 224): Bitmap {
        val md5 = calculateMD5(romFile)
        val thumbnailFile = File(thumbnailDir, "$md5.png")
        
        if (thumbnailFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(thumbnailFile.absolutePath)
            if (bitmap != null) {
                return Bitmap.createScaledBitmap(bitmap, width, height, true)
            }
        }
        
        // Tentar buscar thumbnail online
        val thumbnail = fetchThumbnailOnline(md5, romFile.nameWithoutExtension)
        if (thumbnail != null) {
            saveThumbnail(thumbnailFile, thumbnail)
            return Bitmap.createScaledBitmap(thumbnail, width, height, true)
        }
        
        // Retornar thumbnail padrão
        return Bitmap.createScaledBitmap(defaultThumbnail, width, height, true)
    }
    
    fun saveThumbnail(file: File, bitmap: Bitmap) {
        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun createDefaultThumbnail(): Bitmap {
        val bitmap = Bitmap.createBitmap(256, 224, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        
        // Fundo gradiente
        paint.color = Color.rgb(40, 40, 40)
        canvas.drawRect(0f, 0f, 256f, 224f, paint)
        
        // Desenhar logo NES simplificado
        paint.color = Color.rgb(200, 0, 0)
        canvas.drawRect(50f, 80f, 206f, 144f, paint)
        
        paint.color = Color.WHITE
        paint.textSize = 32f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("NES", 100f, 120f, paint)
        
        return bitmap
    }
    
    private fun fetchThumbnailOnline(md5: String, romName: String): Bitmap? {
        return try {
            // Usar API do IGDB ou similar para buscar thumbnail
            // Por enquanto, retornar null para usar thumbnail padrão
            null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun calculateMD5(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
    
    fun clearCache() {
        try {
            thumbnailDir.deleteRecursively()
            thumbnailDir.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

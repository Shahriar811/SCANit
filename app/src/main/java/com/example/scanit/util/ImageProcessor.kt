package com.example.scanit.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class ScanFilter {
    ORIGINAL,
    AUTO_ENHANCE,
    BLACK_AND_WHITE,
    GRAYSCALE
}

data class ImageAdjustment(
    val brightness: Float = 0f, // Range: -100 to 100
    val contrast: Float = 0f,    // Range: -100 to 100
    val rotation: Int = 0,       // Rotation angle in degrees (0, 90, 180, 270)
    val filter: ScanFilter = ScanFilter.ORIGINAL
)

object ImageProcessor {
    /**
     * Apply a specific preset filter to a bitmap
     */
    fun applyFilter(bitmap: Bitmap, filter: ScanFilter): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        
        when (filter) {
            ScanFilter.ORIGINAL -> return bitmap
            ScanFilter.AUTO_ENHANCE -> {
                // Enhance document clarity: boost contrast (+25%) and brightness (+10)
                val contrastFactor = 1.25f
                val brightnessOffset = 15f
                colorMatrix.set(floatArrayOf(
                    contrastFactor, 0f, 0f, 0f, (1f - contrastFactor) * 128f + brightnessOffset,
                    0f, contrastFactor, 0f, 0f, (1f - contrastFactor) * 128f + brightnessOffset,
                    0f, 0f, contrastFactor, 0f, (1f - contrastFactor) * 128f + brightnessOffset,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            ScanFilter.GRAYSCALE -> {
                colorMatrix.setSaturation(0f)
            }
            ScanFilter.BLACK_AND_WHITE -> {
                // High contrast monochrome thresholding filter for sharp text
                val grayMatrix = ColorMatrix().apply { setSaturation(0f) }
                val thresholdMatrix = ColorMatrix(floatArrayOf(
                    8f, 8f, 8f, 0f, -2048f,
                    8f, 8f, 8f, 0f, -2048f,
                    8f, 8f, 8f, 0f, -2048f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(grayMatrix)
                colorMatrix.postConcat(thresholdMatrix)
            }
        }
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    /**
     * Apply brightness, contrast, filter, and rotation adjustments to a bitmap
     */
    fun applyAdjustments(
        bitmap: Bitmap,
        brightness: Float,
        contrast: Float,
        rotation: Int = 0,
        filter: ScanFilter = ScanFilter.ORIGINAL
    ): Bitmap {
        var adjustedBitmap = bitmap
        
        // Apply rotation first
        if (rotation != 0) {
            adjustedBitmap = rotateBitmap(adjustedBitmap, rotation)
        }
        
        // Apply filter preset
        if (filter != ScanFilter.ORIGINAL) {
            adjustedBitmap = applyFilter(adjustedBitmap, filter)
        }
        
        val outputBitmap = Bitmap.createBitmap(adjustedBitmap.width, adjustedBitmap.height, adjustedBitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint()
        
        // Create color matrix for brightness and contrast
        val colorMatrix = ColorMatrix()
        
        // Brightness adjustment: -100 to 100, convert to -1.0 to 1.0
        val brightnessValue = brightness / 100f
        
        // Contrast adjustment: -100 to 100, convert to contrast factor
        val contrastValue = contrast / 100f
        val contrastFactor = if (contrastValue >= 0) {
            1f + contrastValue * 2f // 1.0 to 3.0
        } else {
            1f + contrastValue // 0.0 to 1.0
        }
        
        // Apply brightness
        val brightnessMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, brightnessValue * 255f,
            0f, 1f, 0f, 0f, brightnessValue * 255f,
            0f, 0f, 1f, 0f, brightnessValue * 255f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        // Apply contrast
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrastFactor, 0f, 0f, 0f, (1f - contrastFactor) * 128f,
            0f, contrastFactor, 0f, 0f, (1f - contrastFactor) * 128f,
            0f, 0f, contrastFactor, 0f, (1f - contrastFactor) * 128f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        // Combine matrices
        colorMatrix.postConcat(brightnessMatrix)
        colorMatrix.postConcat(contrastMatrix)
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(adjustedBitmap, 0f, 0f, paint)
        
        return outputBitmap
    }
    
    /**
     * Load bitmap from URI without auto-rotation (to prevent auto-rotation glitch)
     */
    fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Load bitmap and get EXIF orientation (for reference, but don't auto-apply)
     */
    fun getExifOrientation(context: Context, uri: Uri): Int {
        return try {
            val filePath = uri.path
            if (filePath != null && File(filePath).exists()) {
                FileInputStream(filePath).use { fis ->
                    val exif = ExifInterface(fis)
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }
            } else {
                // Try to read from input stream
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            }
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }
    
    /**
     * Get rotation angle from EXIF orientation
     */
    private fun getRotationFromExif(orientation: Int): Int {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> -1 // Special case
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> -2 // Special case
            else -> 0
        }
    }
    
    /**
     * Rotate bitmap by specified degrees
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        
        val matrix = Matrix()
        when (degrees) {
            -1 -> matrix.postScale(-1f, 1f) // Flip horizontal
            -2 -> matrix.postScale(1f, -1f) // Flip vertical
            else -> matrix.postRotate(degrees.toFloat())
        }
        
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
    
    /**
     * Apply rotation to bitmap
     */
    fun applyRotation(bitmap: Bitmap, rotation: Int): Bitmap {
        return rotateBitmap(bitmap, rotation)
    }
    
    /**
     * Crop bitmap to specified rectangle
     */
    fun cropBitmap(bitmap: Bitmap, left: Int, top: Int, right: Int, bottom: Int): Bitmap {
        val width = (right - left).coerceIn(1, bitmap.width)
        val height = (bottom - top).coerceIn(1, bitmap.height)
        val x = left.coerceIn(0, bitmap.width - 1)
        val y = top.coerceIn(0, bitmap.height - 1)
        
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }
    
    /**
     * Save adjusted bitmap to a temporary file
     */
    fun saveAdjustedBitmap(context: Context, bitmap: Bitmap, originalUri: Uri): Uri {
        val outputFile = File(context.cacheDir, "adjusted_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(outputFile)
    }
    
    /**
     * Get bitmap from URI with adjustments applied
     */
    fun getAdjustedBitmap(context: Context, uri: Uri, adjustment: ImageAdjustment): Bitmap? {
        val originalBitmap = loadBitmap(context, uri) ?: return null
        return applyAdjustments(
            originalBitmap,
            adjustment.brightness,
            adjustment.contrast,
            adjustment.rotation,
            adjustment.filter
        )
    }
}


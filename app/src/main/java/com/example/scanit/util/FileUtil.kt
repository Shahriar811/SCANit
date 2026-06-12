package com.example.scanit.util

import android.content.ContentValues
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream

class FileUtil {
    companion object {
        private fun getOutputDirectory(context: Context): File {
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, "ScanIt").apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
        }

        fun saveImageFromUri(context: Context, uri: Uri): File {
            val outputFile = File(getOutputDirectory(context), "IMG_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            return outputFile
        }

        fun getScannedFiles(context: Context): List<File> {
            return getOutputDirectory(context).listFiles { file ->
                file.isFile && (file.extension == "pdf" || file.extension == "docx" || file.extension == "jpg")
            }?.toList() ?: emptyList()
        }
    }
}

fun saveTextAsPdf(context: Context, text: String, fileName: String, pageSize: String = "A4") {
    val width = if (pageSize == "Letter") 612 else 595
    val height = if (pageSize == "Letter") 792 else 842
    val document = android.graphics.pdf.PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    val paint = android.graphics.Paint()
    paint.textSize = 12f
    var y = 30f
    text.split("\n").forEach {
        canvas.drawText(it, 25f, y, paint)
        y += 18f
    }
    document.finishPage(page)

    val pdfFileName = "$fileName.pdf"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, pdfFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                document.writeTo(outputStream)
            }
        }
    } else {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), pdfFileName)
        document.writeTo(FileOutputStream(file))
    }
    document.close()
}

fun saveTextAsWord(context: Context, text: String, fileName: String) {
    val document = XWPFDocument()
    val paragraph = document.createParagraph()
    val run = paragraph.createRun()
    run.setText(text)

    val docxFileName = "$fileName.docx"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, docxFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                document.write(outputStream)
            }
        }
    } else {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), docxFileName)
        document.write(FileOutputStream(file))
    }
    document.close()
}

fun saveImagesAsPdf(context: Context, uris: List<Uri>, fileName: String, pageSize: String = "A4") {
    val pdfFileName = "$fileName.pdf"
    val pdf = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, pdfFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        val outputStream = resolver.openOutputStream(uri!!)
        val writer = PdfWriter(outputStream)
        com.itextpdf.kernel.pdf.PdfDocument(writer)
    } else {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), pdfFileName)
        val writer = PdfWriter(file)
        com.itextpdf.kernel.pdf.PdfDocument(writer)
    }
    
    val document = Document(pdf, if (pageSize == "Letter") com.itextpdf.kernel.geom.PageSize.LETTER else com.itextpdf.kernel.geom.PageSize.A4)

    uris.forEach { uri ->
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bytes = inputStream.readBytes()
            val image = ImageDataFactory.create(bytes)
            document.add(com.itextpdf.layout.element.Image(image))
        }
    }

    document.close()
    pdf.close()
}

fun saveImagesAsWord(context: Context, uris: List<Uri>, fileName: String) {
    val document = XWPFDocument()
    val docxFileName = "$fileName.docx"

    uris.forEach { uri ->
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val paragraph = document.createParagraph()
            val run = paragraph.createRun()
            run.addPicture(
                inputStream,
                XWPFDocument.PICTURE_TYPE_JPEG,
                uri.lastPathSegment ?: "image.jpg",
                Units.toEMU(400.0),
                Units.toEMU(600.0)
            )
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, docxFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                document.write(outputStream)
            }
        }
    } else {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), docxFileName)
        document.write(FileOutputStream(file))
    }
    document.close()
}

fun saveTextAsTxt(context: Context, text: String, fileName: String) {
    val txtFileName = "$fileName.txt"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, txtFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        uri?.let {
            resolver.openOutputStream(it).use { outputStream ->
                outputStream?.write(text.toByteArray())
            }
        }
    } else {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), txtFileName)
        FileOutputStream(file).use { out ->
            out.write(text.toByteArray())
        }
    }
}
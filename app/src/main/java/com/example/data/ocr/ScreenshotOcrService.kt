package com.example.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.security.ThreatIntelligenceService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class OcrExtractionResult(
    val extractedText: String,
    val detectedUrls: List<String>,
    val detectedPhoneNumbers: List<String>
)

object ScreenshotOcrService {

    private const val MAX_FILE_BYTES = 15 * 1024 * 1024L // 15 MB safety guard
    private const val MAX_DIMENSION_PX = 1920

    suspend fun extractTextFromUri(context: Context, uri: Uri): OcrExtractionResult =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri)
            if (mimeType != null && !mimeType.startsWith("image/")) {
                throw IllegalArgumentException("Unsupported file format ($mimeType). Please select an image screenshot.")
            }

            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                if (afd.length > MAX_FILE_BYTES) {
                    throw IllegalArgumentException("Image exceeds the 15 MB limit. Please choose a smaller screenshot.")
                }
            }

            val bitmap = decodeSampledBitmap(context, uri)
                ?: throw IllegalArgumentException("Could not decode the selected image. It may be corrupted or unsupported.")

            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = try {
                recognizer.process(inputImage).await()
            } finally {
                recognizer.close()
            }

            val text = result.text.trim()
            if (text.isBlank()) {
                throw IllegalStateException("No readable text was detected in this screenshot. Try cropping closer to the message.")
            }

            val urls = ThreatIntelligenceService.extractUrlsFromText(text)
            val phones = extractPhoneNumbers(text)

            OcrExtractionResult(
                extractedText = text,
                detectedUrls = urls,
                detectedPhoneNumbers = phones
            )
        }

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_DIMENSION_PX || bounds.outHeight / sampleSize > MAX_DIMENSION_PX) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }

    private fun extractPhoneNumbers(text: String): List<String> {
        val phoneRegex = Regex("""(?:\+?\d[\d\s\-().]{6,16}\d)""")
        return phoneRegex.findAll(text)
            .map { it.value.trim() }
            .filter { it.count(Char::isDigit) in 7..15 }
            .distinct()
            .toList()
    }
}

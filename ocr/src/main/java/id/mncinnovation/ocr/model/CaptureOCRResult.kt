package id.mncinnovation.ocr.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcelable
import android.util.Log
import androidx.core.net.toUri
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.ocr.utils.TAG_OCR
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
data class CaptureOCRResult(
    val isSuccess: Boolean,
    val errorMessage: String?,
    val imagePath: String?,
    val ocrValue: OCRValue
) : Parcelable {
    fun getBitmapImage(): Bitmap? {
        if (imagePath == null)
            return null
        val imgFile = File(imagePath)
        return BitmapFactory.decodeFile(imgFile.absolutePath)
    }
}
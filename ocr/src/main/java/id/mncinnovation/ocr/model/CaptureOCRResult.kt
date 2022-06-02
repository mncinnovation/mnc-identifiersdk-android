package id.mncinnovation.ocr.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
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

    fun toJson(): String {
        val mutableMap = mutableMapOf<String, Any?>()
        mutableMap["isSuccess"] = isSuccess
        mutableMap["errorMessage"] = errorMessage
        mutableMap["imagePath"] = imagePath
        mutableMap["ocrValue"] = JSONObject(ocrValue.toJson())

        val json = JSONObject(mutableMap)

        return json.toString()
    }
}
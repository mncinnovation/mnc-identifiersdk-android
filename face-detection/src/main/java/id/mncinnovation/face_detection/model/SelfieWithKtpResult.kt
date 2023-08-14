package id.mncinnovation.face_detection.model

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import id.mncinnovation.face_detection.SelfieWithKtpActivity
import id.mncinnovation.identification.core.utils.BitmapUtils
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelfieWithKtpResult(
    val isSuccess: Boolean,
    val errorMessage: String?,
    val imageUri: Uri,
    val detailFacesUri: List<Uri>
): Parcelable {
    fun getBitmap(context: Context, onError: (String) -> Unit): Bitmap?{
        return BitmapUtils.getBitmapFromContentUri(context.contentResolver, imageUri, onError = onError)
    }

    fun getListFaceBitmap(context: Context, onError: (String) -> Unit): List<Bitmap>{
        val result = mutableListOf<Bitmap>()
        detailFacesUri.forEach {
            BitmapUtils.getBitmapFromContentUri(context.contentResolver, it, onError = onError)?.let { bitmap ->
                result.add(bitmap)
            }
        }
        return result
    }
}

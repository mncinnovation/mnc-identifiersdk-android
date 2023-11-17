package id.mncinnovation.ocr

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CaptureOCRViewModel : ViewModel() {
    private var _isCompleted = MutableLiveData<Boolean?>()
    val isCompleted: LiveData<Boolean?> = _isCompleted

    private var bitmapList = mutableListOf<Bitmap?>()
    var isCapturing = false
    init {
        _isCompleted.value = null
    }
    fun captureImage(bitmap: Bitmap?) {
        if(bitmapList.size < MAX_CAPTURE && isCapturing && !bitmapList.contains(bitmap)) {
            bitmapList.add(bitmap)
        }
        viewModelScope.launch {
            delay(500)
            _isCompleted.value = bitmapList.size >= MAX_CAPTURE
            isCapturing = bitmapList.size < MAX_CAPTURE
        }
    }
    fun clearDataCapture() {
        isCapturing = false
        bitmapList.forEach { it?.recycle() }
        bitmapList.clear()
        _isCompleted.value = null
    }

    fun processExtract(extractDataOCR: ExtractDataOCR) {
        extractDataOCR.processExtractDataBitmap(bitmapList)
    }
    companion object {
        const val MAX_CAPTURE = 6
        const val COUNTDOWN_TIME = 3
    }
}
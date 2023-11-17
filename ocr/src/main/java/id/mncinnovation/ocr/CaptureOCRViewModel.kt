package id.mncinnovation.ocr

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CaptureOCRViewModel : ViewModel() {
    private var _currentState = MutableLiveData<StateCapture>()
    val currentState: LiveData<StateCapture> = _currentState

    private var bitmapList = mutableListOf<Bitmap?>()
    init {
        _currentState.value = StateCapture.READY
    }
    fun captureImage(bitmap: Bitmap?) {
        if(bitmapList.size < MAX_CAPTURE && !bitmapList.contains(bitmap)) {
            bitmapList.add(bitmap)
        }
        viewModelScope.launch {
            delay(500)
            _currentState.value = if(bitmapList.size == MAX_CAPTURE) StateCapture.COMPLETED else StateCapture.SCANNING
        }
    }
    fun clearDataCapture() {
        bitmapList.forEach { it?.recycle() }
        bitmapList.clear()
        _currentState.value = StateCapture.READY
    }

    fun processExtract(extractDataOCR: ExtractDataOCR) {
        extractDataOCR.processExtractDataBitmap(bitmapList)
    }
    companion object {
        const val MAX_CAPTURE = 6
        const val COUNTDOWN_TIME = 3
    }
}

enum class StateCapture {
    READY,
    SCANNING,
    COMPLETED
}
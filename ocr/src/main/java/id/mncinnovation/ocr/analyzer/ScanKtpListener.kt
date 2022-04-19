package id.mncinnovation.ocr.analyzer

import android.graphics.Bitmap
import id.mncinnovation.ocr.model.Ktp

interface ScanKtpListener {
    fun onStatusChanged(status: Status)
    fun onProgress(progress: Int)
    fun onScanComplete(ktp: Ktp)
    fun onScanFailed(exception: Exception)
}

interface CaptureKtpListener {
    fun onStatusChanged(status: Status)
    fun onCaptureComplete(bitmap: Bitmap)
    fun onCaptureFailed(exception: Exception)
}

enum class Status {
    NOT_READY, NOT_FOUND, SCANNING, COMPLETE
}
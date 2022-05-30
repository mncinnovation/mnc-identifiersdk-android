package id.mncinnovation.ocr.analyzer

import id.mncinnovation.ocr.model.OCRValue

interface ScanKtpListener {
    fun onStatusChanged(status: Status)
    fun onProgress(progress: Int)
    fun onScanComplete(ocrValue: OCRValue)
    fun onScanFailed(exception: Exception)
}

interface CaptureKtpListener {
    fun onStatusChanged(status: Status)
    fun onCaptureFailed(exception: Exception)
}

enum class Status {
    NOT_READY, NOT_FOUND, SCANNING, COMPLETE
}
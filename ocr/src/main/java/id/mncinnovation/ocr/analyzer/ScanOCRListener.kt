package id.mncinnovation.ocr.analyzer

import id.mncinnovation.ocr.model.KTPModel

interface ScanKtpListener {
    fun onStatusChanged(status: Status)
    fun onProgress(progress: Int)
    fun onScanComplete(ktpModel: KTPModel)
    fun onScanFailed(exception: Exception)
}

interface CaptureKtpListener {
    fun onStatusChanged(status: Status)
    fun onCaptureFailed(exception: Exception)
}

enum class Status {
    NOT_READY, NOT_FOUND, SCANNING, COMPLETE
}
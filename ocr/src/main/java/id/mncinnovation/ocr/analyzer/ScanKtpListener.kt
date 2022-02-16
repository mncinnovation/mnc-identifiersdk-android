package id.mncinnovation.ocr.analyzer

import id.mncinnovation.ocr.model.Ktp

interface ScanKtpListener {
    fun onStatusChanged(status: Status)
    fun onProgress(progress: Int)
    fun onScanComplete(ktp: Ktp)
    fun onScanFailed(exception: Exception)
}

enum class Status {
    NOT_READY, NOT_FOUND, SCANNING, COMPLETE
}
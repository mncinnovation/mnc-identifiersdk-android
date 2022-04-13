package id.mncinnovation.ocr

import android.content.Intent
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.ocr.model.CaptureKtpResult

object MNCIdentifierOCR {
    fun getCaptureKtpResult(intent: Intent?): CaptureKtpResult? {
        return intent?.getParcelableExtra(EXTRA_RESULT) as CaptureKtpResult?
    }
}
package id.mncinnovation.ocr

import android.content.Intent
import androidx.annotation.Keep
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.ocr.model.CaptureKtpResult

@Keep
object MNCIdentifierOCR {
    @JvmStatic
    fun getCaptureKtpResult(intent: Intent?): CaptureKtpResult? {
        return intent?.getParcelableExtra(EXTRA_RESULT) as CaptureKtpResult?
    }
}
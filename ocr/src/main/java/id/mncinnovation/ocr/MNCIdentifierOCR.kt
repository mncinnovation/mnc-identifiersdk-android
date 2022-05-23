package id.mncinnovation.ocr

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.common.EXTRA_WITH_FLASH
import id.mncinnovation.ocr.model.CaptureKtpResult

@Keep
object MNCIdentifierOCR {
    var CAPTURE_EKTP_REQUEST_CODE = 102

    @JvmStatic
    fun getCaptureKtpResult(intent: Intent?): CaptureKtpResult? {
        return intent?.getParcelableExtra(EXTRA_RESULT) as CaptureKtpResult?
    }

    @JvmStatic
    fun startCapture(
        activity: Activity,
        withFlash: Boolean? = false,
        requestCode: Int? = CAPTURE_EKTP_REQUEST_CODE
    ) {
        activity.startActivityForResult(
            getIntent(activity, withFlash),
            requestCode ?: CAPTURE_EKTP_REQUEST_CODE
        )
    }

    private fun getIntent(context: Context, withFlash: Boolean?): Intent {
        return Intent(context, CaptureKtpActivity::class.java)
            .putExtra(
                EXTRA_WITH_FLASH,
                withFlash
            )
    }
}
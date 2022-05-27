package id.mncinnovation.ocr

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.Keep
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.common.EXTRA_WITH_FLASH
import id.mncinnovation.ocr.model.CaptureOCRResult

@Keep
object MNCIdentifierOCR {
    private var CAPTURE_EKTP_REQUEST_CODE = 102

    /**
     * Start capture
     * @param intent an Intent data from activity result
     * @return an CaptureOCRResult
     */
    @JvmStatic
    fun getCaptureOCRResult(intent: Intent?): CaptureOCRResult? {
        return intent?.getParcelableExtra(EXTRA_RESULT) as CaptureOCRResult?
    }

    /**
     * Start capture
     * @param activity an Activity
     */
    @JvmStatic
    fun startCapture(
        activity: Activity
    ) {
        start(activity)
    }

    /**
     * Start capture
     * @param activity an Activity
     * @param withFlash boolean value to show button flash or not (true to show or false to hide it). The default value is false
     */
    @JvmStatic
    fun startCapture(
        activity: Activity,
        withFlash: Boolean? = false
    ) {
        start(activity, withFlash)
    }

    /**
     * Start capture
     * @param activity an Activity
     * @param withFlash boolean value to show button flash or not (true to show or false to hide it). The default value is false
     * @param requestCode an unique request code for activity result
     */
    @JvmStatic
    fun startCapture(
        activity: Activity,
        withFlash: Boolean? = false,
        requestCode: Int? = CAPTURE_EKTP_REQUEST_CODE
    ) {
        start(activity, withFlash, requestCode)
    }

    /**
     * Start capture activity (you need to override onActivityResult)
     * @param activity an Activity
     * @param withFlash boolean value to show button flash or not (true to show or false to hide it). The default value is false
     * @param requestCode an unique request code for activity result
     */
    @JvmStatic
    fun start(activity: Activity, withFlash: Boolean? = null, requestCode: Int? = null) {
        activity.startActivityForResult(
            getIntent(activity, withFlash),
            requestCode ?: CAPTURE_EKTP_REQUEST_CODE
        )
    }

    /**
     * Start capture activity (you need to override onActivityResult)
     * @param context an Context
     * @param activityResultLauncher an ActivityResultLauncher<Intent>
     * @param withFlash boolean value to show button flash or not (true to show or false to hide it). The default value is false
     */
    @JvmStatic
    fun startCapture(
        context: Context,
        activityResultLauncher: ActivityResultLauncher<Intent>,
        withFlash: Boolean? = null
    ) {
        activityResultLauncher.launch(getIntent(context, withFlash))
    }

    private fun getIntent(context: Context, withFlash: Boolean?): Intent {
        return Intent(context, CaptureOCRActivity::class.java)
            .putExtra(
                EXTRA_WITH_FLASH,
                withFlash
            )
    }
}
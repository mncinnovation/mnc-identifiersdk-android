package id.mncinnovation.ocr

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.Keep
import id.mncinnovation.identification.core.common.CAPTURE_EKTP_REQUEST_CODE
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.common.EXTRA_WITH_FLASH
import id.mncinnovation.ocr.model.OCRResultModel


@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.EXPRESSION
)
@MustBeDocumented
annotation class KeepDocumented

@Keep
object MNCIdentifierOCR {

    /**
     * Start capture
     * @param intent an Intent data from activity result
     * @return an OCRResultModel
     */
    @JvmStatic
    @KeepDocumented
    fun getOCRResult(@KeepDocumented intent: Intent?): OCRResultModel? {
        return intent?.getParcelableExtra(EXTRA_RESULT) as OCRResultModel?
    }

    /**
     * Start capture
     * @param activity an Activity
     */
    @JvmStatic
    @KeepDocumented
    fun startCapture(
        @KeepDocumented activity: Activity
    ) {
        start(activity)
    }

    /**
     * Start capture
     * @param activity an Activity
     * @param withFlash boolean value to show button flash or not (true to show or false to hide it). The default value is false
     */
    @JvmStatic
    @KeepDocumented
    fun startCapture(
        @KeepDocumented activity: Activity,
        @KeepDocumented withFlash: Boolean? = false
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
    @KeepDocumented
    fun startCapture(
        @KeepDocumented activity: Activity,
        @KeepDocumented withFlash: Boolean? = false,
        @KeepDocumented requestCode: Int? = CAPTURE_EKTP_REQUEST_CODE
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
    @KeepDocumented
    fun start(
        @KeepDocumented activity: Activity,
        @KeepDocumented withFlash: Boolean? = null,
        @KeepDocumented requestCode: Int? = null
    ) {
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
    @KeepDocumented
    fun startCapture(
        @KeepDocumented context: Context,
        @KeepDocumented activityResultLauncher: ActivityResultLauncher<Intent>,
        @KeepDocumented withFlash: Boolean? = null
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
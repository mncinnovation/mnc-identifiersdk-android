package id.mncinnovation.ocr

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.Keep
import androidx.core.net.toUri
import id.mncinnovation.identification.core.common.CAPTURE_EKTP_REQUEST_CODE
import id.mncinnovation.identification.core.common.EXTRA_RESULT
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
    internal var withFlash: Boolean? = null
    internal var cameraOnly: Boolean? = null
    internal var lowMemoryThreshold: Int? = null

    /**
     * Start capture
     * @param withFlash boolean value to show button flash or not (true to show or false to hide it). The default value is false
     * @param cameraOnly boolean value to show an activity camera only (without splash and confirmation screen) to get result OCR. The default value is false
     * @param lowMemoryThreshold Int value to set low memory threshold for show warning usage minimum alocation memory. The default value is 50 MB
     */
    @JvmStatic
    @KeepDocumented
    fun config(
        @KeepDocumented withFlash: Boolean? = false,
        @KeepDocumented cameraOnly: Boolean? = false,
        @KeepDocumented lowMemoryThreshold: Int? = 50
    ) {
        MNCIdentifierOCR.withFlash = withFlash
        MNCIdentifierOCR.cameraOnly = cameraOnly
        MNCIdentifierOCR.lowMemoryThreshold = lowMemoryThreshold
    }

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
     * Start capture directly without customize withFlash and cameraOnly
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
     * @param requestCode an unique request code for activity result
     */
    @JvmStatic
    @KeepDocumented
    fun startCapture(
        @KeepDocumented activity: Activity,
        @KeepDocumented requestCode: Int? = CAPTURE_EKTP_REQUEST_CODE
    ) {
        start(activity, requestCode)
    }

    /**
     * Start capture activity (you need to override onActivityResult)
     * @param activity an Activity
     * @param requestCode an unique request code for activity result
     */
    @JvmStatic
    @KeepDocumented
    private fun start(
        @KeepDocumented activity: Activity,
        @KeepDocumented requestCode: Int? = null
    ) {
        activity.startActivityForResult(
            getIntent(activity),
            requestCode ?: CAPTURE_EKTP_REQUEST_CODE
        )
    }

    /**
     * Start capture activity (you need to override onActivityResult)
     * @param context an Context
     * @param activityResultLauncher an ActivityResultLauncher<Intent>
     */
    @JvmStatic
    @KeepDocumented
    fun startCapture(
        @KeepDocumented context: Context,
        @KeepDocumented activityResultLauncher: ActivityResultLauncher<Intent>
    ) {
        activityResultLauncher.launch(getIntent(context))
    }

    private fun getIntent(
        context: Context
    ): Intent {
        return Intent(context, CaptureOCRActivity::class.java)
    }

    /**
     * Function to extract Data OCR from one image path.
     * @param imagePath list of image path
     * @param context an Context
     * @param listener an listener to listen on start and on finish process extract data.
     */
    @JvmStatic
    fun extractData(
        imagePath: String, context: Context,
        listener: ExtractDataOCRListener
    ) {
        val uriList = mutableListOf<Uri>()
        uriList.add(imagePath.toUri())

        extractDataFromUri(uriList, context, listener)
    }

    /**
     * Function to extract Data OCR from list of image path.
     * Use this function to get best result by using many options of input images.
     * @param imagePaths list of image path
     * @param context an Context
     * @param listener an listener to listen on start and on finish process extract data.
     */
    @JvmStatic
    fun extractData(
        imagePaths: List<String>,
        context: Context,
        listener: ExtractDataOCRListener
    ) {
        val uriList = mutableListOf<Uri>()
        imagePaths.forEach {
            uriList.add(it.toUri())
        }
        extractDataFromUri(uriList, context, listener)
    }

    @JvmStatic
    fun extractDataFromUri(
        uri: Uri,
        context: Context,
        listener: ExtractDataOCRListener
    ) {
        val uriList = mutableListOf<Uri>()
        uriList.add(uri)
        val extractDataOCR = ExtractDataOCR(context, listener)
        extractDataOCR.processExtractData(uriList)
    }

    @JvmStatic
    fun extractDataFromUri(
        uriList: List<Uri>,
        context: Context,
        listener: ExtractDataOCRListener
    ) {
        val extractDataOCR = ExtractDataOCR(context, listener)
        extractDataOCR.processExtractData(uriList)
    }
}
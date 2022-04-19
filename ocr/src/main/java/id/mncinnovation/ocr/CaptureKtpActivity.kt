package id.mncinnovation.ocr

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import id.mncinnovation.identification.core.base.BaseCameraActivity
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.identification.core.utils.BitmapUtils.saveBitmapToFile
import id.mncinnovation.ocr.analyzer.CaptureKtpAnalyzer
import id.mncinnovation.ocr.analyzer.CaptureKtpListener
import id.mncinnovation.ocr.analyzer.Status
import id.mncinnovation.ocr.databinding.PopupBottomsheetScanTimerBinding
import id.mncinnovation.ocr.model.CaptureKtpResult
import id.mncinnovation.ocr.utils.extractEktp
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorMatrixFilter
import java.io.File
import java.util.*
import kotlin.concurrent.fixedRateTimer

@Suppress("DEPRECATION")
class CaptureKtpActivity : BaseCameraActivity(), CaptureKtpListener {
    private lateinit var uiContainer: View
    private lateinit var btnCapture: ImageButton

    private var hasLaunchSplash = false
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var timer: Timer? = null
    private var isCaptured = false

    private val localModel =
        LocalModel.Builder().setAssetFilePath("custom_models/object_labeler.tflite").build()
    private var option =
        CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()
            .setMaxPerObjectLabelCount(1)
            .build()
    private val objectDetector = ObjectDetection.getClient(option)
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    lateinit var gpuImage: GPUImage
    private var captureUseCase: ImageCapture? = null

    private val progressDialog: ProgressDialog by lazy {
        ProgressDialog(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasLaunchSplash)
            resultLauncherSplash.launch(Intent(this, SplashOCRActivity::class.java))

        gpuImage = GPUImage(this).apply {
            setFilter(
                GPUImageColorMatrixFilter(
                    1f,
                    floatArrayOf(
                        0f, 0f, 1.3f, 0f,
                        0f, 0f, 1.3f, 0f,
                        0f, 0f, 1.3f, 0f,
                        0f, 0f, 0f, 1f
                    )
                )
            )
        }
        uiContainer =
            LayoutInflater.from(this).inflate(R.layout.activity_capture_ktp, rootView, true)
        btnCapture = uiContainer.findViewById(R.id.camera_capture_button)
        btnCapture.setOnClickListener {
            captureImage()
        }
    }

    override fun startCamera(
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView,
    ) {
        // CameraSelector
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview
        val previewUseCase = Preview.Builder().build()

        // ImageCapture
        captureUseCase = ImageCapture.Builder().apply {
            setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        }.build()


        val analysisUseCase = ImageAnalysis.Builder().build().also {
            it.setAnalyzer(cameraExecutor, CaptureKtpAnalyzer(this))
        }
        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            cameraProvider.bindToLifecycle(
                this, cameraSelector, previewUseCase, analysisUseCase, captureUseCase
            )
            // Attach the viewfinder's surface provider to preview use case
            previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun captureImage() {
        val photoFile = File.createTempFile("selfiektp", ".jpg")
        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = false
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()

        // Setup image capture listener which is triggered after photo has been taken
        captureUseCase?.takePicture(
            outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                    extractDataKtp(savedUri)
                }
            })
    }

    private fun extractDataKtp(uri: Uri) {
        showProgressDialog()
        val imageBitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, uri)
            ?: return
        objectDetector.process(InputImage.fromBitmap(imageBitmap, 0))
            .addOnSuccessListener { objects ->
                val cropedBitmap = if (objects.isEmpty()) imageBitmap else
                    Bitmap.createBitmap(
                        imageBitmap,
                        objects.first().boundingBox.left,
                        objects.first().boundingBox.top,
                        objects.first().boundingBox.width(),
                        objects.first().boundingBox.height()
                    )
                val resultUri = saveBitmapToFile(cropedBitmap, filesDir.absolutePath, "ktpocr.jpg")
                val filteredBitmap = gpuImage.getBitmapWithFilterApplied(cropedBitmap)

                textRecognizer.process(InputImage.fromBitmap(filteredBitmap, 0))
                    .addOnSuccessListener { text ->
                        val ekp = text.extractEktp()
                        val ocrResult =
                            CaptureKtpResult(true, "Success", resultUri, ekp)
                        setResult(RESULT_OK, intent)
                        hideProgressDialog()
                        val intent = Intent(this, ConfirmationActivity::class.java).apply {
                            putExtra(EXTRA_RESULT, ocrResult)
                        }
                        resultLauncherConfirm.launch(intent)
                    }
            }
    }

    override fun onResume() {
        super.onResume()
        isCaptured = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    private fun showProgressDialog() {
        runOnUiThread {
            progressDialog.show()
        }
    }

    private fun hideProgressDialog() {
        progressDialog.dismiss()
    }

    private val resultLauncherSplash =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                hasLaunchSplash = true
            }
        }
    private val resultLauncherConfirm =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val captureKtpResult = MNCIdentifierOCR.getCaptureKtpResult(data)
                captureKtpResult?.let { scanResult ->
                    val intent = Intent().apply {
                        putExtra(EXTRA_RESULT, scanResult)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }

    //Listener of CaptureKtpListener
    override fun onStatusChanged(status: Status) {
        Log.e(TAG, "onStatusChanged ${status.name}")
        if (status == Status.SCANNING) {
            if (isCaptured) return
            showPopupHoldScanDialog()
        } else {
            stopTimer()
            bottomSheetDialog?.dismiss()
        }
    }

    override fun onCaptureFailed(exception: Exception) {
        exception.printStackTrace()
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun showPopupHoldScanDialog() {
        if (timer != null) return
        val bindingPopup = PopupBottomsheetScanTimerBinding.inflate(LayoutInflater.from(this))
        var counter = COUNTDOWN_TIME

        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogThemeOCR)
        bindingPopup.apply {
            timer = fixedRateTimer(initialDelay = 0, period = 1000) {
                runOnUiThread {
                    tvCountdown.text = "$counter"
                    counter--

                    if (counter == 0) {
                        bottomSheetDialog?.dismiss()
                        isCaptured = true
                        captureImage()
                    }
                }
            }
        }

        if (bottomSheetDialog?.isShowing == true) {
            bottomSheetDialog?.dismiss()
        }

        bottomSheetDialog?.setContentView(bindingPopup.root)
        bottomSheetDialog?.show()
    }

    companion object {
        const val TAG = "CaptureKtpActivity"
        const val COUNTDOWN_TIME = 3
    }
}

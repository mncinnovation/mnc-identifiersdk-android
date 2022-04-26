package id.mncinnovation.ocr

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import id.mncinnovation.identification.core.base.BaseCameraActivity
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.common.showCustomToast
import id.mncinnovation.identification.core.common.toVisibilityOrGone
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.identification.core.utils.BitmapUtils.saveBitmapToFile
import id.mncinnovation.ocr.analyzer.CaptureKtpAnalyzer
import id.mncinnovation.ocr.analyzer.CaptureKtpListener
import id.mncinnovation.ocr.analyzer.Status
import id.mncinnovation.ocr.databinding.PopupBottomsheetScanTimerBinding
import id.mncinnovation.ocr.model.CaptureKtpResult
import id.mncinnovation.ocr.utils.LightSensor
import id.mncinnovation.ocr.utils.LightSensorListener
import id.mncinnovation.ocr.utils.extractEktp
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorMatrixFilter
import java.io.File
import java.util.*
import kotlin.concurrent.fixedRateTimer

@Suppress("DEPRECATION")
class CaptureKtpActivity : BaseCameraActivity(), CaptureKtpListener {
    private lateinit var uiContainer: View
    private lateinit var btnFlash: ImageButton
    private lateinit var gifLoading: LottieAnimationView

    private var hasLaunchSplash = false
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var timer: Timer? = null
    private var isCaptured = false
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var camera: Camera? = null

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
    private lateinit var gpuImage: GPUImage
    private var captureUseCase: ImageCapture? = null

    private var lightSensor: LightSensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasLaunchSplash)
            resultLauncherSplash.launch(Intent(this, SplashOCRActivity::class.java))


        lightSensor = LightSensor(this, object : LightSensorListener {
            override fun onCurrentLightChanged(value: Int) {
                val isLowLight = value < 5
                if (isLowLight && flashMode == ImageCapture.FLASH_MODE_OFF) {
                    showCustomToast("Cahaya terlalu gelap, anda bisa menggunkan flash")
                }
                btnFlash.visibility = isLowLight.toVisibilityOrGone()
            }
        })

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
        btnFlash = uiContainer.findViewById(R.id.ib_flash_mode)
        gifLoading = uiContainer.findViewById(R.id.gif_loading)
        hideProgressDialog()

        btnFlash.setOnClickListener {
            flashMode =
                if (flashMode == ImageCapture.FLASH_MODE_OFF) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            val isTorchEnable = flashMode == ImageCapture.FLASH_MODE_ON
            btnFlash.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (isTorchEnable) R.drawable.ic_baseline_flash_off else R.drawable.ic_baseline_flash_on
                )
            )
            camera?.cameraControl?.enableTorch(isTorchEnable)
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
            camera = cameraProvider.bindToLifecycle(
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
        lightSensor?.closeSensor()
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
        lightSensor?.startDetectingSensor()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
        lightSensor?.closeSensor()
    }

    private fun showProgressDialog() {
        runOnUiThread {
            gifLoading.visibility = View.VISIBLE
        }
    }

    private fun hideProgressDialog() {
        gifLoading.visibility = View.GONE
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
                    if (counter == 0) {
                        bottomSheetDialog?.dismiss()
                        isCaptured = true
                        captureImage()
                    }
                    counter--
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

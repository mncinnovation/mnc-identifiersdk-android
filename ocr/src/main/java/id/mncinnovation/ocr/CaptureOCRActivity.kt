package id.mncinnovation.ocr

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import id.mncinnovation.identification.core.base.BaseCameraActivity
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.common.toVisibilityOrGone
import id.mncinnovation.identification.core.utils.MemoryUsageMonitor
import id.mncinnovation.ocr.analyzer.CaptureKtpListener
import id.mncinnovation.ocr.analyzer.CaptureOCRAnalyzer
import id.mncinnovation.ocr.analyzer.Status
import id.mncinnovation.ocr.databinding.PopupBottomsheetScanTimerOcrBinding
import id.mncinnovation.ocr.model.KTPModel
import id.mncinnovation.ocr.model.OCRResultModel
import id.mncinnovation.ocr.utils.LightSensor
import id.mncinnovation.ocr.utils.LightSensorListener
import java.io.File
import java.util.Timer
import kotlin.concurrent.fixedRateTimer


class CaptureOCRActivity : BaseCameraActivity(), CaptureKtpListener {
    private lateinit var uiContainer: View
    private lateinit var btnFlash: ImageButton
    private lateinit var tvMessage: TextView
    private lateinit var gifLoading: LottieAnimationView
    private var hasLaunchSplash = false
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var timer: Timer? = null

    //Property for capture logic
    private var handlers: Handler? = null
    private var runnableCapture: Runnable? = null

    private var isCaptured = false
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var camera: Camera? = null

    private var captureUseCase: ImageCapture? = null
    private var lightSensor: LightSensor? = null
    private var uriList = mutableListOf<Uri>()
    private var ktpList = mutableListOf<KTPModel>()

    private var memoryUsageMonitor: MemoryUsageMonitor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasLaunchSplash && MNCIdentifierOCR.cameraOnly == false)
            resultLauncherSplash.launch(Intent(this, SplashOCRActivity::class.java))

        lightSensor = LightSensor(this, object : LightSensorListener {
            override fun onCurrentLightChanged(value: Int) {
                val isLowLight = value < 5
                tvMessage.visibility = (isLowLight).toVisibilityOrGone()
            }
        })

        uiContainer =
            LayoutInflater.from(this).inflate(R.layout.activity_capture_ocr, rootView, true)
        btnFlash = uiContainer.findViewById(R.id.ib_flash_mode)
        tvMessage = uiContainer.findViewById(R.id.tv_message)
        gifLoading = uiContainer.findViewById(R.id.gif_loading)
        hideProgressDialog()

        btnFlash.visibility = (MNCIdentifierOCR.withFlash ?: false).toVisibilityOrGone()

        tvMessage.visibility = View.GONE
        setMessageIsTorchEnable(false)

        btnFlash.setOnClickListener {
            flashMode =
                if (flashMode == ImageCapture.FLASH_MODE_OFF) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            val isTorchEnable = flashMode == ImageCapture.FLASH_MODE_ON

            setMessageIsTorchEnable(isTorchEnable)

            btnFlash.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (isTorchEnable) R.drawable.ic_baseline_flash_off else R.drawable.ic_baseline_flash_on
                )
            )
            camera?.cameraControl?.enableTorch(isTorchEnable)
        }

        memoryUsageMonitor = MemoryUsageMonitor(this, getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager, lowMemoryThreshold = MNCIdentifierOCR.lowMemoryThreshold)
    }

    private fun setMessageIsTorchEnable(isActive: Boolean) {
        tvMessage.text = if (isActive) "Cahaya terlalu gelap, silahkan mencari tempat yang lebih terang" else "Cahaya terlalu gelap, bisa menggunakan flash"
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
            it.setAnalyzer(cameraExecutor, CaptureOCRAnalyzer(this))
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
        val photoFile = File.createTempFile("ktp", ".jpg")
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
                    try {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        Log.d(TAG, "Photo capture succeeded: $savedUri")
                        if (uriList.size < MAX_CAPTURE) {
                            uriList.add(savedUri)
                        }
                        if (uriList.size == MAX_CAPTURE) {
                            MNCIdentifierOCR.extractDataFromUri(
                                uriList,
                                this@CaptureOCRActivity,
                                object : ExtractDataOCRListener {
                                    override fun onStart() {
                                        if (isCaptured) {
                                            showProgressDialog()
                                        }
                                    }

                                    override fun onFinish(result: OCRResultModel) {
                                        hideProgressDialog()
                                        if (MNCIdentifierOCR.cameraOnly == true) {
                                            val intent = Intent().apply {
                                                putExtra(EXTRA_RESULT, result)
                                            }
                                            setResult(RESULT_OK, intent)
                                            finish()
                                        } else {
                                            val intent = Intent(
                                                this@CaptureOCRActivity,
                                                ConfirmationOCRActivity::class.java
                                            ).apply {
                                                putExtra(EXTRA_RESULT, result)
                                            }
                                            resultLauncherConfirm.launch(intent)
                                        }
                                    }

                                    override fun onError(message: String?) {
                                        Log.e(TAG, "Failed extract ocr: $message")

                                        val intent = Intent().apply {
                                            putExtra(EXTRA_RESULT, OCRResultModel(false, message, null, KTPModel()))
                                        }
                                        setResult(RESULT_OK, intent)
                                        finish()
                                    }
                                })
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            })
    }

    override fun onResume() {
        super.onResume()
        isCaptured = false
        lightSensor?.startDetectingSensor()
    }

    override fun onStop() {
        super.onStop()
        stopTimer()
        hideProgressDialog()
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
                val captureKtpResult = MNCIdentifierOCR.getOCRResult(data)
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
        if (isCaptured) return
        if (status == Status.SCANNING) {
            memoryUsageMonitor?.checkMemoryAndProceed {
                showPopupHoldScanDialog()
            }
        } else {
            clearDataCapture()
            stopTimer()
            hideProgressDialog()
            bottomSheetDialog?.dismiss()
        }
    }

    private fun clearDataCapture() {
        ktpList.clear()
        uriList.clear()
    }

    override fun onCaptureFailed(exception: Exception) {
        exception.printStackTrace()
        setResult(
            RESULT_CANCELED,
            Intent().apply {
                putExtra(EXTRA_RESULT, OCRResultModel(false, exception.message, null, KTPModel()))
            }
        )
        finish()
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
        runnableCapture?.let {
            handlers?.removeCallbacks(it)
        }
    }

    private fun showPopupHoldScanDialog() {
        if (timer != null) return
        val bindingPopup = PopupBottomsheetScanTimerOcrBinding.inflate(LayoutInflater.from(this))
        var counter = COUNTDOWN_TIME

        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogThemeOCR)
        bindingPopup.apply {
            timer = fixedRateTimer(initialDelay = 0, period = 1000) {
                runOnUiThread {
                    tvCountdown.text = "$counter"
                    if (counter == 0) {
                        bottomSheetDialog?.dismiss()
                        isCaptured = true
                        stopTimer()
                    }
                    counter--
                }
            }
        }
        handlers = Handler(Looper.getMainLooper())
        runnableCapture = object : Runnable {
            override fun run() {
                if (!isCaptured) {
                    captureImage()
                }
                handlers?.postDelayed(this, 500)
            }
        }
        runnableCapture?.let {
            handlers?.post(it)
        }

        if (bottomSheetDialog?.isShowing == true) {
            bottomSheetDialog?.dismiss()
        }

        bottomSheetDialog?.setContentView(bindingPopup.root)
        bottomSheetDialog?.show()
    }

    override fun onBackPressed() {
        setResult(
            RESULT_CANCELED,
            Intent().apply {
                putExtra(EXTRA_RESULT, OCRResultModel(false, "Cancelled by user", null, KTPModel()))
            }
        )
        super.onBackPressed()
    }

    companion object {
        const val TAG = "CaptureOCRActivity"
        const val COUNTDOWN_TIME = 3
        const val MAX_CAPTURE = 6
    }
}
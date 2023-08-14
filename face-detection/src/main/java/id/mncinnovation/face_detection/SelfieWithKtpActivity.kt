package id.mncinnovation.face_detection

import android.app.ActivityManager
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import id.mncinnovation.face_detection.analyzer.FaceDetectionAnalyzer
import id.mncinnovation.face_detection.analyzer.FaceDetectionListener
import id.mncinnovation.face_detection.model.SelfieWithKtpResult
import id.mncinnovation.identification.core.base.BaseCameraActivity
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.identification.core.utils.MemoryUsageMonitor
import java.io.File
import java.util.*
import kotlin.concurrent.fixedRateTimer

@Suppress("DEPRECATION")
class SelfieWithKtpActivity : BaseCameraActivity(), FaceDetectionListener {
    private var memoryUsageMonitor: MemoryUsageMonitor? = null
    private lateinit var uiContainer: View
    private lateinit var btnCapture: ImageButton
    private lateinit var tvFaceNotFound: TextView
    private lateinit var tvTimer: TextView
    private val faceDetector = FaceDetection.getClient()
    private val imageAnalyzer = FaceDetectionAnalyzer(listener = this)
    private var captureUseCase: ImageCapture? = null
    private var timer: Timer? = null
    private var countdownTime = COUNTDOWN_TIME
    private var isCaptured = false
    private val progressDialog: ProgressDialog by lazy {
        ProgressDialog(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uiContainer =
            LayoutInflater.from(this).inflate(R.layout.activity_selfie_with_ktp, rootView, true)
        btnCapture = uiContainer.findViewById(R.id.camera_capture_button)
        tvFaceNotFound = uiContainer.findViewById(R.id.tv_face_notfound)
        tvTimer = uiContainer.findViewById(R.id.tv_timer)
        btnCapture.setOnClickListener {
            memoryUsageMonitor?.checkMemoryAndProceed {
                captureImage()
            }
        }
        memoryUsageMonitor = MemoryUsageMonitor(this, getSystemService(ACTIVITY_SERVICE) as ActivityManager, lowMemoryThreshold = MNCIdentifier.lowMemoryThreshold)
    }

    private fun captureImage(){
        val photoFile = File.createTempFile("selfiektp",".jpg")
        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = true
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
                    extractFace(savedUri)
                }
            })

        // We can only change the foreground Drawable using API level 23+ API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Display flash animation to indicate that photo was captured
            rootView.postDelayed({
                rootView.foreground = ColorDrawable(Color.WHITE)
                rootView.postDelayed(
                    {  rootView.foreground = null }, ANIMATION_FAST_MILLIS)
            }, ANIMATION_SLOW_MILLIS)
        }
    }

    override fun startCamera(cameraProvider: ProcessCameraProvider, previewView: PreviewView) {
        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

        // Preview
        val previewUseCase = Preview.Builder().build()

        // ImageCapture
        captureUseCase = ImageCapture.Builder().apply {
            setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
        }.build()


        //Image Analysis
        val analysisUseCase = ImageAnalysis.Builder().build()
            .also {
                imageAnalyzer.let { analyzer ->
                    it.setAnalyzer(cameraExecutor, analyzer) }
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            cameraProvider.bindToLifecycle(
                this, cameraSelector, previewUseCase, captureUseCase, analysisUseCase)
            // Attach the viewfinder's surface provider to preview use case
            previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun extractFace(uri: Uri) {
        showProgressDialog()
        val faceImages = mutableListOf<Uri>()
        val originalBitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, uri, onError = { message ->
            Log.e(TAG, message)
            handleResult(false,message, uri, faceImages)
        })
        originalBitmap?.let {
            faceDetector.process(InputImage.fromFilePath(this, uri))
                .addOnSuccessListener {
                    it.forEachIndexed { index, face ->
                        val croppedFace = Bitmap.createBitmap(
                            originalBitmap,
                            face.boundingBox.left,
                            face.boundingBox.top,
                            face.boundingBox.width(),
                            face.boundingBox.height())
                        val faceUri = BitmapUtils.saveBitmapToFile(croppedFace,
                            filesDir.absolutePath,
                            "face${index+1}.jpg")
                        faceImages.add(faceUri)
                    }
                }
                .addOnCompleteListener {
                    handleResult(true,"Success", uri, faceImages)
                }
        }
    }

    private fun handleResult(isSuccess: Boolean, message: String, uri: Uri, faceImages: List<Uri>) {
        val selfieResult = SelfieWithKtpResult(isSuccess, message, uri, faceImages)
        val intent = Intent().apply {
            putExtra(EXTRA_RESULT, selfieResult)
        }
        setResult(RESULT_OK, intent)
        hideProgressDialog()
        finish()
    }

    private fun startCountdownTimer(){
        if(timer != null ) return
        countdownTime = COUNTDOWN_TIME
        timer = fixedRateTimer(initialDelay = 0, period = 1000){
            runOnUiThread {
                tvTimer.apply {
                    visibility = View.VISIBLE
                    text = countdownTime.toString()
                }
                countdownTime--
                if (countdownTime==0){
                    isCaptured = true
                    stopTimer()
                    captureImage()
                }
            }
        }
    }

    private fun stopTimer(){
        timer?.cancel()
        timer = null
        tvTimer.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    private fun showProgressDialog(){
        runOnUiThread {
            progressDialog.show()
        }
    }

    private fun hideProgressDialog(){
        runOnUiThread {
            progressDialog.dismiss()
        }
    }

    override fun onFaceDetectionSuccess(faces: List<Face>) {
        if (isCaptured) return
        if (faces.size > 1) {
            startCountdownTimer()
            tvFaceNotFound.visibility = View.INVISIBLE
        } else {
            stopTimer()
            tvFaceNotFound.visibility = View.VISIBLE
        }
    }

    override fun onFaceDetectionFailure(exception: Exception) {

    }

    companion object{
        const val TAG = "SelfieKtp"
        const val COUNTDOWN_TIME = 3
        const val ANIMATION_FAST_MILLIS = 50L
        const val ANIMATION_SLOW_MILLIS = 100L
    }
}
package id.mncinnovation.ocr

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import id.mncinnovation.ocr.model.CaptureKtpResult
import id.mncinnovation.ocr.utils.extractEktp
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorMatrixFilter
import java.io.File

@Suppress("DEPRECATION")
class CaptureKtpActivity : BaseCameraActivity() {
    private lateinit var uiContainer: View
    private lateinit var btnCapture: ImageButton

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
        gpuImage = GPUImage(this).apply {
            setFilter(GPUImageColorMatrixFilter(1f,
                floatArrayOf(
                    0f,0f,1.3f,0f,
                    0f,0f,1.3f,0f,
                    0f,0f,1.3f,0f,
                    0f,0f,0f,1f)))
        }
        uiContainer = LayoutInflater.from(this).inflate(R.layout.activity_capture_ktp, rootView, true)
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
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview
        val previewUseCase = Preview.Builder().build()

        // ImageCapture
        captureUseCase = ImageCapture.Builder().apply {
            setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        }.build()


        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            cameraProvider.bindToLifecycle(
                this, cameraSelector, previewUseCase, captureUseCase)
            // Attach the viewfinder's surface provider to preview use case
            previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun captureImage(){
        val photoFile = File.createTempFile("selfiektp",".jpg")
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

    private fun extractDataKtp(uri: Uri){
        showProgressDialog()
        val imageBitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, uri)
            ?: return
        objectDetector.process(InputImage.fromBitmap(imageBitmap, 0))
            .addOnSuccessListener { objects ->
                val cropedBitmap = if (objects.isEmpty()) imageBitmap else
                    Bitmap.createBitmap(imageBitmap,
                        objects.first().boundingBox.left,
                        objects.first().boundingBox.top,
                        objects.first().boundingBox.width(),
                        objects.first().boundingBox.height())
                val filteredBitmap = gpuImage.getBitmapWithFilterApplied(cropedBitmap)

                textRecognizer.process(InputImage.fromBitmap(filteredBitmap,0))
                    .addOnSuccessListener { text ->
                        val ekp = text.extractEktp()
                        val resultUri = saveBitmapToFile(filteredBitmap,filesDir.absolutePath,"ktpocr.jpg")
                        val ocrResult = CaptureKtpResult(true,"Success", resultUri, ekp)
                        val intent = Intent().apply {
                            putExtra(EXTRA_RESULT,ocrResult)
                        }
                        setResult(RESULT_OK, intent)
                        hideProgressDialog()
                        finish()
                    }
            }
    }

    private fun showProgressDialog(){
        runOnUiThread {
            progressDialog.show()
        }
    }
    private fun hideProgressDialog(){
        progressDialog.dismiss()
    }

    companion object{
        const val TAG = "CaptureKtpActivity"
    }
}

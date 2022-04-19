package id.mncinnovation.ocr.analyzer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import id.mncinnovation.identification.core.utils.BitmapUtils

class CaptureKtpAnalyzer(private val listener: CaptureKtpListener) :
    ImageAnalysis.Analyzer {

    private val localModel =
        LocalModel.Builder().setAssetFilePath("custom_models/object_labeler.tflite").build()
    private var option =
        CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()
            .setMaxPerObjectLabelCount(1)
            .build()
    private val objectDetector = ObjectDetection.getClient(option)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val originalBitmap = BitmapUtils.getBitmap(image)
        Log.e(TAG, "MASUK ANALYZE")
        if (originalBitmap != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                val inputImage = InputImage.fromMediaImage(
                    image.image!!,
                    image.imageInfo.rotationDegrees
                )
                objectDetector.process(inputImage)
                    .addOnSuccessListener { detectedObjects ->
                        Log.d(
                            TAG,
                            detectedObjects.firstOrNull()?.labels?.firstOrNull()?.text
                                ?: "Label Not Found"
                        )
                        if (detectedObjects.firstOrNull()?.labels?.firstOrNull()?.text in listOf(
                                "Driver's license",
                                "Passport"
                            )
                        ) {
                            listener.onStatusChanged(Status.SCANNING)
                            val box = detectedObjects.first().boundingBox
                            val croppedBitmap = Bitmap.createBitmap(
                                originalBitmap,
                                box.left,
                                box.top,
                                box.width(),
                                box.height()
                            )
                            Handler(Looper.getMainLooper()).postDelayed({
                                listener.onCaptureComplete(bitmap = croppedBitmap)
                                listener.onStatusChanged(Status.COMPLETE)
                                image.close()
                            }, 3000)
                        } else {
                            listener.onStatusChanged(Status.NOT_FOUND)
                            image.close()
                        }
                    }.addOnFailureListener {
                        listener.onCaptureFailed(it)
                        image.close()
                    }
            }, 2000)

        }
    }

    companion object {
        private const val TAG = "CustomObjectAnalyzer"
    }
}
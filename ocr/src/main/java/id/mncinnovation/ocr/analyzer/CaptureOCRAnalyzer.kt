package id.mncinnovation.ocr.analyzer

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import id.mncinnovation.identification.core.utils.BitmapUtils

class CaptureOCRAnalyzer(private val listener: CaptureKtpListener) :
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
        if (originalBitmap != null) {
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
                    } else {
                        listener.onStatusChanged(Status.NOT_FOUND)
                    }
                }.addOnFailureListener {
                    listener.onCaptureFailed(it)
                }.addOnCompleteListener {
                    image.close()
                }

        }
    }

    companion object {
        private const val TAG = "CustomObjectAnalyzer"
    }
}
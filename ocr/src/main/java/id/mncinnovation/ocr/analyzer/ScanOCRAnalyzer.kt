package id.mncinnovation.ocr.analyzer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.ocr.model.KTPModel
import id.mncinnovation.ocr.utils.extractEktp

class ScanOCRAnalyzer(private val listener: ScanKtpListener) :
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
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val listEktp = mutableListOf<KTPModel>()

    @SuppressLint( "UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val originalBitmap = BitmapUtils.getBitmap(image)
        if (originalBitmap != null) {
            val inputImage = InputImage.fromMediaImage(image.image!!,
                image.imageInfo.rotationDegrees)
            objectDetector.process(inputImage)
                .addOnSuccessListener { detectedObjects ->
                    Log.d(TAG, detectedObjects.firstOrNull()?.labels?.firstOrNull()?.text?:"Label Not Found")
                    if (detectedObjects.firstOrNull()?.labels?.firstOrNull()?.text in listOf("Driver's license","Passport")) {
                        listener.onStatusChanged(Status.SCANNING)
                        val box = detectedObjects.first().boundingBox
                        val croppedBitmap = Bitmap.createBitmap(originalBitmap, box.left, box.top, box.width(), box.height())
                        val textInputImage = InputImage.fromMediaImage(image.image!!,image.imageInfo.rotationDegrees)
                        textRecognizer.process(textInputImage)
                            .addOnSuccessListener {
                                if ("\\d{16}".toRegex().containsMatchIn(it.text)) {
                                    listEktp.add(it.extractEktp().apply {
                                        bitmap = croppedBitmap
                                    })
                                    val progress = listEktp.size * 100 / BUFFER_SIZE
                                    listener.onProgress(progress)
                                }
                            }
                            .addOnCompleteListener {
                                if (listEktp.size == BUFFER_SIZE) {
                                    val ektp = listEktp.findBestResult()
                                    ektp?.let {
                                        listener.onScanComplete(it)
                                    } ?: listener.onScanFailed(Exception("Failed Scan Ektp"))
                                    listener.onStatusChanged(Status.COMPLETE)
                                }
                                image.close()
                            }
                    } else {
                        listEktp.clear()
                        listener.onStatusChanged(Status.NOT_FOUND)
                        image.close()
                    }
                }
        }
    }

    private fun List<KTPModel>.findBestResult(): KTPModel? {
        val highestConfidence = maxByOrNull {
            it.confidence
        }?.apply {
            if (provinsi.isNullOrEmpty()) provinsi = firstNotNullOfOrNull { it.provinsi }
            if (kabKot.isNullOrEmpty()) kabKot = firstNotNullOfOrNull { it.kabKot }
            if (nama.isNullOrEmpty()) nama = maxByOrNull { it.nama?.length ?: 0 }?.nama
            if (tempatLahir.isNullOrEmpty()) tempatLahir = firstNotNullOfOrNull { it.tempatLahir }
            if (tglLahir.isNullOrEmpty()) tglLahir = firstNotNullOfOrNull { it.tglLahir }
            if (jenisKelamin.isNullOrEmpty()) jenisKelamin = maxByOrNull { it.jenisKelamin?.length ?: 0 }?.jenisKelamin
            if (alamat.isNullOrEmpty()) alamat = firstNotNullOfOrNull { it.alamat }
            if (rt.isNullOrEmpty()) rt = firstNotNullOfOrNull { it.rt }
            if (rw.isNullOrEmpty()) rw = firstNotNullOfOrNull { it.rw }
            if (kelurahan.isNullOrEmpty()) kelurahan = firstNotNullOfOrNull { it.kelurahan }
            if (kecamatan.isNullOrEmpty()) kecamatan = firstNotNullOfOrNull { it.kecamatan }
            if (agama.isNullOrEmpty()) agama = firstNotNullOfOrNull { it.agama }
            if (statusPerkawinan.isNullOrEmpty()) statusPerkawinan = firstNotNullOfOrNull { it.statusPerkawinan }
            if (pekerjaan.isNullOrEmpty()) pekerjaan = firstNotNullOfOrNull { it.pekerjaan }
            if (kewarganegaraan.isNullOrEmpty()) kewarganegaraan = firstNotNullOfOrNull { it.kewarganegaraan }
            if (berlakuHingga.isNullOrEmpty()) berlakuHingga = firstNotNullOfOrNull { it.berlakuHingga }
        }
        return highestConfidence
    }

    companion object {
        private const val TAG = "CustomObjectAnalyzer"
        private const val BUFFER_SIZE = 5
    }
}
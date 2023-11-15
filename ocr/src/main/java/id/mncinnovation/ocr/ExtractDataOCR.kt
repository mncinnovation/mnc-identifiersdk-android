package id.mncinnovation.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import id.mncinnovation.identification.core.common.ResultErrorType
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.ocr.model.KTPModel
import id.mncinnovation.ocr.model.OCRResultModel
import id.mncinnovation.ocr.utils.extractEktp
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorMatrixFilter

class ExtractDataOCR(private val context: Context, private val listener: ExtractDataOCRListener) {

    private var option =
        CustomObjectDetectorOptions.Builder(
            LocalModel.Builder().setAssetFilePath("custom_models/object_labeler.tflite").build()
        )
            .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableClassification()
            .setMaxPerObjectLabelCount(1)
            .build()
    private val objectDetector = ObjectDetection.getClient(option)
    private val gpuImage: GPUImage = GPUImage(context).apply {
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
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var ktpList = mutableListOf<KTPModel>()

    fun processExtractData(uriList: List<Uri>) {
        listener.onStart()
        fun onError(index: Int, uri: Uri, message: String, errorType: ResultErrorType) {
            if (index == uriList.size - 1) {
                if (ktpList.isEmpty()) {
                    listener.onError(message, errorType)
                } else {
                    filterResult(uri)
                }
            }
        }
        uriList.forEachIndexed { index, uri ->
            BitmapUtils.getBitmapFromContentUri(
                context.contentResolver,
                uri
            ) { message, errorType ->
                onError(index, uri, message, errorType)
            }?.let { imageBitmap ->
                val defaultErrorMsg = "Terjadi kesalahan pada saat memproses gambar"
                objectDetector.process(InputImage.fromBitmap(imageBitmap, 0))
                    .addOnFailureListener {
                        onError(
                            index,
                            uri,
                            it.message ?: defaultErrorMsg,
                            ResultErrorType.EXCEPTION
                        )
                    }
                    .addOnSuccessListener { objects ->
                        val cropedBitmap = if (objects.isEmpty()) imageBitmap else
                            Bitmap.createBitmap(
                                imageBitmap,
                                objects.first().boundingBox.left,
                                objects.first().boundingBox.top,
                                objects.first().boundingBox.width(),
                                objects.first().boundingBox.height()
                            )
                        val resultUri =
                            BitmapUtils.saveBitmapToFile(
                                cropedBitmap,
                                context.filesDir.absolutePath,
                                "ktpocr.jpg",
                                onError = { message, errorType ->
                                    onError(index, uri, message, errorType)
                                }
                            )
                        val filteredBitmap : Bitmap = try {
                             gpuImage.getBitmapWithFilterApplied(cropedBitmap)
                        } catch (e: OutOfMemoryError) {
                            e.printStackTrace()
                            cropedBitmap
                        } catch (e: Exception) {
                            e.printStackTrace()
                            cropedBitmap
                        }

                        textRecognizer.process(InputImage.fromBitmap(filteredBitmap, 0))
                            .addOnFailureListener {
                                onError(
                                    index,
                                    uri,
                                    it.message ?: defaultErrorMsg,
                                    ResultErrorType.EXCEPTION
                                )
                            }
                            .addOnSuccessListener { text ->
                                val ktp = text.extractEktp()
                                ktpList.add(ktp)
                                if (ktpList.size == uriList.size) {
                                    filterResult(resultUri)
                                }
                            }
                    }
            }
        }
    }

    private fun filterResult(uri: Uri) {
        val usedKtp = ktpList.first()
        if (ktpList.size > 1) {
            for (i in 1 until ktpList.size) {
                val nextKtp = ktpList[i]
                if (usedKtp.confidence <= nextKtp.confidence) {
                    if (usedKtp.nik == null) {
                        usedKtp.nik = nextKtp.nik.takeIf { it != null }
                    }
                    if (usedKtp.nama == null) {
                        usedKtp.nama =
                            nextKtp.nama.takeIf { it != null }
                    }
                    if (usedKtp.tempatLahir == null) {
                        usedKtp.tempatLahir =
                            nextKtp.tempatLahir.takeIf { it != null }
                    }
                    if (usedKtp.golDarah == null) {
                        usedKtp.golDarah =
                            nextKtp.golDarah.takeIf { it != null }
                    }
                    if (usedKtp.tglLahir == null) {
                        usedKtp.tglLahir =
                            nextKtp.tglLahir.takeIf { it != null }
                    }
                    if (usedKtp.jenisKelamin == null) {
                        usedKtp.jenisKelamin =
                            nextKtp.jenisKelamin.takeIf { it != null }
                    }
                    if (usedKtp.alamat == null) {
                        usedKtp.alamat =
                            nextKtp.alamat.takeIf { it != null }
                    }
                    if (usedKtp.rt == null) {
                        usedKtp.rt = nextKtp.rt.takeIf { it != null }
                    }
                    if (usedKtp.rw == null) {
                        usedKtp.rw = nextKtp.rw.takeIf { it != null }
                    }
                    if (usedKtp.kelurahan == null) {
                        usedKtp.kelurahan =
                            nextKtp.kelurahan.takeIf { it != null }
                    }
                    if (usedKtp.kecamatan == null) {
                        usedKtp.kecamatan =
                            nextKtp.kecamatan.takeIf { it != null }
                    }
                    if (usedKtp.agama == null) {
                        usedKtp.agama =
                            nextKtp.agama.takeIf { it != null }
                    }
                    if (usedKtp.statusPerkawinan == null) {
                        usedKtp.statusPerkawinan =
                            nextKtp.statusPerkawinan.takeIf { it != null }
                    }
                    if (usedKtp.pekerjaan == null) {
                        usedKtp.pekerjaan =
                            nextKtp.pekerjaan.takeIf { it != null }
                    }
                    if (usedKtp.kewarganegaraan == null) {
                        usedKtp.kewarganegaraan =
                            nextKtp.kewarganegaraan.takeIf { it != null }
                    }
                    if (usedKtp.berlakuHingga == null) {
                        usedKtp.berlakuHingga =
                            nextKtp.berlakuHingga.takeIf { it != null }
                    }
                    if (usedKtp.provinsi == null) {
                        usedKtp.provinsi =
                            nextKtp.provinsi.takeIf { it != null }
                    }
                    if (usedKtp.kabKot == null) {
                        usedKtp.kabKot =
                            nextKtp.kabKot.takeIf { it != null }
                    }
                }
            }
        }

        val ocrResult =
            OCRResultModel(true, "Success", errorType = null, uri.path, usedKtp)
        listener.onFinish(ocrResult)
    }
}

/**
 * An interface to listen onStart process and onFinish of process extract data OCR
 */
interface ExtractDataOCRListener {
    /**
     * Function to listen onStart process of extract data ocr
     */
    fun onStart()

    /**
     * Function to listen onFinish process of extract data ocr
     * @param result an result data ocr
     */
    fun onFinish(result: OCRResultModel)

    /**
     * Function to listen onFailed process of extract data ocr
     */
    fun onError(message: String?, errorType: ResultErrorType?)
}
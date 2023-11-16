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
import id.mncinnovation.identification.core.common.ERROR_MSG_IMAGE_PROCESS_DEFAULT
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.ocr.model.KTPModel
import id.mncinnovation.ocr.model.OCRResultModel
import id.mncinnovation.ocr.utils.extractEktp
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorMatrixFilter
import java.lang.Exception

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

    fun processExtractDataBitmap(bitmapList: List<Bitmap?>) {
        listener.onStart()
        val notNullBitmap = bitmapList.filterNotNull()
        if (notNullBitmap.isEmpty()) {
            onError(true, "Error extract from empty list of bitmap")
            return
        }
        notNullBitmap.forEachIndexed { index, croppedBitmap ->
            val resultUri =
                BitmapUtils.saveBitmapToFile(
                    croppedBitmap,
                    context.filesDir.absolutePath,
                    "ktpocr.jpg"
                )
            val filteredBitmap: Bitmap = try {
                gpuImage.getBitmapWithFilterApplied(croppedBitmap)
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                croppedBitmap
            } catch (e: Exception) {
                e.printStackTrace()
                croppedBitmap
            }
            textRecognizer.process(InputImage.fromBitmap(filteredBitmap, 0))
                .addOnFailureListener {
                    filteredBitmap.recycle()
                    croppedBitmap.recycle()
                    onError(
                        index == bitmapList.size - 1,
                        it.message ?: ERROR_MSG_IMAGE_PROCESS_DEFAULT,
                        resultUri
                    )
                }
                .addOnSuccessListener { text ->
                    filteredBitmap.recycle()
                    croppedBitmap.recycle()
                    val ktp = text.extractEktp()
                    ktpList.add(ktp)
                    if (ktpList.size == bitmapList.size) {
                        filterResult(resultUri)
                    }
                }
        }
    }

    private fun onError(isLastPosition: Boolean, message: String, uri: Uri? = null) {
        if (isLastPosition) {
            if (ktpList.isEmpty()) {
                listener.onError(message)
            } else {
                uri?.let { filterResult(it) }
            }
        }
    }

    fun processExtractDataUri(uriList: List<Uri>) {
        listener.onStart()
        uriList.forEachIndexed { index, uri ->
            BitmapUtils.getBitmapFromContentUri(context.contentResolver, uri) { message ->
                onError(index == uriList.size - 1, message, uri)
            }?.let { imageBitmap ->
                objectDetector.process(InputImage.fromBitmap(imageBitmap, 0))
                    .addOnFailureListener {
                        imageBitmap.recycle()
                        onError(
                            index == uriList.size - 1,
                            it.message ?: ERROR_MSG_IMAGE_PROCESS_DEFAULT,
                            uri
                        )
                    }
                    .addOnSuccessListener { objects ->
                        val croppedBitmap = if (objects.isEmpty()) imageBitmap else
                            Bitmap.createBitmap(
                                imageBitmap,
                                objects.first().boundingBox.left,
                                objects.first().boundingBox.top,
                                objects.first().boundingBox.width(),
                                objects.first().boundingBox.height()
                            )
                        val resultUri =
                            BitmapUtils.saveBitmapToFile(
                                croppedBitmap,
                                context.filesDir.absolutePath,
                                "ktpocr.jpg"
                            )
                        val filteredBitmap: Bitmap = try {
                            gpuImage.getBitmapWithFilterApplied(croppedBitmap)
                        } catch (e: OutOfMemoryError) {
                            e.printStackTrace()
                            croppedBitmap
                        } catch (e: Exception) {
                            e.printStackTrace()
                            croppedBitmap
                        }

                        textRecognizer.process(InputImage.fromBitmap(filteredBitmap, 0))
                            .addOnFailureListener {
                                imageBitmap.recycle()
                                croppedBitmap.recycle()
                                onError(
                                    index == uriList.size - 1,
                                    it.message ?: ERROR_MSG_IMAGE_PROCESS_DEFAULT,
                                    uri
                                )
                            }
                            .addOnSuccessListener { text ->
                                imageBitmap.recycle()
                                croppedBitmap.recycle()
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
                    if (usedKtp.nik.isNullOrBlank()) {
                        usedKtp.nik = nextKtp.nik.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.nama.isNullOrBlank()) {
                        usedKtp.nama =
                            nextKtp.nama.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.tempatLahir.isNullOrBlank()) {
                        usedKtp.tempatLahir =
                            nextKtp.tempatLahir.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.golDarah.isNullOrBlank()) {
                        usedKtp.golDarah =
                            nextKtp.golDarah.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.tglLahir.isNullOrBlank()) {
                        usedKtp.tglLahir =
                            nextKtp.tglLahir.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.jenisKelamin.isNullOrBlank()) {
                        usedKtp.jenisKelamin =
                            nextKtp.jenisKelamin.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.alamat.isNullOrBlank()) {
                        usedKtp.alamat =
                            nextKtp.alamat.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.rt.isNullOrBlank()) {
                        usedKtp.rt = nextKtp.rt.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.rw.isNullOrBlank()) {
                        usedKtp.rw = nextKtp.rw.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.kelurahan.isNullOrBlank()) {
                        usedKtp.kelurahan =
                            nextKtp.kelurahan.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.kecamatan.isNullOrBlank()) {
                        usedKtp.kecamatan =
                            nextKtp.kecamatan.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.agama.isNullOrBlank()) {
                        usedKtp.agama =
                            nextKtp.agama.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.statusPerkawinan.isNullOrBlank()) {
                        usedKtp.statusPerkawinan =
                            nextKtp.statusPerkawinan.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.pekerjaan.isNullOrBlank()) {
                        usedKtp.pekerjaan =
                            nextKtp.pekerjaan.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.kewarganegaraan.isNullOrBlank()) {
                        usedKtp.kewarganegaraan =
                            nextKtp.kewarganegaraan.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.berlakuHingga.isNullOrBlank()) {
                        usedKtp.berlakuHingga =
                            nextKtp.berlakuHingga.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.provinsi.isNullOrBlank()) {
                        usedKtp.provinsi =
                            nextKtp.provinsi.takeIf { !it.isNullOrBlank() }
                    }
                    if (usedKtp.kabKot.isNullOrBlank()) {
                        usedKtp.kabKot =
                            nextKtp.kabKot.takeIf { !it.isNullOrBlank() }
                    }
                }
            }
        }

        val ocrResult =
            OCRResultModel(true, "Success", uri.path, usedKtp)
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
    fun onError(message: String?)
}
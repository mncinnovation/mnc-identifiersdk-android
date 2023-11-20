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
import id.mncinnovation.identification.core.common.ResultErrorType
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
            onError(true, "Error extract from empty list of bitmap", ResultErrorType.EXCEPTION)
            return
        }
        val resultUri =
            BitmapUtils.saveBitmapToFile(
                notNullBitmap.last(),
                context.filesDir.absolutePath,
                "ktpocr.jpg",
                onError = { message, errorType ->
                    onError(true, message, ResultErrorType.EXCEPTION)
                }
            )
        notNullBitmap.forEachIndexed { index, bitmap ->
            val filteredBitmap: Bitmap = try {
                gpuImage.getBitmapWithFilterApplied(bitmap)
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                bitmap
            }
            textRecognizer.process(InputImage.fromBitmap(filteredBitmap, 0))
                .addOnFailureListener {
                    filteredBitmap.recycle()
                    onError(
                        index == bitmapList.size - 1,
                        it.message ?: ERROR_MSG_IMAGE_PROCESS_DEFAULT,
                        ResultErrorType.EXCEPTION,
                        resultUri
                    )
                }
                .addOnSuccessListener { text ->
                    filteredBitmap.recycle()
                    val ktp = text.extractEktp()
                    ktpList.add(ktp)
                    if (ktpList.size == bitmapList.size) {
                        filterResult(resultUri, bitmapList)
                    }
                }
        }
    }

    private fun onError(isLastPosition: Boolean, message: String, errorType: ResultErrorType, uri: Uri? = null) {
        if (isLastPosition) {
            if (ktpList.isEmpty()) {
                listener.onError(message, errorType)
            } else {
                uri?.let { filterResult(it) }
            }
        } else {
            //add dummy data to trigger filter result by ktp.size == source.size
            ktpList.add(KTPModel())
        }
    }

    fun processExtractDataUri(uriList: List<Uri>) {
        listener.onStart()
        val listCroppedImage = mutableListOf<Bitmap?>()
        uriList.forEachIndexed { index, uri ->
            BitmapUtils.getBitmapFromContentUri(context.contentResolver, uri) { message, errorType ->
                onError(index == uriList.size - 1, message, errorType, uri)
            }?.let { imageBitmap ->
                objectDetector.process(InputImage.fromBitmap(imageBitmap, 0))
                    .addOnFailureListener {
                        imageBitmap.recycle()
                        onError(
                            index == uriList.size - 1,
                            it.message ?: ERROR_MSG_IMAGE_PROCESS_DEFAULT,
                            ResultErrorType.EXCEPTION,
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
                        imageBitmap.recycle()
                        listCroppedImage.add(croppedBitmap)
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
                                onError(
                                    index == uriList.size - 1,
                                    it.message ?: ERROR_MSG_IMAGE_PROCESS_DEFAULT,
                                    ResultErrorType.EXCEPTION,
                                    uri
                                )
                            }
                            .addOnSuccessListener { text ->
                                filteredBitmap.recycle()
                                val ktp = text.extractEktp()
                                ktpList.add(ktp)
                                if (ktpList.size == uriList.size) {
                                    val resultUri =
                                        BitmapUtils.saveBitmapToFile(
                                            croppedBitmap,
                                            context.filesDir.absolutePath,
                                            "ktpocr.jpg",
                                            removeBitmap = true,
                                            onError = { message, errorType ->
                                                onError(
                                                    index == uriList.size - 1,
                                                    message,
                                                    errorType,
                                                    uri
                                                )
                                            }
                                        )
                                    filterResult(resultUri, listCroppedImage)
                                }
                            }
                    }
            }
        }
    }

    private fun filterResult(uri: Uri, bitmapList: List<Bitmap?>? = null) {
        val usedKtp = ktpList.first()
        var indexBitmap = 0
        if (ktpList.size > 1) {
            for (i in 1 until ktpList.size) {
                val nextKtp = ktpList[i]
                if (usedKtp.confidence <= nextKtp.confidence) {
                    indexBitmap = i
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

        val resultUri =
            bitmapList?.getOrNull(indexBitmap)?.let {
                BitmapUtils.saveBitmapToFile(
                    it,
                    context.filesDir.absolutePath,
                    "ktpocr.jpg",
                    onError = { message, errorType ->
                        onError(true, message, errorType, uri)
                    }
                )
            } ?: uri
        bitmapList?.forEach { it?.recycle() }
        val ocrResult =
            OCRResultModel(true, "Success", errorType = null, resultUri.path, usedKtp)
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
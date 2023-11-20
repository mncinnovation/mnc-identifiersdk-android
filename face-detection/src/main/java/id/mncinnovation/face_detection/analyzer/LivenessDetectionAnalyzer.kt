package id.mncinnovation.face_detection.analyzer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import id.mncinnovation.face_detection.SelfieWithKtpActivity
import id.mncinnovation.face_detection.model.LivenessResult
import id.mncinnovation.face_detection.utils.FileUtils
import id.mncinnovation.identification.core.GraphicOverlay
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.identification.core.utils.BitmapUtils.saveBitmapToFile

class LivenessDetectionAnalyzer(
    private val context: Context,
    private val listDetectionMode: List<DetectionMode>,
    private val detectionArea: Rect,
    private val graphicOverlay: GraphicOverlay,
    private val drawFaceGraphic: Boolean,
    private val listener: LivenessDetectionListener,
) : ImageAnalysis.Analyzer {
    private val classificationDetector: FaceDetector
    private val countourDetector: FaceDetector
    private var faceStatus: FaceStatus = FaceStatus.NOT_FOUND
    private var queueDetectionMode = mutableListOf<DetectionMode>()
    private var isMouthOpen: Boolean = false
    private var startHoldStillTimemilis: Long? = null
    private val startTimeMilis = System.currentTimeMillis()
    private var startDetectionTime: Long? = null
    private var originalBitmap: Bitmap? = null
    private var originalBitmapList = mutableListOf<Bitmap?>()
    private val detectionResults: MutableList<LivenessResult.DetectionResult> = mutableListOf()

    init {
        /**
         * For better performance, separate between classification and countour options,
         * see [https://developers.google.com/ml-kit/vision/face-detection/android#4.-process-the-image]
        **/
        val classificationOptions = FaceDetectorOptions.Builder()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setMinFaceSize(0.3f)
            .build()

        val contourOptions = FaceDetectorOptions.Builder()
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setMinFaceSize(0.3f)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()

        classificationDetector = FaceDetection.getClient(classificationOptions)
        countourDetector = FaceDetection.getClient(contourOptions)
        queueDetectionMode = listDetectionMode.toMutableList()
    }

    private val isBlinkModeOnly get() = listDetectionMode.size == 1 && listDetectionMode.getOrNull(0) == DetectionMode.BLINK

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees == 0 || rotationDegrees == 180) {
            graphicOverlay.setImageSourceInfo(image.width, image.height, true)
        }
        else{
            graphicOverlay.setImageSourceInfo(image.height, image.width, true)
        }
        BitmapUtils.getBitmap(image)?.let {
            originalBitmap = it
            if(isBlinkModeOnly) {
                originalBitmapList.add(it)
            }
        } ?: kotlin.run { return }

        val inputImage = InputImage.fromMediaImage(image.image!!,
            rotationDegrees)
        when (currentDetectionMode()) {
            DetectionMode.BLINK,
            DetectionMode.SMILE,
            DetectionMode.SHAKE_HEAD,
            DetectionMode.HOLD_STILL -> {
                classificationDetector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        handleFaces(faces)
                    }
                    .addOnCompleteListener {
                        image.close()
                    }
            }
            DetectionMode.OPEN_MOUTH -> {
                countourDetector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        handleFaces(faces)
                    }
                    .addOnCompleteListener {
                        image.close()
                    }
            }
            else -> {
                image.close()
            }
        }
    }

    private fun handleFaces(faces: List<Face>){
        graphicOverlay.clear()
        if (faces.isEmpty()) {
            setFaceStatus(FaceStatus.NOT_FOUND)
        }
        else
            for (face in faces){
                val faceGraphic = FaceGraphic(graphicOverlay,face)
                if(drawFaceGraphic){
                    graphicOverlay.add(faceGraphic)
                }
                if (checkFaceStatus(faceGraphic) == FaceStatus.READY) {
                    currentDetectionMode()?.let {
                        detectGesture(face, it)
                    }
                }
            }
    }

    private fun detectGesture(face: Face, detectionMode: DetectionMode) {
        when (detectionMode) {
            DetectionMode.BLINK -> detectBlink(face)
            DetectionMode.SHAKE_HEAD -> detectShakeHead(face)
            DetectionMode.OPEN_MOUTH -> detectMouthOpen(face)
            DetectionMode.SMILE -> detectSmile(face)
            DetectionMode.HOLD_STILL -> detectHoldStill(face)
        }
    }

    private fun detectHoldStill(face: Face){
        if (face.headEulerAngleY < 5 && face.headEulerAngleY > -5) {
            if (startHoldStillTimemilis == null)
                startHoldStillTimemilis = System.currentTimeMillis()
            else{
                startHoldStillTimemilis?.let {
                    if((System.currentTimeMillis() - it) > 2000){
                        nextDetection()
                    }
                }
            }
        }
        else{
            startHoldStillTimemilis = null
        }
    }

    private fun detectSmile(face: Face){
        Log.d(TAG, "Smile Probability ${face.smilingProbability}")
        if (face.smilingProbability?:0f > 0.8f){
            nextDetection()
        }
    }

    private fun detectBlink(face: Face) {
        Log.d(TAG, "LeftEyeOpenProbability ${face.leftEyeOpenProbability} RightEyeOpenProbability ${face.rightEyeOpenProbability}")
        if (face.leftEyeOpenProbability != null && face.rightEyeOpenProbability != null) {
            if (face.leftEyeOpenProbability!! < 0.1f && face.rightEyeOpenProbability!! < 0.1f) {
                nextDetection()
            }
        }
    }

    private fun detectMouthOpen(face: Face) {
        val topLip = face.getContour(FaceContour.LOWER_LIP_TOP)
        val bottomLip = face.getContour(FaceContour.UPPER_LIP_BOTTOM)

        if (topLip != null && bottomLip != null) {
            val delta = topLip.points[4].y - bottomLip.points[4].y
            Log.d(TAG, "Delta Lip $delta FaceHeight ${face.boundingBox.height()}")
            if (delta > 25) {
                isMouthOpen = true
            }
            if (isMouthOpen && delta < 10) {
                isMouthOpen = false
                nextDetection()
            }
        }
    }

    private fun detectShakeHead(face: Face) {
        Log.d(TAG, "HeadEulerAngleY ${face.headEulerAngleY}")
        if (face.headEulerAngleY > 30 || face.headEulerAngleY < -30) {
            nextDetection()
        }
    }

    private fun nextDetection() {
        currentDetectionMode()?.let { detectionMode ->
            val fileUri = (if(isBlinkModeOnly) originalBitmapList.getOrNull(originalBitmapList.size - 2) else originalBitmap)?.let { bitmap ->
                saveBitmapToFile(
                    bitmap,
                    context.filesDir.absolutePath,
                    "img_${detectionMode.name}.jpg",
                    true,
                    onError = { message, errorType ->
                        Log.e(SelfieWithKtpActivity.TAG, "Error: $message, Type: $errorType")
                    })
            }
            fileUri?.let { uri ->
                detectionResults.add(LivenessResult.DetectionResult(detectionMode, uri, FileUtils(context).getPath(uri), startDetectionTime?.let { time -> System.currentTimeMillis()-time }))
            }
        }
        queueDetectionMode.removeFirst()
        if (queueDetectionMode.isEmpty()) {
            if(originalBitmapList.isNotEmpty()){
                originalBitmapList.forEach { it?.recycle() }
            }
                listener.onLiveDetectionSuccess(
                    LivenessResult(
                        true,
                        "Sucess",
                        null,
                        System.currentTimeMillis() - startTimeMilis,
                        detectionResults
                    ))
        } else {
            startDetectionTime = System.currentTimeMillis()
            currentDetectionMode()?.let { listener.onStartDetection(it) }
        }
    }

    private fun currentDetectionMode() = queueDetectionMode.firstOrNull()


    private fun checkFaceStatus(face: FaceGraphic): FaceStatus {
        val faceBox = face.getBoundingBox()
        val faceStatus: FaceStatus = when {
            detectionArea.contains(faceBox) -> FaceStatus.READY
            detectionArea.height() < faceBox.height() -> FaceStatus.TOO_CLOSE
            else -> FaceStatus.NOT_READY
        }
        setFaceStatus(faceStatus)
        return faceStatus
    }

    private fun setFaceStatus(faceStatus: FaceStatus) {
        if (this.faceStatus == faceStatus) return
        this.faceStatus = faceStatus
        listener.onFaceStatusChanged(faceStatus)
        when (faceStatus) {
            FaceStatus.READY -> {
                if(startDetectionTime == null) startDetectionTime = System.currentTimeMillis()
                currentDetectionMode()?.let { listener.onStartDetection(it) }
            }
            FaceStatus.NOT_FOUND -> {
                startHoldStillTimemilis = null
                detectionResults.clear()
                refreshQueue()
            }
            else -> {
                startHoldStillTimemilis = null
            }
        }
    }

    private fun refreshQueue() {
        queueDetectionMode = listDetectionMode.toMutableList()
    }

    companion object {
        const val TAG = "LiveDetectionAnalyzer"
    }
}

enum class DetectionMode {
    BLINK, SHAKE_HEAD, OPEN_MOUTH, SMILE, HOLD_STILL
}

enum class FaceStatus {
    NOT_FOUND, NOT_READY, READY, TOO_FAR, TOO_CLOSE
}
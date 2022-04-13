package id.mncinnovation.ocr.analyzer

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import id.mncinnovation.identification.core.CameraImageGraphic
import id.mncinnovation.identification.core.GraphicOverlay
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.ocr.extensions.toBitmap
import id.mncinnovation.ocr.extensions.toMat
import id.mncinnovation.ocr.extensions.yuvToRgba
import id.mncinnovation.ocr.utils.OpenCvNativeBridge
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class PrespectiveAnalyzer(val graphicOverlay: GraphicOverlay): ImageAnalysis.Analyzer {
    private val nativeClass = OpenCvNativeBridge()
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val originalBitmap = BitmapUtils.getBitmap(image) ?: return
        val src = originalBitmap.toMat()
        val destination = Mat()
////        Imgproc.cvtColor(src,src, Imgproc.COLOR_RGB2GRAY)
        Imgproc.GaussianBlur(src, src, Size(BLURRING_KERNEL_SIZE, BLURRING_KERNEL_SIZE), 0.0)
////        Core.normalize(src, src,
////            NORMALIZATION_MIN_VALUE,
////            NORMALIZATION_MAX_VALUE, Core.NORM_MINMAX)
//////
////        Imgproc.threshold(src, src,
////            TRUNCATE_THRESHOLD,
////            NORMALIZATION_MAX_VALUE, Imgproc.THRESH_TRUNC)
////        Core.normalize(src, src,
////            NORMALIZATION_MIN_VALUE,
////            NORMALIZATION_MAX_VALUE, Core.NORM_MINMAX)
////
        Imgproc.Canny(src, destination,
            CANNY_THRESHOLD_HIGH,
            CANNY_THRESHOLD_LOW)
//////
////        Imgproc.threshold(destination, destination,
////            CUTOFF_THRESHOLD,
////            NORMALIZATION_MAX_VALUE, Imgproc.THRESH_TOZERO)
////////
        Imgproc.morphologyEx(
            destination, destination, Imgproc.MORPH_CLOSE,
            Mat(Size(CLOSE_KERNEL_SIZE, CLOSE_KERNEL_SIZE), CvType.CV_8UC1, Scalar(
                NORMALIZATION_MAX_VALUE)),
            Point(-1.0, -1.0), 1
        )
        graphicOverlay.setImageSourceInfo(originalBitmap.width, originalBitmap.height, false)
        val contour = nativeClass.getContourEdgePoints(originalBitmap)
        graphicOverlay.clear()
        graphicOverlay.add(ObjectGraphic(graphicOverlay, originalBitmap, contour))
//        graphicOverlay.add(CameraImageGraphic(graphicOverlay, destination.toBitmap()))

        image.close()
    }
    companion object{
        const val TAG = "PrespectiveAnalyzer"
        private const val ANGLES_NUMBER = 4
        private const val EPSILON_CONSTANT = 0.02
        private const val CLOSE_KERNEL_SIZE = 10.0
        private const val CANNY_THRESHOLD_LOW = 75.0
        private const val CANNY_THRESHOLD_HIGH = 200.0
        private const val CUTOFF_THRESHOLD = 155.0
        private const val TRUNCATE_THRESHOLD = 150.0
        private const val NORMALIZATION_MIN_VALUE = 0.0
        private const val NORMALIZATION_MAX_VALUE = 255.0
        private const val BLURRING_KERNEL_SIZE = 5.0
        private const val DOWNSCALE_IMAGE_SIZE = 600.0
        private const val FIRST_MAX_CONTOURS = 10
    }
}
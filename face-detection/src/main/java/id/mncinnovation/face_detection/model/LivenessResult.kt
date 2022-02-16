package id.mncinnovation.face_detection.model

import android.net.Uri
import android.os.Parcelable
import id.mncinnovation.face_detection.analyzer.DetectionMode
import id.mncinnovation.identification.core.common.Result
import kotlinx.parcelize.Parcelize


@Parcelize
data class LivenessResult(
    override val isSuccess: Boolean,
    override val errorMessage: String?,
    val totalTimeMilis: Long? = null,
    val detectionResult: List<DetectionResult>? = null,
    var attempt: Int = 0): Result(), Parcelable{

    @Parcelize
    data class DetectionResult(
        val detectionMode: DetectionMode,
        val image: Uri?,
        val timeMilis: Long?): Parcelable
}

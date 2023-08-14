package id.mncinnovation.face_detection

import android.content.Context
import android.content.Intent
import id.mncinnovation.face_detection.analyzer.DetectionMode
import id.mncinnovation.face_detection.model.LivenessResult
import id.mncinnovation.face_detection.model.SelfieWithKtpResult
import id.mncinnovation.identification.core.common.EXTRA_RESULT

object MNCIdentifier {
    private var attempt = 0
    internal var lowMemoryThreshold: Int? = null

    var detectionMode = listOf(
        DetectionMode.HOLD_STILL,
        DetectionMode.OPEN_MOUTH,
        DetectionMode.BLINK,
        DetectionMode.SHAKE_HEAD,
        DetectionMode.SMILE
    )

    @JvmStatic
    fun setDetectionModeSequence(shuffle: Boolean, detectionMode: List<DetectionMode>){
        MNCIdentifier.detectionMode = detectionMode.run {
            if (shuffle) shuffled() else this
        }
    }

    @JvmStatic
    fun setLowMemoryThreshold(threshold: Int){
        lowMemoryThreshold = threshold
    }

    @JvmStatic
    fun getLivenessIntent(context: Context): Intent {
        attempt++
        return Intent(context, SplashLivenessActivity::class.java)
    }

    @JvmStatic
    fun getLivenessResult(intent: Intent?) =
        intent?.getParcelableExtra<LivenessResult>(EXTRA_RESULT)?.apply {
            attempt = MNCIdentifier.attempt
            if(isSuccess) MNCIdentifier.attempt = 0
        }

    @JvmStatic
    fun getSelfieResult(intent: Intent?) =
        intent?.getParcelableExtra(EXTRA_RESULT) as SelfieWithKtpResult?
}
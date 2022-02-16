package id.mncinnovation.face_detection

import android.content.Context
import android.content.Intent
import id.mncinnovation.face_detection.model.LivenessResult
import id.mncinnovation.identification.core.common.EXTRA_RESULT

object MNCIdentifier {
    private var attempt = 0

    @JvmStatic
    fun getLivenessIntent(context: Context): Intent {
        attempt++
        return Intent(context, SplashActivity::class.java)
    }

    @JvmStatic
    fun getLivenessResult(intent: Intent?) =
        intent?.getParcelableExtra<LivenessResult>(EXTRA_RESULT)?.apply {
            attempt = MNCIdentifier.attempt
            if(isSuccess) MNCIdentifier.attempt = 0
        }
}
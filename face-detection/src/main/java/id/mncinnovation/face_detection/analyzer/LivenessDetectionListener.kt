package id.mncinnovation.face_detection.analyzer

import id.mncinnovation.face_detection.model.LivenessResult

interface LivenessDetectionListener {
    fun onFaceStatusChanged(faceStatus: FaceStatus)
    fun onStartDetection(detectionMode: DetectionMode)
    fun onLiveDetectionSuccess(livenessResult: LivenessResult)
    fun onLiveDetectionFailure(exception: Exception)
}
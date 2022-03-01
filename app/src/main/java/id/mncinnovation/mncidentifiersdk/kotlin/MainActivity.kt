package id.mncinnovation.mncidentifiersdk.kotlin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import id.mncinnovation.face_detection.MNCIdentifier
import id.mncinnovation.face_detection.SelfieWithKtpActivity
import id.mncinnovation.face_detection.analyzer.DetectionMode
import id.mncinnovation.identification.core.common.EXTRA_IMAGE_URI
import id.mncinnovation.identification.core.common.EXTRA_KTP
import id.mncinnovation.identification.core.common.EXTRA_LIST_IMAGE_URI
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.mncidentifiersdk.databinding.ActivityMainBinding
import id.mncinnovation.ocr.CaptureKtpActivity
import id.mncinnovation.ocr.ScanKTPActivity
import id.mncinnovation.ocr.model.Ktp

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        MNCIdentifier.setDetectionModeSequence(true, listOf(
            DetectionMode.HOLD_STILL,
            DetectionMode.BLINK,
            DetectionMode.OPEN_MOUTH,
            DetectionMode.SMILE,
            DetectionMode.SHAKE_HEAD))
        with(binding){
            btnScanKtp.setOnClickListener {
                startActivityForResult(Intent(this@MainActivity, ScanKTPActivity::class.java), SCAN_KTP_REQUEST_CODE)
            }

            btnCaptureKtp.setOnClickListener {
                startActivityForResult(Intent(this@MainActivity, CaptureKtpActivity::class.java), CAPTURE_EKTP_REQUEST_CODE)
            }

            btnLivenessDetection.setOnClickListener {
                startActivityForResult(MNCIdentifier.getLivenessIntent(this@MainActivity), LIVENESS_DETECTION_REQUEST_CODE)
            }

            btnSelfieWKtp.setOnClickListener {
                startActivityForResult(Intent(this@MainActivity, SelfieWithKtpActivity::class.java), SELFIE_WITH_KTP_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK){
            when(requestCode){
                CAPTURE_EKTP_REQUEST_CODE -> {
                    val imageUri = data?.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)
                    val ktp = data?.getParcelableExtra<Ktp>(EXTRA_KTP)
                    imageUri?.let { uri ->
                        val imageBitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, uri)
                        imageBitmap?.let { bitmap ->
                            binding.ivKtpCapture.setImageBitmap(bitmap)
                        }
                    }
                    binding.tvCaptureKtp.text = ktp.toString()
                }

                SCAN_KTP_REQUEST_CODE -> {
                    val imageUri = data?.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)
                    val ktp = data?.getParcelableExtra<Ktp>(EXTRA_KTP)
                    imageUri?.let { uri ->
                        val imageBitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, uri)
                        imageBitmap?.let { bitmap ->
                            binding.ivKtp.setImageBitmap(bitmap)
                        }
                    }
                    binding.tvScanKtp.text = ktp?.toString()?:""
                }

                LIVENESS_DETECTION_REQUEST_CODE -> {
                    val livenessResult = MNCIdentifier.getLivenessResult(data)
                    livenessResult?.let {
                        binding.tvAttempt.apply {
                            visibility = View.VISIBLE
                            text = "Attempt: ${it.attempt}"
                        }
                        val livenessResultAdapter = LivenessResultAdapter(it.detectionResult?: emptyList())
                        binding.rvLiveness.apply {
                            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL,false)
                            adapter = livenessResultAdapter
                        }
                    }
                }

                SELFIE_WITH_KTP_REQUEST_CODE -> {
                    binding.llResultSelfieWKtp.visibility = View.VISIBLE
                    val imageUri = data?.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)
                    val listFaceUri = data?.getParcelableArrayListExtra<Uri>(EXTRA_LIST_IMAGE_URI)
                    imageUri?.let { uri ->
                        val imageBitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, uri)
                        imageBitmap?.let { bitmap ->
                            binding.ivSelfieWKtpOri.setImageBitmap(bitmap)
                        }
                    }
                    listFaceUri?.forEachIndexed { index, uri ->
                        val imageBitmap = BitmapUtils.getBitmapFromContentUri(contentResolver, uri)
                        when(index){
                            0 -> {
                                binding.ivFace1.apply {
                                    visibility = View.VISIBLE
                                    setImageBitmap(imageBitmap)
                                }
                            }
                            1 -> binding.ivFace2.apply {
                                 visibility = View.VISIBLE
                                 setImageBitmap(imageBitmap)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object{
        const val LIVENESS_DETECTION_REQUEST_CODE = 101
        const val CAPTURE_EKTP_REQUEST_CODE = 102
        const val SCAN_KTP_REQUEST_CODE = 103
        const val SELFIE_WITH_KTP_REQUEST_CODE = 104
    }
}
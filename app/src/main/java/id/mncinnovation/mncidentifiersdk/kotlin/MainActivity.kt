package id.mncinnovation.mncidentifiersdk.kotlin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import id.mncinnovation.face_detection.MNCIdentifier
import id.mncinnovation.face_detection.SelfieWithKtpActivity
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.mncidentifiersdk.databinding.ActivityMainBinding
import id.mncinnovation.ocr.CaptureKtpActivity
import id.mncinnovation.ocr.MNCIdentifierOCR
import id.mncinnovation.ocr.ScanKTPActivity
import id.mncinnovation.ocr.model.Ktp

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            btnScanKtp.setOnClickListener {
                startActivityForResult(
                    Intent(this@MainActivity, ScanKTPActivity::class.java),
                    SCAN_KTP_REQUEST_CODE
                )
            }

            btnCaptureKtp.setOnClickListener {
                startActivityForResult(
                    Intent(this@MainActivity, CaptureKtpActivity::class.java),
                    CAPTURE_EKTP_REQUEST_CODE
                )
            }

            btnLivenessDetection.setOnClickListener {
                startActivityForResult(
                    MNCIdentifier.getLivenessIntent(this@MainActivity),
                    LIVENESS_DETECTION_REQUEST_CODE
                )
            }

            btnSelfieWKtp.setOnClickListener {
                startActivityForResult(
                    Intent(this@MainActivity, SelfieWithKtpActivity::class.java),
                    SELFIE_WITH_KTP_REQUEST_CODE
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAPTURE_EKTP_REQUEST_CODE -> {
                    val captureKtpResult = MNCIdentifierOCR.getCaptureKtpResult(data)
                    captureKtpResult?.let { result ->
                        result.getBitmapImage(this)?.let {
                            binding.ivKtpCapture.setImageBitmap(it)
                        }
                        binding.tvCaptureKtp.text = result.ktp.toString()
                    }

                }

                SCAN_KTP_REQUEST_CODE -> {
                    val captureKtpResult = MNCIdentifierOCR.getCaptureKtpResult(data)
                    captureKtpResult?.let { result ->
                        result.getBitmapImage(this)?.let {
                            binding.ivKtp.setImageBitmap(it)
                        }
                        binding.tvScanKtp.text = captureKtpResult.toString()
                    }
                }

                LIVENESS_DETECTION_REQUEST_CODE -> {
                    val livenessResult = MNCIdentifier.getLivenessResult(data)
                    livenessResult?.let {
                        binding.tvAttempt.apply {
                            visibility = View.VISIBLE
                            text = "Attempt: ${it.attempt}"
                        }
                        val livenessResultAdapter = LivenessResultAdapter(it)
                        binding.rvLiveness.apply {
                            layoutManager = LinearLayoutManager(
                                this@MainActivity,
                                LinearLayoutManager.HORIZONTAL,
                                false
                            )
                            adapter = livenessResultAdapter
                        }
                    }
                }

                SELFIE_WITH_KTP_REQUEST_CODE -> {
                    binding.llResultSelfieWKtp.visibility = View.VISIBLE
                    val selfieResult = MNCIdentifier.getSelfieResult(data)
                    selfieResult?.let { result ->
                        result.getBitmap(this)?.let {
                            binding.ivSelfieWKtpOri.setImageBitmap(it)
                        }
                        result.getListFaceBitmap(this).forEachIndexed { index, bitmap ->
                            when (index) {
                                0 -> {
                                    binding.ivFace1.apply {
                                        visibility = View.VISIBLE
                                        setImageBitmap(bitmap)
                                    }
                                }
                                1 -> binding.ivFace2.apply {
                                    visibility = View.VISIBLE
                                    setImageBitmap(bitmap)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val LIVENESS_DETECTION_REQUEST_CODE = 101
        const val CAPTURE_EKTP_REQUEST_CODE = 102
        const val SCAN_KTP_REQUEST_CODE = 103
        const val SELFIE_WITH_KTP_REQUEST_CODE = 104
    }
}
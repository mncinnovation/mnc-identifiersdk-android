package id.mncinnovation.mncidentifiersdk.kotlin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import id.mncinnovation.face_detection.MNCIdentifier
import id.mncinnovation.face_detection.SelfieWithKtpActivity
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.common.EXTRA_WITH_FLASH
import id.mncinnovation.identification.core.utils.BitmapUtils
import id.mncinnovation.mncidentifiersdk.databinding.ActivityMainBinding
import id.mncinnovation.ocr.CaptureKtpActivity
import id.mncinnovation.ocr.MNCIdentifierOCR
import id.mncinnovation.ocr.ScanKTPActivity
import id.mncinnovation.ocr.model.CaptureKtpResult
import id.mncinnovation.ocr.model.Ktp

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            btnScanKtp.setOnClickListener {
                resultLauncherOcr.launch(Intent(this@MainActivity, ScanKTPActivity::class.java))
            }

            btnCaptureKtp.setOnClickListener {
                MNCIdentifierOCR.startCapture(this@MainActivity, resultLauncherOcr, true)
            }

            btnLivenessDetection.setOnClickListener {
                resultLauncherLiveness.launch(MNCIdentifier.getLivenessIntent(this@MainActivity))
            }

            btnSelfieWKtp.setOnClickListener {
                resultLauncherSelfieWithKtp.launch(
                    Intent(
                        this@MainActivity,
                        SelfieWithKtpActivity::class.java
                    )
                )
            }
        }
    }

    private val resultLauncherOcr =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val captureKtpResult = MNCIdentifierOCR.getCaptureKtpResult(data)
                captureKtpResult?.let { ktpResult ->
                    ktpResult.getBitmapImage(this)?.let {
                        binding.ivKtp.setImageBitmap(it)
                    }
                    binding.tvScanKtp.text = captureKtpResult.toString()
                }
            }
        }

    private val resultLauncherLiveness =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
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
        }

    private val resultLauncherSelfieWithKtp =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val selfieResult = MNCIdentifier.getSelfieResult(data)
                selfieResult?.let { selfieWithKtpResult ->
                    selfieWithKtpResult.getBitmap(this)?.let {
                        binding.ivSelfieWKtpOri.setImageBitmap(it)
                    }
                    selfieWithKtpResult.getListFaceBitmap(this).forEachIndexed { index, bitmap ->
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
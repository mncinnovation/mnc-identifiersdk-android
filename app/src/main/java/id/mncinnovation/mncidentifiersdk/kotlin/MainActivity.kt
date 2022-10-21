package id.mncinnovation.mncidentifiersdk.kotlin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import id.mncinnovation.face_detection.MNCIdentifier
import id.mncinnovation.face_detection.SelfieWithKtpActivity
import id.mncinnovation.mncidentifiersdk.BuildConfig
import id.mncinnovation.mncidentifiersdk.databinding.ActivityMainBinding
import id.mncinnovation.ocr.ExtractDataOCRListener
import id.mncinnovation.ocr.MNCIdentifierOCR
import id.mncinnovation.ocr.ScanOCRActivity
import id.mncinnovation.ocr.model.OCRResultModel
import java.io.File


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    private var latestTmpUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding) {
            btnScanKtp.setOnClickListener {
                resultLauncherOcr.launch(Intent(this@MainActivity, ScanOCRActivity::class.java))
            }

            btnCaptureKtpOwn.setOnClickListener {
                takeImage()
            }

            btnCaptureKtp.setOnClickListener {
                MNCIdentifierOCR.config(withFlash = true, cameraOnly = true)
                MNCIdentifierOCR.startCapture(this@MainActivity, 102)
//                MNCIdentifierOCR.startCapture(
//                    this@MainActivity, resultLauncherOcr
//                )
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

    private val resultLauncherOwnCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                latestTmpUri?.let {
                    val uriList = mutableListOf<Uri>()
                    uriList.add(it)
                    MNCIdentifierOCR.extractDataFromUri(
                        uriList,
                        this@MainActivity,
                        object : ExtractDataOCRListener {
                            override fun onStart() {
                                Log.d("TAGAPP", "onStart Process Extract")
                            }

                            override fun onFinish(result: OCRResultModel) {

                                result.getBitmapImage()?.let { bitmap ->
                                    binding.ivKtp.setImageBitmap(bitmap)
                                }
                                binding.tvScanKtp.text = result.toString()

                            }
                        })
                }
            } else {
                Toast.makeText(this, "Capture image failed", Toast.LENGTH_SHORT).show()
            }
        }


    private val resultLauncherOcr =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val captureOCRResult = MNCIdentifierOCR.getOCRResult(data)
                captureOCRResult?.let { ktpResult ->
                    ktpResult.getBitmapImage()?.let {
                        binding.ivKtp.setImageBitmap(it)
                    }
                    binding.tvScanKtp.text = captureOCRResult.toString()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 102){
        if (resultCode == Activity.RESULT_OK) {
            val captureOCRResult = MNCIdentifierOCR.getOCRResult(data)
            captureOCRResult?.let { ktpResult ->
                ktpResult.getBitmapImage()?.let {
                    binding.ivKtp.setImageBitmap(it)
                }
                binding.tvScanKtp.text = captureOCRResult.toString()
            }
        }
    }
}
    private fun takeImage() {
        getTmpFileUri().let { uri ->
            latestTmpUri = uri
            resultLauncherOwnCamera.launch(uri)
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(
            applicationContext,
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }
}
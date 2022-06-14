package id.mncinnovation.ocr

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import id.mncinnovation.identification.core.base.BaseCameraActivity
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.utils.BitmapUtils.saveBitmapToFile
import id.mncinnovation.ocr.analyzer.ScanOCRAnalyzer
import id.mncinnovation.ocr.analyzer.ScanKtpListener
import id.mncinnovation.ocr.analyzer.Status
import id.mncinnovation.ocr.databinding.ActivityScanOcrBinding
import id.mncinnovation.ocr.model.OCRResultModel
import id.mncinnovation.ocr.model.KTPModel

class ScanOCRActivity : BaseCameraActivity(), ScanKtpListener {
    private lateinit var binding: ActivityScanOcrBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanOcrBinding.inflate(layoutInflater, rootView, true)
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun startCamera(cameraProvider: ProcessCameraProvider, previewView: PreviewView) {
        // CameraSelector
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview
        val previewUseCase = Preview.Builder().build()


        val analysisUseCase = ImageAnalysis.Builder().build().also {
            it.setAnalyzer(cameraExecutor, ScanOCRAnalyzer(this))
        }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            cameraProvider.bindToLifecycle(
                this, cameraSelector, previewUseCase, analysisUseCase
            )
            // Attach the viewfinder's surface provider to preview use case
            previewUseCase.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(CaptureOCRActivity.TAG, "Use case binding failed", exc)
        }
    }

    override fun onStatusChanged(status: Status) {
        Log.d(TAG, "onStatusChanged ${status.name}")
        binding.progressBar.visibility = View.INVISIBLE
        when (status) {
            Status.NOT_FOUND -> {
                binding.tvInfo.text = "KTP Tidak Ditemukan"
            }
            Status.NOT_READY -> {
                binding.tvInfo.text = "Posisikan KTP anda pada kotak yg telah disediakan"
            }
            Status.SCANNING -> {
                binding.tvInfo.text = "Scanning..."
                binding.progressBar.visibility = View.VISIBLE
            }
            else -> {
                binding.tvInfo.text = "Scan Complete"
            }
        }
    }

    override fun onProgress(progress: Int) {
        binding.progressBar.progress = progress
    }

    override fun onScanComplete(ktpModel: KTPModel) {
        ktpModel.bitmap?.let {
            val bitmapuri = saveBitmapToFile(it, filesDir.absolutePath, "scanktp.jpg")

            val scanResult =
                OCRResultModel(true, "Success", bitmapuri.path, ktpModel.apply { bitmap = it })
            val intent = Intent(this@ScanOCRActivity, ConfirmationOCRActivity::class.java).apply {
                putExtra(EXTRA_RESULT, scanResult)
            }
            resultLauncherConfirm.launch(intent)
        }
    }

    override fun onScanFailed(exception: Exception) {
        Log.d(TAG, "onScanFailed ${exception.message}")
    }

    private val resultLauncherConfirm =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val captureKtpResult = MNCIdentifierOCR.getOCRResult(data)
                captureKtpResult?.let { scanResult ->
                    val intent = Intent().apply {
                        putExtra(EXTRA_RESULT, scanResult)
                    }
                    setResult(RESULT_OK, intent)
                    finish()
                }
            }
        }

    companion object {
        private const val TAG = "ScanKtpActivity"
    }
}
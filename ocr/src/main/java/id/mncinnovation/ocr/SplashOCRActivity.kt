package id.mncinnovation.ocr

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class SplashOCRActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_ocr)

        Handler(Looper.getMainLooper()).postDelayed({
            setResult(Activity.RESULT_OK)
            finish()
            this.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 800)
    }
}
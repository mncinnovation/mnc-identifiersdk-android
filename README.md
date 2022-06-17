# MNC Identifier SDK <img src="https://img.shields.io/github/v/release/mncinnovation/mnc-identifiersdk-android.svg?label=latest"/>

![banner_identifier](/screenshots/identifier-cover.jpeg)

MNC Identifier is a service to identify, and verify consumer with AI in it.

## Table


* [About Identifier](https://mobile.mncinnovation.id/docs/mncidentifier/overview/)
* [Liveness Detection](#liveness-detection) 
* [OCR](#ocr-optical-character-recognition)


---

## Liveness Detection

![banner_liveness](/screenshots/banner_liveness.jpeg)

Liveness Detection using mlkit face recognition to detect live person present at the point of capture.

### Requirements

- Min SDK 21

### Setup

build.gradle (root)

```groovy
repositories {
    ...
    maven { url 'https://jitpack.io' }
}
```

build.gradle (app)

```groovy
dependencies {
    implementation "com.github.mncinnovation.mnc-identifiersdk-android:core:1.0.1"
    implementation "com.github.mncinnovation.mnc-identifiersdk-android:face-detection:1.0.1"
}
```

AndroidManifest.xml

```xml

<application ...>...<meta-data android:name="com.google.mlkit.vision.DEPENDENCIES"
android:value="face" /></application>
```

### How To Use

Start liveness activity

```kotlin
startActivityForResult(MNCIdentifier.getLivenessIntent(this), LIVENESS_DETECTION_REQUEST_CODE)

companion object {
    const val LIVENESS_DETECTION_REQUEST_CODE = xxxx
}
```

Get Liveness Result

```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == RESULT_OK) {
        when (requestCode) {
            LIVENESS_DETECTION_REQUEST_CODE -> {
                //get liveness result
                val livenessResult = MNCIdentifier.getLivenessResult(data)
                livenessResult?.let { result ->
                    if (result.isSuccess) {  // check if liveness detection success
                        // get image result
                        val bitmap = result.getBitmap(this, DetectionMode.SMILE)
                    } else {  //Liveness Detection Error
                        //get Error Message
                        val errorMessage = result.errorMessage
                    }
                }
            }
        }
    }
}
```

### Customize Detection Sequence

Default detection sequence is HOLD_STILL > BLINK > OPEN_MOUTH > SHAKE_HEAD > SMILE. You can cutomize
detection sequence using following method

```kotlin
//the first boolean value indicates if the given detection sequence should be shuffled.
MNCIdentifier.setDetectionModeSequence(
    false, listOf(
        DetectionMode.HOLD_STILL,
        DetectionMode.BLINK,
        DetectionMode.OPEN_MOUTH,
        DetectionMode.SMILE,
        DetectionMode.SHAKE_HEAD
    )
)
  ```

---

## OCR (Optical Character Recognition)

![banner_ocr](/screenshots/banner_ocr.jpeg)


Optical Character Recognition using mlkit text recognition to detect text at the point of capture.

### Requirements

- Min SDK 21

### Setup

build.gradle (root)

```groovy
repositories {
    ...
    maven { url 'https://jitpack.io' }
}
```

build.gradle (app)

```groovy

dependencies{
	implementation "com.github.mncinnovation.mnc-identifiersdk-android:core:1.0.9"
	implementation "com.github.mncinnovation.mnc-identifiersdk-android:ocr:1.0.9"  
}
```

AndroidManifest.xml

```xml

<application ...>
	...
	<meta-data android:name="com.google.mlkit.vision.DEPENDENCIES"
		   android:value="ocr" />
</application>
```

If you use face and ocr, AndroidManifest.xml

```xml

<application ...>
	...
	<meta-data android:name="com.google.mlkit.vision.DEPENDENCIES"
		   android:value="face, ocr" />
</application>
```

### How To Use

Start scan to capture activity

```kotlin

//start directly
MNCIdentifierOCR.startCapture(this@MainActivity)

//withFlash value
MNCIdentifierOCR.startCapture(this@MainActivity, true)

//or withFlash value and also requestCode value
MNCIdentifierOCR.startCapture(this@MainActivity, true, CAPTURE_EKTP_REQUEST_CODE)

companion object {
    const val CAPTURE_EKTP_REQUEST_CODE = xxxx
}
```

Get Capture OCR Result

```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == RESULT_OK) {
        when (requestCode) {
            CAPTURE_EKTP_REQUEST_CODE -> {
                val captureOCRResult = MNCIdentifierOCR.getOCRResult(data)
                captureOCRResult?.let { result ->
                    result.getBitmapImage(this)?.let {
                        //get image result
                        binding.ivKtpCapture.setImageBitmap(it)
                    }
                    //show all of data result
                    binding.tvCaptureKtp.text = result.ktpModel.toString()
                }

            }
        }
    }
}

//another option (using registerForActivityResult) 
private val resultLauncherOcr =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val captureOCRResult = MNCIdentifierOCR.getOCRResult(data)
            captureOCRResult?.let { ocrResult ->
                ocrResult.getBitmapImage(this)?.let {
                    binding.ivKtp.setImageBitmap(it)
                }
                binding.tvScanKtp.text = captureOCRResult.toString()
            }
        }
    }

MNCIdentifierOCR.startCapture(this@MainActivity, resultLauncherOcr, true)
```

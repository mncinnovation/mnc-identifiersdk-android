package id.mncinnovation.ocr.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Ktp(var nik: String? = null,
               var nama: String? = null,
               var tempatLahir: String? = null,
               var tglLahir: String? = null,
               var jenisKelamin: String? = null,
               var alamat: String? = null,
               var rt: String? = null,
               var rw: String? = null,
               var kelurahan: String? = null,
               var kecamatan: String? = null,
               var agama: String? = null,
               var statusPerkawinan: String? = null,
               var pekerjaan: String? = null,
               var kewarganegaraan: String? = null,
               var berlakuHingga: String? = null,
               var provinsi: String? = null,
               var kabKot: String? = null,
               var confidence: Int = 0,
               var rawText: String? = null,
               var bitmap: Bitmap? = null
): Parcelable

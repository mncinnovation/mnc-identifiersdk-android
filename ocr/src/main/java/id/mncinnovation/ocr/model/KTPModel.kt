package id.mncinnovation.ocr.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class KTPModel(
    var nik: String? = null,
    var nama: String? = null,
    var tempatLahir: String? = null,
    var golDarah: String? = null,
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
    var bitmap: Bitmap? = null
) : Parcelable {
    fun toJson(): String {
        val mutableMap = mutableMapOf<String, Any?>()
        mutableMap["nik"] = nik
        mutableMap["nama"] = nama
        mutableMap["tempatLahir"] = tempatLahir
        mutableMap["golDarah"] = golDarah
        mutableMap["tglLahir"] = tglLahir
        mutableMap["jenisKelamin"] = jenisKelamin
        mutableMap["alamat"] = alamat
        mutableMap["rt"] = rt
        mutableMap["rw"] = rw
        mutableMap["kelurahan"] = kelurahan
        mutableMap["kecamatan"] = kecamatan
        mutableMap["agama"] = agama
        mutableMap["statusPerkawinan"] = statusPerkawinan
        mutableMap["pekerjaan"] = pekerjaan
        mutableMap["kewarganegaraan"] = kewarganegaraan
        mutableMap["berlakuHingga"] = berlakuHingga
        mutableMap["provinsi"] = provinsi
        mutableMap["kabKot"] = kabKot
        mutableMap["confidence"] = confidence

        val json = JSONObject(mutableMap)
        return json.toString()
    }
}

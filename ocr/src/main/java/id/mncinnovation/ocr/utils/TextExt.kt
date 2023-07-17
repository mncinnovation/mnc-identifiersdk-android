package id.mncinnovation.ocr.utils

import android.util.Log
import com.google.mlkit.vision.text.Text
import id.mncinnovation.ocr.BuildConfig
import id.mncinnovation.ocr.model.KTPModel
import org.json.JSONObject
import java.text.Normalizer
import java.util.regex.Pattern


fun Text.findAndClean(line: Text.Line, key: String): String? {
    return if (line.elements.size > key.split(" ").size)
        line.text.cleanse(key)
    else
        findInline(line)?.text?.cleanse(key)
}


fun Text.findInline(line: Text.Line): Text.Line? {
    val top = line.boundingBox?.top ?: return null
    val bottom = line.boundingBox?.bottom ?: return null
    val result = mutableListOf<Text.Line>()
    textBlocks.forEach { blok ->
        blok.lines.forEach {
            if (it.boundingBox?.centerY() in top..bottom && it.text != line.text) {
                result.add(it)
            }
        }
    }
    return result.minByOrNull { it.boundingBox?.left ?: 0 }
}


fun Text.extractEktp(): KTPModel {
    val ektp = KTPModel()
    if (BuildConfig.DEBUG) {
        Log.d(TAG_OCR, "rawText : $text")
    }

    val rtrw = REGEX_RT_RW.toRegex().find(text)
    rtrw?.value?.let {
        ektp.confidence++
        ektp.rt = it.split("/").first()
        ektp.rw = it.split("/").last()
    }

    val jk = REGEX_JENIS_KELAMIN.toRegex().find(text)
    jk?.value?.let {
        ektp.confidence++
        ektp.jenisKelamin = it
    }

    var previousLine: Text.Line? = null
    textBlocks.forEach { textBlock ->
        textBlock.lines.forEach { line ->
            when {
                line.text.startsWith("PROVINSI") -> {
                    ektp.confidence++
                    ektp.provinsi = line.text.cleanse("PROVINSI").filterNumberToAlphabet()
                    ektp.provinsi?.let { ektp.confidence++ }
                }

                line.text.startsWith("KOTA") ||
                        line.text.startsWith("KABUPATEN") || line.text.startsWith("JAKARTA") -> {
                    ektp.confidence++
                    if (ektp.kabKot.isNullOrEmpty())
                        ektp.kabKot = line.text.filterNumberToAlphabet()
                }

                line.text.startsWith("NIK") -> {
                    ektp.confidence++
                    ektp.nik = ((if (line.elements.size > 1)
                        line.elements.last().text
                    else
                        filterNik())?.cleanse("NIK"))?.filterAlphabetToNumber()
                }

                line.text.startsWith("Nama", true) -> {
                    ektp.confidence++
                    ektp.nama = findAndClean(line, "Nama")?.filterNumberToAlphabet()
                    ektp.nama?.let { ektp.confidence++ }
                }

                line.text.startsWith("Tempat", true) || line.text.contains("Lahir") -> {
                    ektp.confidence++
                    //tempat lahir allcaps
                    val ttl = REGEX_CAPS.toRegex().findAll(line.text)

                    ektp.tempatLahir = ttl.firstOrNull()?.value?.trim()
                    ektp.tempatLahir?.let { ektp.confidence++ }

                    ektp.tglLahir = ttl.elementAtOrNull(1)?.value?.trim()
                    ektp.tglLahir?.let { ektp.confidence++ }
                }

                line.text.startsWith("Jenis", true) -> {
                    //jenis kelamin allcaps
                    ektp.confidence++
                    ektp.jenisKelamin =
                        jk?.value?.takeIf { it == GENDER_FEMALE || it == GENDER_FEMALE_2 }
                            ?: GENDER_MALE
                    ektp.jenisKelamin?.let { ektp.confidence++ }
                }

                line.text.contains("Gol", true) || line.text.contains(
                    "Darah",
                    true
                ) || line.text.contains("Daah") -> {
                    ektp.golDarah = findAndClean(line, "Gol. Darah")?.cleanse("Gol. Daah")?.cleanse(
                        GENDER_MALE
                    )?.cleanse(GENDER_FEMALE)?.filterBloodGroup()
                    ektp.golDarah?.let { ektp.confidence++ }
                }

                line.text.startsWith("Alamat", true) ||
                        line.text.startsWith("Aiamat", true) -> {
                    ektp.apply {
                        confidence++
                        alamat =
                            findAndClean(line, "Alamat")?.cleanse("Aiamat")
                                ?.cleanse("Gol. Darah ${golDarah ?: ""}")
                        alamat?.let { confidence++ }
                    }
                }

                line.text.contains("RT", true) && line.text.contains(
                    "RW",
                    true
                ) && (ektp.rt == null || ektp.rw == null) -> {
                    ektp.apply {
                        val rtrwLine = findAndClean(line, "RTRW")
                        val rtrwSplit1 = rtrwLine?.split("/")
                        val rtrwSplit2 = rtrwLine?.split(" ")
                        if ((rtrwSplit1?.size ?: 0) > 1) {
                            rt = rtrwSplit1?.first()?.cleanse("RT")?.filterAlphabetToNumber()
                            rw = rtrwSplit1?.last()?.cleanse("RW")?.filterAlphabetToNumber()
                        } else {
                            rt = rtrwSplit2?.first()?.cleanse("RT")?.filterAlphabetToNumber()
                            rw = rtrwSplit2?.last()?.cleanse("RW")?.filterAlphabetToNumber()
                        }
                    }
                }

                line.text.startsWith("Kel", true) || line.text.contains("Desa", true) -> {
                    ektp.apply {
                        confidence++
                        kelurahan = findAndClean(line, "Kel")?.apply {
                            cleanse("Desa")
                            cleanse("/")
                            cleanse("KeV")
                        }?.filterNumberToAlphabet()
                        kelurahan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Kecamatan", true) -> {
                    ektp.apply {
                        confidence++
                        kecamatan = findAndClean(line, "Kecamatan")?.filterNumberToAlphabet()
                        kecamatan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Agama", true) -> {
                    ektp.apply {
                        confidence++
                        agama =
                            findAndClean(line, "Agama")?.filterNumberToAlphabet()?.filterReligion()
                        agama?.let { confidence++ }
                    }
                }

                line.text.startsWith("Status Perkawinan", true) || line.text.contains(
                    "Status Perk",
                    true
                ) -> {
                    ektp.apply {
                        confidence++
                        statusPerkawinan =
                            findAndClean(line, "Status Perkawinan")?.cleanse("Perkainan")
                                ?.filterNumberToAlphabet()
                                ?.filterMaritalStatus()

                        statusPerkawinan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Pekerjaan", true) || line.text.contains("kerjaan", true) -> {
                    ektp.apply {
                        confidence++
                        pekerjaan =
                            findAndClean(line, "Pekerjaan")?.cleanse("ekerjaan")?.cleanse("kerjaan")
                                ?.filterNumberToAlphabet()
                        pekerjaan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Kewarganegaraan", true) ||
                        line.text.startsWith(
                            "Kewarga negaraan",
                            true
                        ) -> {
                    ektp.apply {
                        confidence++
                        kewarganegaraan =
                            findAndClean(line, "Kewarganegaraan")?.cleanse("Kewarga negaraan")
                                ?.cleanse("ewarganegaraan")?.filterNumberToAlphabet()
                                ?.filterCitizenship()
                        kewarganegaraan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Berlaku Hingga", true) ||
                        line.text.startsWith("Beriaku Hingga", true) -> {
                    ektp.apply {
                        confidence++
                        berlakuHingga =
                            findAndClean(line, "Berlaku Hingga")?.cleanse("Beriaku Hingga")

                        berlakuHingga?.let { confidence++ }
                    }
                }

                else -> {
                    previousLine?.let {
                        if (line != null && line.text != "null") {
                            var containLowerCase = false
                            for (c in line.text) {
                                if (c.isLowerCase()) {
                                    containLowerCase = true
                                    break
                                }
                            }
                            Log.e(TAG_OCR, "prevLine> ${it.text}")
                            Log.e(TAG_OCR, "curLine> ${line.text}")

                            if (!containLowerCase) {
                                if (findAndClean(
                                        it,
                                        "Nama"
                                    )?.equals(ektp.nama) == true && ektp.nama != null &&
                                    !line.text.contains("[0-9]".toRegex()) && !line.text.contains("/")
                                    && findAndClean(line, "Nama") != ektp.nama
                                ) {
                                    ektp.apply {
                                        nama += " " + (findAndClean(
                                            line,
                                            "Nama"
                                        ) ?: line.text).cleanse("Nama")
                                    }
                                }

                                if (it.text != "Alamat" && ektp.alamat != null && findAndClean(
                                        it,
                                        "Alamat"
                                    )?.cleanse("Aiamat")
                                        ?.contains(
                                            ektp.alamat ?: ""
                                        ) == true && !line.text.contains("/") &&
                                    !line.text.contains("RT") && !line.text.contains("RW") &&
                                    !line.text.contains("Darah") && findAndClean(
                                        line,
                                        "Alamat"
                                    )?.cleanse("Aiamat") != ektp.alamat
                                ) {
                                    ektp.apply {
                                        alamat = "$alamat ${
                                            (findAndClean(
                                                line,
                                                "Alamat"
                                            ) ?: line.text).cleanse("Alamat", false)
                                                .cleanse("Aiamat", false)
                                        }"
                                    }
                                }
                            }

                        }
                    }
                }
            }
            previousLine = line
        }
    }
    return ektp
}

fun Text.extractKtp() {
    val ktpModel = KTPModel()
    var lastProcessedPosition = 0
    textBlocks.forEach { block ->
        block.lines.forEach { line ->
            val result = "[A-Z0-9-/ ]{3,}+".toRegex().find(line.text)
            if (result != null) {
                when (lastProcessedPosition) {
                    0 -> ktpModel.provinsi = result.value.cleanse("PROVINSI")
                    1 -> ktpModel.kabKot = result.value.cleanse("KOTA")
                    2 -> ktpModel.nik = result.value
                    3 -> ktpModel.nama = result.value
                    4 -> {
                        ktpModel.tempatLahir = result.groupValues.firstOrNull()
                        ktpModel.tglLahir = result.groupValues.elementAtOrNull(1)
                    }
                    5 -> ktpModel.jenisKelamin = result.value
                    6 -> ktpModel.alamat = result.value
                    7 -> {
                        val rtrw = REGEX_RT_RW.toRegex().find(text)
                        rtrw?.value?.let {
                            ktpModel.rt = it.split("/").first()
                            ktpModel.rw = it.split("/").last()
                        }
                        ktpModel.rt = result.value
                    }
                    8 -> ktpModel.kelurahan = result.value
                    9 -> ktpModel.kecamatan = result.value
                    10 -> ktpModel.agama = result.value
                    11 -> ktpModel.statusPerkawinan = result.value
                    12 -> ktpModel.pekerjaan = result.value
                    13 -> ktpModel.kewarganegaraan = result.value
                    14 -> ktpModel.berlakuHingga = result.value
                }
                lastProcessedPosition++
            }
        }

    }
}


fun Text.filterNik(): String? {
    var matchElement: String? = null
    for (i in textBlocks.indices) {
        val blocks = textBlocks[i]
        for (j in blocks.lines.indices) {
            val lines = blocks.lines[j]
            val nik = lines.text.filter {
                it.isDigit() || it == 'O' || it == 'I'
            }
            if (nik.length >= 13) {
                matchElement = nik
                break
            }
            if (matchElement != null) break
        }
        if (matchElement != null) break
    }
    return matchElement
}

fun String?.filterMaritalStatus(): String? {
    val objectJs = JSONObject(JSON_FILTERS)
    val marriageStatus = objectJs.getJSONObject("marriageStatus")
    val kawinArray = marriageStatus.getJSONArray("kawin")
    val belumArray = marriageStatus.getJSONArray("belum")
    val ceraiArray = marriageStatus.getJSONArray("cerai")
    val hidupArray = marriageStatus.getJSONArray("hidup")

    this?.let {
        for (i in 0 until kawinArray.length()) {
            if (it.contains(kawinArray.getString(i), true)) {
                for (j in 0 until belumArray.length()) {
                    if (it.contains(belumArray.getString(j), true)) {
                        return MARITAL_SINGLE
                    }
                }
                return MARITAL_MERRIED
            }
        }
        for (i in 0 until ceraiArray.length()) {
            if (it.contains(ceraiArray.getString(i), true)) {
                for (j in 0 until hidupArray.length()) {
                    if (it.contains(hidupArray.getString(j), true)) {
                        return MARITAL_DIVORCED
                    }
                }
                return MARITAL_DEATH_DIVORCE
            }
        }
    }
    return this
}

fun String?.filterBloodGroup(): String? {
    this?.let {
        return it.replace("8", "B").replace("0", "O").replace("4", "A")
    }
    return this
}

fun String?.filterCitizenship(): String? {
    this?.let {
        if (it.startsWith("WN", false)) {
            return CITIZEN_WNI
        }
    }
    return this
}

fun String?.filterReligion(): String? {
    val objectJs = JSONObject(JSON_FILTERS)
    val religions = objectJs.getJSONObject("religions")
    val islamArray = religions.getJSONArray("islam")
    val kristenArray = religions.getJSONArray("kristen")
    val katholikArray = religions.getJSONArray("katholik")
    val budhaArray = religions.getJSONArray("budha")
    val hinduArray = religions.getJSONArray("hindu")
    val konghuchuArray = religions.getJSONArray("konghuchu")
    val kepercayaanArray = religions.getJSONArray("kepercayaan")

    this?.let {
        for (i in 0 until islamArray.length()) {
            if (it.contains(islamArray.getString(i), true)) {
                return RELIGION_ISLAM
            }
        }
        for (i in 0 until kristenArray.length()) {
            if (it.contains(kristenArray.getString(i), true)) {
                return RELIGION_KRISTEN
            }
        }
        for (i in 0 until hinduArray.length()) {
            if (it.contains(hinduArray.getString(i), true)) {
                return RELIGION_HINDU
            }
        }

        for (i in 0 until budhaArray.length()) {
            if (it.contains(budhaArray.getString(i), true)) {
                return RELIGION_BUDHA
            }
        }

        for (i in 0 until katholikArray.length()) {
            if (it.contains(katholikArray.getString(i), true)) {
                return RELIGION_KATHOLIK
            }
        }

        for (i in 0 until konghuchuArray.length()) {
            if (it.contains(konghuchuArray.getString(i), true)) {
                return RELIGION_KONGHUCHU
            }
        }

        for (i in 0 until kepercayaanArray.length()) {
            if (it.contains(kepercayaanArray.getString(i), true)) {
                if (it.contains("TERH", true) || it.contains("ADA", true) || it.contains(
                        "ADAP",
                        true
                    )
                ) {
                    return RELIGION_KEPERCAYAAN_TERHADAP_TUHAN_YME
                }
                return RELIGION_KEPERCAYAAN
            }
        }

    }
    return this
}


fun String.cleanse(text: String, ignoreCase: Boolean = true): String {
    return replace(text, "", ignoreCase).replace(":", "").trim()
}

fun String.filterNumberToAlphabet(): String {
    return replace("0", "O")
        .replace("1", "I")
        .replace("4", "A")
        .replace("5", "S")
        .replace("7", "T")
        .replace("8", "B")
}

fun String.filterAlphabetToNumber(): String {
    return replace("O", "0")
        .replace("I", "1")
        .replace("A", "4")
        .replace("S", "5")
        .replace("T", "7")
        .replace("B", "8")
}

fun String.removeAccents(): String {
    val normalizedString = Normalizer.normalize(this, Normalizer.Form.NFD)
    val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(normalizedString).replaceAll("")
}

const val CITIZEN_WNI = "WNI"
const val MARITAL_MERRIED = "KAWIN"
const val MARITAL_SINGLE = "BELUM KAWIN"
const val MARITAL_DEATH_DIVORCE = "CERAI MATI"
const val MARITAL_DIVORCED = "CERAI HIDUP"

const val GENDER_MALE = "LAKI-LAKI"
const val GENDER_FEMALE = "PEREMPUAN"
const val GENDER_FEMALE_2 = "WANITA"
const val RELIGION_ISLAM = "ISLAM"
const val RELIGION_KRISTEN = "KRISTEN"
const val RELIGION_HINDU = "HINDU"
const val RELIGION_KATHOLIK = "KATHOLIK"
const val RELIGION_BUDHA = "BUDHA"
const val RELIGION_KONGHUCHU = "KONGHUCHU"
const val RELIGION_KEPERCAYAAN = "KEPERCAYAAN"
const val RELIGION_KEPERCAYAAN_TERHADAP_TUHAN_YME = "KEPERCAYAAN TERHADAP TUHAN YME"
const val TAG_OCR = "OCRLibrary"
const val REGEX_TGL_LAHIR = "\\d\\d-\\d\\d-\\d\\d\\d\\d"
const val REGEX_JENIS_KELAMIN = "LAKI-LAKI|PEREMPUAN|WANITA|PRIA|LAKI|LAKILAKI"
const val REGEX_RT_RW = "\\d\\d\\d\\/\\d\\d\\d"
const val REGEX_CAPS = "[A-Z0-9-/ ]{3,}+"
const val JSON_FILTERS =
    "{\"religions\":{\"islam\":[\"sl\",\"la\",\"am\",\"isl\",\"sla\",\"lam\",\"isla\",\"slam\",\"islam\"],\"kristen\":[\"kr\",\"ri\",\"st\",\"en\",\"kri\",\"ris\",\"ist\",\"ste\",\"ten\",\"kris\",\"rist\",\"iste\",\"sten\",\"krist\",\"riste\",\"isten\",\"kriste\",\"risten\",\"kristen\"],\"katholik\":[\"ka\",\"at\",\"to\",\"ol\",\"lh\",\"hi\",\"ik\",\"kat\",\"ath\",\"tho\",\"tol\",\"oli\",\"lik\",\"kath\",\"atho\",\"thol\",\"holi\",\"olik\",\"katho\",\"athol\",\"tholi\",\"holik\",\"kathol\",\"atholi\",\"tholik\",\"katholi\",\"atholik\",\"katholik\"],\"budha\":[\"bu\",\"ud\",\"dh\",\"ha\",\"bud\",\"udh\",\"dha\",\"budh\",\"udha\",\"budha\"],\"hindu\":[\"hi\",\"in\",\"nd\",\"du\",\"hin\",\"ind\",\"ndu\",\"hind\",\"indu\",\"hindu\"],\"konghuchu\":[\"ko\",\"on\",\"ng\",\"gh\",\"hu\",\"uc\",\"ch\",\"hu\",\"kon\",\"ong\",\"ngh\",\"ghu\",\"huc\",\"uch\",\"chu\",\"kong\",\"ongh\",\"nghu\",\"ghuc\",\"huch\",\"uchu\",\"kongh\",\"onghu\",\"nghuc\",\"ghuch\",\"huchu\",\"konghu\",\"onghuc\",\"nghuch\",\"ghuchu\",\"konghuc\",\"onghuch\",\"nghcuchu\",\"konghuch\",\"onghuchu\",\"konghuchu\"],\"kepercayaan\":[\"ke\",\"ep\",\"pe\",\"er\",\"rc\",\"ca\",\"ay\",\"ya\",\"aa\",\"an\",\"kep\",\"epe\",\"per\",\"erc\",\"rca\",\"cay\",\"aya\",\"yaa\",\"aan\",\"kepe\",\"eper\",\"perc\",\"erca\",\"rcay\",\"caya\",\"ayaa\",\"yaan\",\"keper\",\"eperc\",\"perca\",\"ercay\",\"rcaya\",\"cayaa\",\"ayaan\",\"keperc\",\"eperca\",\"percay\",\"ercaya\",\"rcayaa\",\"cayaan\",\"keperca\",\"epercay\",\"percaya\",\"ercayaa\",\"rcayaan\",\"kepercay\",\"epercaya\",\"percayaa\",\"ercayaan\",\"kepercaya\",\"epercayaa\",\"percayaan\",\"kepercayaa\",\"epercayaan\",\"kepercayaan\"]},\"marriageStatus\":{\"kawin\":[\"ka\",\"aw\",\"wi\",\"in\",\"kaw\",\"awi\",\"win\",\"kawi\",\"awin\",\"kawin\"],\"belum\":[\"be\",\"el\",\"lu\",\"um\",\"bel\",\"elu\",\"lum\",\"belu\",\"elum\",\"belum\"],\"cerai\":[\"ce\",\"er\",\"ra\",\"ai\",\"cer\",\"era\",\"rai\",\"cera\",\"erai\",\"cerai\"],\"hidup\":[\"hi\",\"id\",\"du\",\"up\",\"hid\",\"idu\",\"dup\",\"hidu\",\"idup\",\"hidup\"]},\"numberValidation\":{\"0\":[\"o\",\"O\"],\"1\":[\"L\",\"I\",\"l\",\"i\",\"J\",\"j\"],\"2\":[\"Z\",\"z\"],\"3\":[\"B\"],\"4\":[\"A\"],\"5\":[\"S\",\"s\"],\"6\":[\"b\",\"G\"],\"7\":[\"T\"],\"8\":[\"R\"],\"9\":[\"g\",\"q\"]}}"
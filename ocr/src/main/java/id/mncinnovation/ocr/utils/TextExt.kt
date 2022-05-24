package id.mncinnovation.ocr.utils

import com.google.mlkit.vision.text.Text
import id.mncinnovation.ocr.model.Ktp


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


fun Text.extractEktp(): Ktp {
    val ektp = Ktp()
    ektp.rawText = text

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
                    ektp.provinsi = line.text.cleanse("PROVINSI")
                    ektp.provinsi?.let { ektp.confidence++ }
                }

                line.text.startsWith("KOTA") ||
                        line.text.startsWith("KABUPATEN") || line.text.startsWith("JAKARTA") -> {
                    ektp.confidence++
                    if (ektp.kabKot.isNullOrEmpty())
                        ektp.kabKot = line.text
                }

                line.text.startsWith("NIK") -> {
                    ektp.confidence++
                    ektp.nik = if (line.elements.size > 1)
                        line.elements.last().text
                    else
                        filterNik()
                }

                line.text.startsWith("Nama", true) -> {
                    ektp.confidence++
                    ektp.nama = findAndClean(line, "Nama")
                    ektp.nama?.let { ektp.confidence++ }
                }

                line.text.startsWith("Tempat", true) -> {
                    ektp.confidence++
                    //tempat lahir allcaps
                    val ttl = REGEX_CAPS.toRegex().findAll(line.text)

                    ektp.tempatLahir = ttl.firstOrNull()?.value
                    ektp.tempatLahir?.let { ektp.confidence++ }

                    ektp.tglLahir = ttl.elementAtOrNull(1)?.value
                    ektp.tglLahir?.let { ektp.confidence++ }
                }

                line.text.startsWith("Jenis", true) -> {
                    //jenis kelamin allcaps
                    ektp.confidence++
                    ektp.jenisKelamin = jk?.value?.takeIf { it == "PEREMPUAN" } ?: "LAKI-LAKI"
                    ektp.jenisKelamin?.let { ektp.confidence++ }
                }

                line.text.contains("Gol", true) || line.text.contains(
                    "Darah",
                    true
                ) || line.text.contains("Daah") -> {
                    ektp.golDarah = findAndClean(line, "Gol. Darah")?.cleanse("Gol. Daah")?.cleanse(
                        GENDER_MALE
                    )?.cleanse(GENDER_FEMALE)
                    ektp.golDarah?.let { ektp.confidence++ }
                }

                line.text.startsWith("Alamat", true) ||
                        line.text.startsWith("Aiamat", true) -> {
                    ektp.apply {
                        confidence++
                        alamat = findAndClean(line, "Alamat")?.cleanse("Aiamat")
                        alamat?.let { confidence++ }
                    }
                }

                line.text.startsWith("Kel", true) || line.text.contains("Desa", true) -> {
                    ektp.apply {
                        confidence++
                        kelurahan = findAndClean(line, "Kel")?.apply {
                            cleanse("Desa")
                            cleanse("/")
                            cleanse("KeV")
                        }
                        kelurahan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Kecamatan", true) -> {
                    ektp.apply {
                        confidence++
                        kecamatan = findAndClean(line, "Kecamatan")
                        kecamatan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Agama", true) -> {
                    ektp.apply {
                        confidence++
                        agama = findAndClean(line, "Agama")?.replace("1", "I")?.filterReligion()
                        agama?.let { confidence++ }
                    }
                }

                line.text.startsWith("Status Perkawinan", true) -> {
                    ektp.apply {
                        confidence++
                        statusPerkawinan =
                            findAndClean(line, "Status Perkawinan")?.filterMaritalStatus()

                        statusPerkawinan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Pekerjaan", true) || line.text.contains("kerjaan", true) -> {
                    ektp.apply {
                        confidence++
                        pekerjaan =
                            findAndClean(line, "Pekerjaan")?.cleanse("ekerjaan")?.cleanse("kerjaan")
                        pekerjaan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Kewarganegaraan", true) ||
                        line.text.startsWith("Kewarga negaraan", true) -> {
                    ektp.apply {
                        confidence++
                        kewarganegaraan =
                            findAndClean(line, "Kewarganegaraan")?.cleanse("Kewarga negaraan")
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
                        if (findAndClean(it, "Alamat")?.cleanse("Aiamat")
                                ?.equals(ektp.alamat) == true && ektp.alamat != null
                        ) {
                            ektp.apply {
                                alamat += " " + findAndClean(line, "Alamat")?.cleanse("Aiamat")
                            }
                        }
                        if (findAndClean(it, "Nama")?.equals(ektp.nama) == true && ektp.nama != null
                        ) {
                            ektp.apply {
                                nama += " " + findAndClean(line, "Nama")
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
    val ktp = Ktp()
    var lastProcessedPosition = 0
    textBlocks.forEach { block ->
        block.lines.forEach { line ->
            val result = "[A-Z0-9-/ ]{3,}+".toRegex().find(line.text)
            if (result != null) {
                when (lastProcessedPosition) {
                    0 -> ktp.provinsi = result.value.cleanse("PROVINSI")
                    1 -> ktp.kabKot = result.value.cleanse("KOTA")
                    2 -> ktp.nik = result.value
                    3 -> ktp.nama = result.value
                    4 -> {
                        ktp.tempatLahir = result.groupValues.firstOrNull()
                        ktp.tglLahir = result.groupValues.elementAtOrNull(1)
                    }
                    5 -> ktp.jenisKelamin = result.value
                    6 -> ktp.alamat = result.value
                    7 -> {
                        val rtrw = REGEX_RT_RW.toRegex().find(text)
                        rtrw?.value?.let {
                            ktp.rt = it.split("/").first()
                            ktp.rw = it.split("/").last()
                        }
                        ktp.rt = result.value
                    }
                    8 -> ktp.kelurahan = result.value
                    9 -> ktp.kecamatan = result.value
                    10 -> ktp.agama = result.value
                    11 -> ktp.statusPerkawinan = result.value
                    12 -> ktp.pekerjaan = result.value
                    13 -> ktp.kewarganegaraan = result.value
                    14 -> ktp.berlakuHingga = result.value
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
    this?.let {
        if (it.startsWith("KAW", true)) {
            return MARITAL_MERRIED
        }
        if (it.startsWith("BEL", true)) {
            return MARITAL_SINGLE
        }
        if ((it.contains("MATI", true) || it.contains("ATI")) && it.contains("CER")) {
            return MARITAL_DEATH_DIVORCE
        }
        if ((it.contains("HID", true) || it.contains("DUP")) && it.contains("CER")) {
            return MARITAL_DIVORCED
        }
    }
    return this
}

fun String?.filterCitizenship(): String? {
    this?.let {
        if (it.startsWith("WN")) {
            return CITIZEN_WNI
        }
    }
    return this
}

fun String?.filterReligion(): String? {
    this?.let {
        if ((it.startsWith("I", true) && it.contains("ISL", true)) || it.contains("LAM", true)
        ) {
            return RELIGION_ISLAM
        } else if (it.startsWith("H", true) || it.contains(
                "HIN",
                true
            ) || it.contains("NDU", true)
        ) {
            return RELIGION_HINDU
        } else if (it.startsWith("B") || it.contains(
                "BUD",
                true
            ) || it.contains("DHA", true)
        ) {
            return RELIGION_BUDHA
        } else if (it.startsWith("KR") || it.contains(
                "KRIS",
                true
            ) || it.contains("STEN", true)
        ) {
            return RELIGION_KRISTEN
        } else if (it.startsWith("KA") || it.contains(
                "KAT",
                true
            ) || it.contains("LIK", true) || it.contains("THO", true)
        ) {
            return RELIGION_KATHOLIK
        } else if (it.startsWith("KONG") || (it.contains(
                "HU",
                true
            ) && it.contains("CU", true))
        ) {
            return RELIGION_KONGHUCU
        }
    }
    return this
}


fun String.cleanse(text: String, ignoreCase: Boolean = true): String {
    return replace(text, "", ignoreCase).replace(":", "").trim()
}

const val CITIZEN_WNI = "WNI"
const val MARITAL_MERRIED = "KAWIN"
const val MARITAL_SINGLE = "BELUM KAWIN"
const val MARITAL_DEATH_DIVORCE = "CERAI MATI"
const val MARITAL_DIVORCED = "CERAI HIDUP"

const val GENDER_MALE = "LAKI-LAKI"
const val GENDER_FEMALE = "PEREMPUAN"
const val RELIGION_ISLAM = "ISLAM"
const val RELIGION_KRISTEN = "KRISTEN"
const val RELIGION_HINDU = "HINDU"
const val RELIGION_KATHOLIK = "KATHOLIK"
const val RELIGION_BUDHA = "BUDHA"
const val RELIGION_KONGHUCU = "KONGHUCU"
const val TAG_OCR = "OCRLibrary"
const val REGEX_TGL_LAHIR = "\\d\\d-\\d\\d-\\d\\d\\d\\d"
const val REGEX_JENIS_KELAMIN = "LAKI-LAKI|PEREMPUAN|LAKI"
const val REGEX_RT_RW = "\\d\\d\\d\\/\\d\\d\\d"
const val REGEX_CAPS = "[A-Z0-9-/ ]{3,}+"
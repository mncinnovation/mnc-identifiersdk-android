package id.mncinnovation.ocr.utils

import com.google.mlkit.vision.text.Text
import id.mncinnovation.ocr.model.Ktp


fun Text.findAndClean(line: Text.Line, key: String): String? {
    return if (line.elements.size > key.split(" ").size)
        line.text.cleanse(key)
    else
        findInline(line)?.text?.cleanse(key)
}


fun Text.findInline(line: Text.Line): Text.Line?{
    val top = line.boundingBox?.top?: return null
    val bottom = line.boundingBox?.bottom?: return null
    val result = mutableListOf<Text.Line>()
    textBlocks.forEach { blok ->
        blok.lines.forEach {
            if(it.boundingBox?.centerY() in top..bottom && it.text != line.text){
                result.add(it)
            }
        }
    }
    return result.minByOrNull { it.boundingBox?.left?:0 }
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

    textBlocks.forEach { textBlock ->
        textBlock.lines.forEach { line ->
            when{
                line.text.startsWith("PROVINSI") -> {
                    ektp.confidence++
                    ektp.provinsi = line.text.cleanse("PROVINSI")
                    ektp.provinsi?.let { ektp.confidence++ }
                }

                line.text.startsWith("KOTA") ||
                        line.text.startsWith("KABUPATEN")-> {
                    ektp.confidence++
                    if (ektp.kabKot.isNullOrEmpty())
                        ektp.kabKot = line.text
                }

                line.text.startsWith("NIK") -> {
                    ektp.confidence++
                    ektp.nik = if (line.elements.size>1)
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

                line.text.startsWith("Jenis",true) -> {
                    ektp.confidence++
                    ektp.jenisKelamin = jk?.value?.takeIf { it == "PEREMPUAN" }?: "LAKI-LAKI"
                    ektp.jenisKelamin?.let { ektp.confidence++ }
                }

                line.text.startsWith("Alamat",true) ||
                        line.text.startsWith("Aiamat",true) -> {
                    ektp.apply {
                        confidence++
                        alamat = findAndClean(line,"Alamat")?.cleanse("Aiamat")
                        alamat?.let { confidence++ }
                    }
                }

                line.text.startsWith("Kel",true)|| line.text.contains("Desa",true) -> {
                    ektp.apply {
                        confidence++
                        kelurahan = findAndClean(line,"Kel")?.apply {
                            cleanse("Desa")
                            cleanse("/")
                            cleanse("KeV")
                        }
                        kelurahan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Kecamatan",true) ->{
                    ektp.apply {
                        confidence++
                        kecamatan = findAndClean(line,"Kecamatan")
                        kecamatan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Agama",true) -> {
                    ektp.apply {
                        confidence++
                        agama = findAndClean(line,"Agama")
                        agama?.let { confidence++ }
                    }
                }

                line.text.startsWith("Status Perkawinan",true) -> {
                    ektp.apply {
                        confidence++
                        statusPerkawinan = findAndClean(line,"Status Perkawinan")
                        statusPerkawinan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Pekerjaan",true) -> {
                    ektp.apply {
                        confidence++
                        pekerjaan = findAndClean(line,"Pekerjaan")
                        pekerjaan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Kewarganegaraan",true) ||
                        line.text.startsWith("Kewarga negaraan",true)-> {
                    ektp.apply {
                        confidence++
                        kewarganegaraan = findAndClean(line,"Kewarganegaraan")?.cleanse("Kewarga negaraan")
                        kewarganegaraan?.let { confidence++ }
                    }
                }

                line.text.startsWith("Berlaku Hingga",true) ||
                        line.text.startsWith("Beriaku Hingga",true) -> {
                    ektp.apply {
                        confidence++
                        berlakuHingga = findAndClean(line,"Berlaku Hingga")?.cleanse("Beriaku Hingga")
                        berlakuHingga?.let { confidence++ }
                    }
                }
            }
        }
    }
    return ektp
}

fun Text.extractKtp(){
    val ktp = Ktp()
    var lastProcessedPosition = 0
    textBlocks.forEach { block ->
        block.lines.forEach { line ->
            val result = "[A-Z0-9-/ ]{3,}+".toRegex().find(line.text)
            if (result != null){
                when(lastProcessedPosition){
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


fun Text.filterNik(): String?{
    var matchElement: String? = null
    for (i in textBlocks.indices) {
        val blocks = textBlocks[i]
        for (j in blocks.lines.indices){
            val lines = blocks.lines[j]
            val nik = lines.text.filter {
                it.isDigit() || it == 'O' || it == 'I'
            }
            if(nik.length >= 13) {
                matchElement = nik
                break
            }
            if (matchElement != null) break
        }
        if (matchElement != null) break
    }
    return matchElement
}


fun String.cleanse(text: String, ignoreCase: Boolean = true): String{
    return replace(text,"", ignoreCase).replace(":","").trim()
}

const val REGEX_TGL_LAHIR = "\\d\\d-\\d\\d-\\d\\d\\d\\d"
const val REGEX_JENIS_KELAMIN = "LAKI-LAKI|PEREMPUAN|LAKI"
const val REGEX_RT_RW = "\\d\\d\\d\\/\\d\\d\\d"
const val REGEX_CAPS = "[A-Z0-9-/ ]{3,}+"
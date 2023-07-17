package id.mncinnovation.ocr

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.core.widget.doAfterTextChanged
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.common.toVisibilityOrGone
import id.mncinnovation.ocr.databinding.ActivityConfirmationOcrBinding
import id.mncinnovation.ocr.model.OCRResultModel
import id.mncinnovation.ocr.utils.*
import java.util.*


class ConfirmationOCRActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmationOcrBinding
    private val viewModel = ConfirmationOCRViewModel()
    private val genders = arrayOf(GENDER_MALE, GENDER_FEMALE)
    private val maritalsStatus =
        arrayOf(MARITAL_MERRIED, MARITAL_SINGLE, MARITAL_DIVORCED, MARITAL_DEATH_DIVORCE)
    private val bloodGroups =
        arrayOf("-", "A", "B", "AB", "O", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    private var captureKtpResult: OCRResultModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmationOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        captureKtpResult = MNCIdentifierOCR.getOCRResult(intent)
        with(binding) {
            llConfirmIdentity.visibility = View.GONE

            spGender.adapter = ArrayAdapter<Any?>(
                context,
                R.layout.simple_text_item_black,
                genders
            )
            spMaritalStatus.adapter = ArrayAdapter<Any?>(
                context,
                R.layout.simple_text_item_black,
                maritalsStatus
            )

            spGolDarah.adapter = ArrayAdapter<Any?>(
                context,
                R.layout.simple_text_item_black,
                bloodGroups
            )

            captureKtpResult?.let {
                with(it.ktp) {

                    if (bitmap != null) {
                        ivIdentity.setImageBitmap(bitmap)
                    } else {
                        ivIdentity.setImageURI(it.imagePath?.toUri())
                    }

                    etNik.setText(nik)
                    checkNIK(nik ?: "")
                    etFullname.setText(nama?.removeAccents())
                    etBornPlace.setText(tempatLahir?.removeAccents())
                    etBirthdate.setText(tglLahir)
                    if (jenisKelamin == GENDER_MALE) {
                        spGender.setSelection(0)
                    } else if (jenisKelamin == GENDER_FEMALE || jenisKelamin == GENDER_FEMALE_2) {
                        spGender.setSelection(1)
                    }
                    when (golDarah) {
                        "A" -> spGolDarah.setSelection(1)
                        "B" -> spGolDarah.setSelection(2)
                        "AB" -> spGolDarah.setSelection(3)
                        "O" -> spGolDarah.setSelection(4)
                        "A+" -> spGolDarah.setSelection(5)
                        "A-" -> spGolDarah.setSelection(6)
                        "B+" -> spGolDarah.setSelection(7)
                        "B-" -> spGolDarah.setSelection(8)
                        "AB+" -> spGolDarah.setSelection(9)
                        "AB-" -> spGolDarah.setSelection(10)
                        "O+" -> spGolDarah.setSelection(11)
                        "O-" -> spGolDarah.setSelection(12)
                        else -> spGolDarah.setSelection(0)
                    }
                    etAddress.setText(alamat?.removeAccents())
                    etRt.setText(rt)
                    etRw.setText(rw)
                    etVillage.setText(kelurahan?.removeAccents())
                    etDistrict.setText(kecamatan?.removeAccents())
                    etCity.setText(kabKot?.removeAccents())
                    etProvince.setText(provinsi?.removeAccents())
                    etReligion.setText(agama)

                    spMaritalStatus.setSelection(
                        when (statusPerkawinan) {
                            MARITAL_SINGLE -> 1
                            MARITAL_DIVORCED -> 2
                            MARITAL_DEATH_DIVORCE -> 3
                            else -> 0
                        }
                    )

                    etJob.setText(pekerjaan?.removeAccents())
                    etCitizenship.setText(kewarganegaraan?.removeAccents())
                    etExpiredDate.setText(berlakuHingga)
                    viewModel.checkValues(this)
                }
            }

            etNik.doAfterTextChanged {
                checkNIK(it.toString())
                captureKtpResult?.ktp?.apply {
                    nik = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etFullname.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    nama = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etBornPlace.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    tempatLahir = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etBirthdate.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    tglLahir = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etAddress.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    alamat = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etRt.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    rt = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etRw.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    rw = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etProvince.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    provinsi = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etCity.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    kabKot = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etVillage.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    kelurahan = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etDistrict.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    kecamatan = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etReligion.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    agama = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etJob.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    pekerjaan = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etCitizenship.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    kewarganegaraan = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etExpiredDate.doAfterTextChanged {
                captureKtpResult?.ktp?.apply {
                    berlakuHingga = it.toString()
                    viewModel.checkValues(this)
                }
            }

            etBirthdate.setOnClickListener {
                showDatePickerAction(initYear = 1990, maxDate = Date().time) { day, month, year ->
                    val monthTxt = if (month < 10) "0$month" else month.toString()
                    val dayTxt = if (day < 10) "0$day" else day.toString()
                    val dateTxt = "$dayTxt-$monthTxt-$year"
                    etBirthdate.setText(dateTxt)
                }.show()
            }

            val backListener = View.OnClickListener { onBackPressed() }
            ivBack.setOnClickListener(backListener)
            tvBack.setOnClickListener(backListener)

            btnNext.setOnClickListener {
                if (viewModel.state.value == StateConfirm.FILL_STATE) {
                    viewModel.updateState()
                    scrollviewContent.post { scrollviewContent.fullScroll(ScrollView.FOCUS_UP) }
                } else {
                    captureKtpResult?.ktp?.apply {
                        nik = etNik.text.toString()
                        nama = etFullname.text.toString().trim()
                        tempatLahir = etBornPlace.text.toString().trim()
                        tglLahir = etBirthdate.text.toString().trim()
                        jenisKelamin =
                            if (spGender.selectedItemPosition == 0) GENDER_MALE else GENDER_FEMALE
                        golDarah = when (spGolDarah.selectedItemPosition) {
                            1 -> "A"
                            2 -> "B"
                            3 -> "AB"
                            4 -> "O"
                            5 -> "A+"
                            6 -> "A-"
                            7 -> "B+"
                            8 -> "B-"
                            9 -> "AB+"
                            10 -> "AB-"
                            11 -> "O+"
                            12 -> "O-"
                            else -> "-"
                        }
                        alamat = etAddress.text.toString().trim()
                        rt = etRt.text.toString().trim()
                        rw = etRw.text.toString().trim()
                        kelurahan = etVillage.text.toString().trim()
                        kecamatan = etDistrict.text.toString().trim()
                        kabKot = etCity.text.toString().trim()
                        provinsi = etProvince.text.toString().trim()
                        agama = etReligion.text.toString().trim()
                        statusPerkawinan =
                            when (spMaritalStatus.selectedItemPosition) {
                                1 -> MARITAL_SINGLE
                                2 -> MARITAL_DIVORCED
                                3 -> MARITAL_DEATH_DIVORCE
                                else -> MARITAL_MERRIED
                            }
                        pekerjaan = etJob.text.toString().trim()
                        kewarganegaraan = etCitizenship.text.toString().trim()
                        berlakuHingga = etExpiredDate.text.toString().trim()
                    }

                    setResult(RESULT_OK, Intent().apply {
                        putExtra(EXTRA_RESULT, captureKtpResult)
                    })
                    finish()
                }
            }

            viewModel.isShouldEnableBtnNext.observe(this@ConfirmationOCRActivity) { enable ->
                btnNext.isEnabled = enable
                btnNext.alpha = if (enable) 1.0f else 0.3f
            }

            viewModel.state.observe(this@ConfirmationOCRActivity) { state ->
                setStateUpdate(state)
            }
        }
    }

    private fun checkNIK(value: String) {
        binding.tilNik.error =
            if (value.length < 16 || !value.isDigitsOnly()) "NIK harus 16 digit" else null
    }

    override fun onBackPressed() {
        if (viewModel.state.value == StateConfirm.FILL_STATE) {
            super.onBackPressed()
        } else {
            viewModel.updateState()
        }
    }

    private fun setStateUpdate(state: StateConfirm) {
        val drawableEditField = ContextCompat.getDrawable(
            context,
            R.drawable.ic_edit
        )
        val drawableArrowDownField = ContextCompat.getDrawable(
            context,
            R.drawable.ic_baseline_keyboard_arrow_down_24
        )

        val isConfirmState = state == StateConfirm.CONFIRM_STATE

        val drawableEdit: Drawable? = if (isConfirmState) null else drawableEditField
        val drawableArrowDown: Drawable? = if (isConfirmState) null else drawableArrowDownField

        val bgField: Drawable? = ContextCompat.getDrawable(
            context,
            if (isConfirmState) R.drawable.bg_edittext_readonly else R.drawable.bg_white_corner_radius_solid
        )

        val bgColorField = ContextCompat.getColor(
            context,
            if (isConfirmState) R.color.bg_disable else android.R.color.white
        )

        with(binding) {
            llConfirmIdentity.visibility = isConfirmState.toVisibilityOrGone()
            btnNext.text = if (isConfirmState) "Konfirmasi ulang" else "Lanjutkan"
            etNik.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etFullname.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etBornPlace.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null

                )
            }
            etBirthdate.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            spGender.isEnabled = !isConfirmState
            rlGender.background = bgField
            spGender.background = bgField

            ivDropdownGender.setImageDrawable(drawableArrowDown)

            spGolDarah.isEnabled = !isConfirmState
            rlGolDarah.background = bgField
            spGolDarah.background = bgField
            ivDropdownGolDarah.setImageDrawable(drawableArrowDown)

            etAddress.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null

                )
            }
            etRt.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etRw.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etVillage.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etDistrict.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etCity.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etProvince.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }

            etReligion.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }

            spMaritalStatus.isEnabled = !isConfirmState
            rlMaritalStatus.background = bgField
            spMaritalStatus.background = bgField

            ivDropdownMaritalStatus.setImageDrawable(drawableArrowDown)

            etJob.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etCitizenship.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etExpiredDate.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
        }
    }

    val context: Context
        get() = this@ConfirmationOCRActivity
}
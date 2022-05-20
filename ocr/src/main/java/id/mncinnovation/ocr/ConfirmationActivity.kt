package id.mncinnovation.ocr

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.common.toVisibilityOrGone
import id.mncinnovation.ocr.databinding.ActivityConfirmationBinding
import id.mncinnovation.ocr.utils.*
import java.util.*


class ConfirmationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmationBinding
    private var state = StateConfirm.FILL_STATE
    private val genders = arrayOf(GENDER_MALE, GENDER_FEMALE)
    private val maritalsStatus =
        arrayOf(MARITAL_MERRIED, MARITAL_SINGLE, MARITAL_DIVORCED, MARITAL_DEATH_DIVORCE)
    private val bloodGroups = arrayOf("-", "A", "B", "AB", "O")
    private val religions = arrayOf(
        RELIGION_ISLAM,
        RELIGION_KRISTEN,
        RELIGION_KATOLIK,
        RELIGION_HINDU,
        RELIGION_BUDHA,
        RELIGION_KONGHUCU
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val captureKtpResult = MNCIdentifierOCR.getCaptureKtpResult(intent)
        with(binding) {
            llConfirmIdentity.visibility = View.GONE

            spGender.adapter = ArrayAdapter<Any?>(
                context,
                android.R.layout.simple_list_item_1,
                genders
            )
            spReligion.adapter = ArrayAdapter<Any?>(
                context,
                android.R.layout.simple_list_item_1,
                religions
            )
            spMaritalStatus.adapter = ArrayAdapter<Any?>(
                context,
                android.R.layout.simple_list_item_1,
                maritalsStatus
            )

            spGolDarah.adapter = ArrayAdapter<Any?>(
                context,
                android.R.layout.simple_list_item_1,
                bloodGroups
            )
            captureKtpResult?.let {
                with(it.ktp) {
                    Log.e("TAGConfirm", "data : ${it.ktp.golDarah}")

                    if (bitmap != null) {
                        ivIdentity.setImageBitmap(bitmap)
                    } else {
                        ivIdentity.setImageURI(it.imageUri)
                    }

                    etNik.setText(nik)
                    etFullname.setText(nama)
                    etBornPlace.setText(tempatLahir)
                    etBirthdate.setText(tglLahir)
                    if (jenisKelamin == GENDER_MALE) {
                        spGender.setSelection(0)
                    } else if (jenisKelamin == GENDER_FEMALE) {
                        spGender.setSelection(1)
                    }
                    when (golDarah) {
                        "A" -> spGolDarah.setSelection(1)
                        "B" -> spGolDarah.setSelection(2)
                        "AB" -> spGolDarah.setSelection(3)
                        "O" -> spGolDarah.setSelection(4)
                        else -> spGolDarah.setSelection(0)
                    }
                    etAddress.setText(alamat)
                    etRt.setText(rt)
                    etRw.setText(rw)
                    etVillage.setText(kelurahan)
                    etDistrict.setText(kecamatan)
                    etCity.setText(kabKot)
                    etProvince.setText(provinsi)
                    when (agama) {
                        RELIGION_KRISTEN -> spReligion.setSelection(1)
                        RELIGION_KATOLIK -> spReligion.setSelection(2)
                        RELIGION_HINDU -> spReligion.setSelection(3)
                        RELIGION_BUDHA -> spReligion.setSelection(4)
                        RELIGION_KONGHUCU -> spReligion.setSelection(5)
                        else -> spReligion.setSelection(0)
                    }

                    spMaritalStatus.setSelection(
                        when (statusPerkawinan) {
                            MARITAL_SINGLE -> 1
                            MARITAL_DIVORCED -> 2
                            MARITAL_DEATH_DIVORCE -> 3
                            else -> 0
                        }
                    )

                    etJob.setText(pekerjaan)
                    etCitizenship.setText(kewarganegaraan)
                    etExpiredDate.setText(berlakuHingga)
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

            ivBack.setOnClickListener {
                onBackPressed()
            }

            btnNext.setOnClickListener {
                if (state == StateConfirm.FILL_STATE) {
                    setStateUpdate(StateConfirm.CONFIRM_STATE)
                    scrollviewContent.post { scrollviewContent.fullScroll(ScrollView.FOCUS_UP) }
                } else {
                    captureKtpResult?.ktp?.apply {
                        nik = etNik.text.toString()
                        nama = etFullname.text.toString()
                        tempatLahir = etBornPlace.text.toString()
                        tglLahir = etBirthdate.text.toString()
                        jenisKelamin =
                            if (spGender.selectedItemPosition == 0) GENDER_MALE else GENDER_FEMALE
                        golDarah = when (spGolDarah.selectedItemPosition) {
                            1 -> "B"
                            2 -> "AB"
                            3 -> "O"
                            else -> "A"
                        }
                        alamat = etAddress.text.toString()
                        rt = etRt.text.toString()
                        rw = etRw.text.toString()
                        kelurahan = etVillage.text.toString()
                        kecamatan = etDistrict.text.toString()
                        kabKot = etCity.text.toString()
                        provinsi = etProvince.text.toString()
                        agama = when (spReligion.selectedItemPosition) {
                            1 -> RELIGION_KRISTEN
                            2 -> RELIGION_KATOLIK
                            3 -> RELIGION_HINDU
                            4 -> RELIGION_BUDHA
                            5 -> RELIGION_KONGHUCU
                            else -> RELIGION_ISLAM
                        }
                        statusPerkawinan =
                            when (spMaritalStatus.selectedItemPosition) {
                                1 -> MARITAL_SINGLE
                                2 -> MARITAL_DIVORCED
                                3 -> MARITAL_DEATH_DIVORCE
                                else -> MARITAL_MERRIED
                            }
                        pekerjaan = etJob.text.toString()
                        kewarganegaraan = etCitizenship.text.toString()
                        berlakuHingga = etExpiredDate.text.toString()
                    }

                    setResult(RESULT_OK, Intent().apply {
                        putExtra(EXTRA_RESULT, captureKtpResult)
                    })
                    finish()
                }

            }
        }
    }

    override fun onBackPressed() {
        if (state == StateConfirm.FILL_STATE) {
            super.onBackPressed()
        } else {
            setStateUpdate(StateConfirm.FILL_STATE)
        }
    }

    private fun setStateUpdate(state: StateConfirm) {
        this.state = state
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
            ivDropdownGender.setImageDrawable(drawableArrowDown)

            spGolDarah.isEnabled = !isConfirmState
            rlGolDarah.background = bgField
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
            spReligion.isEnabled = !isConfirmState
            rlReligion.background = bgField
            ivDropdownReligion.setImageDrawable(drawableArrowDown)

            spMaritalStatus.isEnabled = !isConfirmState
            rlMaritalStatus.background = bgField
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
        get() = this@ConfirmationActivity
}

enum class StateConfirm {
    FILL_STATE,
    CONFIRM_STATE
}
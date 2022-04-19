package id.mncinnovation.ocr

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import id.mncinnovation.identification.core.common.EXTRA_RESULT
import id.mncinnovation.identification.core.common.toVisibilityOrGone
import id.mncinnovation.ocr.databinding.ActivityConfirmationBinding
import id.mncinnovation.ocr.utils.GENDER_FEMALE
import id.mncinnovation.ocr.utils.GENDER_MALE
import id.mncinnovation.ocr.utils.showDatePickerAction
import java.util.*


class ConfirmationActivity : AppCompatActivity() {
    lateinit var binding: ActivityConfirmationBinding
    private var state = FILL_STATE
    private val genders = arrayOf(GENDER_MALE, GENDER_FEMALE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val captureKtpResult = MNCIdentifierOCR.getCaptureKtpResult(intent)

        with(binding) {
            llConfirmIdentity.visibility = View.GONE
            captureKtpResult?.let {
                with(it.ktp) {

                    if (bitmap != null) {
                        ivIdentity.setImageBitmap(bitmap)
                    } else {
                        ivIdentity.setImageURI(it.imageUri)
                    }

                    etNik.setText(nik)
                    etFullname.setText(nama)
                    etBornPlace.setText(tempatLahir)
                    etBirthdate.setText(tglLahir)
                    etGender.setText(jenisKelamin)
                    etAddress.setText(alamat)
                    etRt.setText(rt)
                    etRw.setText(rw)
                    etVillage.setText(kelurahan)
                    etDistrict.setText(kecamatan)
                    etCity.setText(kabKot)
                    etProvince.setText(provinsi)
                    etReligion.setText(agama)
                    etMaritalStatus.setText(statusPerkawinan)
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
            val arrayAdapter: ArrayAdapter<*> =
                ArrayAdapter<Any?>(
                    context,
                    android.R.layout.simple_list_item_1,
                    genders
                )
            etGender.setAdapter(arrayAdapter)
            etGender.onFocusChangeListener =
                OnFocusChangeListener { v, hasFocus -> if (hasFocus) etGender.showDropDown() }

            ivBack.setOnClickListener {
                onBackPressed()
            }

            btnNext.setOnClickListener {
                if (state == FILL_STATE) {
                    setStateUpdate(CONFIRM_STATE)
                } else {
                    captureKtpResult?.ktp?.apply {
                        nik = etNik.text.toString()
                        nama = etFullname.text.toString()
                        tempatLahir = etBornPlace.text.toString()
                        tglLahir = etBirthdate.text.toString()
                        jenisKelamin = etGender.text.toString()
                        alamat = etAddress.text.toString()
                        rt = etRt.text.toString()
                        rw = etRw.text.toString()
                        kelurahan = etVillage.text.toString()
                        kecamatan = etDistrict.text.toString()
                        kabKot = etCity.text.toString()
                        provinsi = etProvince.text.toString()
                        agama = etReligion.text.toString()
                        statusPerkawinan = etMaritalStatus.text.toString()
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
        if (state == FILL_STATE) {
            super.onBackPressed()
        } else {
            setStateUpdate(FILL_STATE)
        }
    }

    private fun setStateUpdate(state: Int) {
        this.state = state
        val drawableEditField = ContextCompat.getDrawable(
            context,
            R.drawable.ic_edit
        )
        val drawableArrowDownField = ContextCompat.getDrawable(
            context,
            R.drawable.ic_baseline_keyboard_arrow_down_24
        )

        val isConfirmState = state == CONFIRM_STATE

        val drawableEdit: Drawable? = if (isConfirmState) null else drawableEditField
        val drawableArrowDown: Drawable? = if (isConfirmState) null else drawableArrowDownField

        val bgField: Drawable? = ContextCompat.getDrawable(
            context,
            if (isConfirmState) R.drawable.bg_edittext_readonly else R.drawable.bg_edittext_solid
        )

        with(binding) {
            llConfirmIdentity.visibility = isConfirmState.toVisibilityOrGone()
            btnNext.text = if (isConfirmState) "Konfirmasi ulang" else "Lanjutkan"
            etNik.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etFullname.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etBornPlace.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null

                )
            }
            etBirthdate.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etGender.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableArrowDown,
                    null
                )
            }
            etAddress.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null

                )
            }
            etRt.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etRw.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etVillage.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etDistrict.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etCity.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etProvince.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etReligion.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etMaritalStatus.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etJob.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etCitizenship.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
                    null,
                    null,
                    drawableEdit,
                    null
                )
            }
            etExpiredDate.apply {
                isEnabled = !isConfirmState
                background = bgField
                setCompoundDrawables(
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

    companion object {
        const val FILL_STATE = 0
        const val CONFIRM_STATE = 1
    }
}
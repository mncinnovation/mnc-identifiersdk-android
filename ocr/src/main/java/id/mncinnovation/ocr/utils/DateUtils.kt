package id.mncinnovation.ocr.utils

import android.app.DatePickerDialog
import android.content.Context
import androidx.annotation.StyleRes
import java.util.*

fun Context.showDatePickerAction(
    @StyleRes style: Int? = null,
    initYear: Int? = null,
    maxDate: Long? = null,
    actionListener: (day: Int, month: Int, year: Int) -> Unit
): DatePickerDialog = this.let {
    val currentTime = Calendar.getInstance()
    val yearInit = initYear ?: currentTime.get(Calendar.YEAR)
    val monthInit = currentTime.get(Calendar.MONTH)
    val dayInit = currentTime.get(Calendar.DAY_OF_MONTH)

    val dialog = if (style != null) DatePickerDialog(
        this, style, { _, year, month, dayOfMonth ->
            actionListener(dayOfMonth, month + 1, year)
        }, yearInit, monthInit, dayInit
    ) else DatePickerDialog(
        this, { _, year, month, dayOfMonth ->
            actionListener(dayOfMonth, month + 1, year)
        }, yearInit, monthInit, dayInit
    )
    maxDate?.let {
        dialog.datePicker.maxDate = it

    }
    return dialog
}
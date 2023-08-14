package id.mncinnovation.identification.core.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import id.mncinnovation.identification.core.R

fun Context.showDismissableErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    isShowBtnContinue: Boolean = false,
    onContinue: (() -> Unit)? = null
) {
    try {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_error, null)
        val errorMessageTextView = dialogView.findViewById<TextView>(R.id.errorMessageTextView)
        val dismissButton = dialogView.findViewById<Button>(R.id.dismissButton)
        val continueButton = dialogView.findViewById<Button>(R.id.continueButton)

        errorMessageTextView.text = message

        val errorDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        if(isShowBtnContinue && onContinue != null) {
            continueButton.setOnClickListener {
                onContinue()
                errorDialog.dismiss()
            }
        } else {
            continueButton.visibility = View.GONE
        }

        dismissButton.setOnClickListener {
            onDismiss()
            errorDialog.dismiss()
        }

        errorDialog.show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
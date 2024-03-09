package com.jan.psywikikt.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface.OnDismissListener
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.CalendarView
import com.google.android.material.button.MaterialButton
import com.jan.psywikikt.R
import java.util.Calendar
import java.util.Date

class CalendarDialog(
    context: Activity,
) {
    private val context: Activity
    private val dialog: Dialog

    var date: Date? = null

    init {
        this.context = context
        this.dialog = Dialog(context)
    }

    fun build() {
        dialog.setContentView(R.layout.dialog_calendar)
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnOk = dialog.findViewById<MaterialButton>(R.id.btnOk)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancel)
        val calendar = dialog.findViewById<CalendarView>(R.id.calendar)
        var tempDate: Date? = null

        val cal = Calendar.getInstance()
        calendar.maxDate = cal.timeInMillis

        calendar.setOnDateChangeListener { view, year, month, dayOfMonth ->
            // Temporary variable "tempDate" which stores the date from setOnDateChangeListener.
            // The "date" variable should be only set when btnOk is clicked.
            cal.set(year, month, dayOfMonth)
            tempDate = Date.from(cal.toInstant())
        }

        btnOk.setOnClickListener {
            this.date = if(tempDate != null) tempDate!! else Date.from(Calendar.getInstance().toInstant())

            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    fun setOnDismissListener(onDismiss: OnDismissListener) {
        dialog.setOnDismissListener(onDismiss)
    }

    fun show() {
        dialog.show()
    }
}
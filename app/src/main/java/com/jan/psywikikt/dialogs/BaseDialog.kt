package com.jan.psywikikt.dialogs

import android.app.Activity
import android.content.DialogInterface
import androidx.core.text.HtmlCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class BaseDialog(
    context: Activity,
    message: String,
    htmlFormat: Boolean = true,
    onPositive: ((DialogInterface, Int) -> Unit)? = null,
    onNegative: ((DialogInterface, Int) -> Unit)? = null,
    title: String,
) {
    private val context: Activity
    private var message: String
    private val htmlFormat: Boolean
    private var onPositive: ((DialogInterface, Int) -> Unit)?
    private var onNegative: ((DialogInterface, Int) -> Unit)?
    private var title: String
    private val dialog: MaterialAlertDialogBuilder

    init {
        this.context = context
        this.message = message
        this.htmlFormat = htmlFormat
        this.onPositive = onPositive
        this.onNegative = onNegative
        this.dialog = MaterialAlertDialogBuilder(context)
        this.title = title
    }

    fun show() {
        this.dialog
            .setTitle(HtmlCompat.fromHtml(this.title, HtmlCompat.FROM_HTML_MODE_COMPACT))
            .setPositiveButton("Ok", onPositive)

        if(onNegative != null) this.dialog.setNegativeButton("Cancel", onNegative)

        if(htmlFormat) {
            context.runOnUiThread {
                dialog.setMessage(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_COMPACT)).show()
            }
        } else {
            context.runOnUiThread {
                dialog.setMessage(message).show()
            }
        }
    }

    fun getDialog(): MaterialAlertDialogBuilder {
        return dialog
    }

    fun setTitle(title: String) {
        this.title = "<h1>$title</h1>"
    }
}
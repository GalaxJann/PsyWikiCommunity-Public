package com.jan.psywikikt.dialogs

import android.app.Activity
import android.content.DialogInterface

open class ErrorDialog(
    context: Activity,
    message: String = "An unknown error has occured. Please send a screenshot to the Developer<br><br>Discord: asumyt",
    htmlFormat: Boolean = true,
    onPositive: ((DialogInterface, Int) -> Unit)? = null,
    title: String = "<h1>Error</h1>",
) : BaseDialog(context, message, htmlFormat, onPositive, title = title) {
    private val context: Activity
    private val message: String
    private val htmlFormat: Boolean
    private val onPositive: ((DialogInterface, Int) -> Unit)?
    private val title: String

    init {
        this.context = context
        this.message = message
        this.htmlFormat = htmlFormat
        this.onPositive = onPositive
        this.title = title
    }
}
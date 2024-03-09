package com.jan.psywikikt.dialogs

import android.app.Activity

class HTMLErrorDialog(
    httpCode: Int,
    context: Activity,
) : ErrorDialog(context, "PsychonautWiki-API is currently not reachable. Please try again later!<br><br>HTTP-Code: ") {
    private val httpCode: Int
    private val context: Activity

    init {
        this.httpCode = httpCode
        this.context = context
    }
}
package com.jan.psywikikt.views

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import com.jan.psywikikt.R

class DeleteButton(
    context: Activity
) : IconButton(context, R.drawable.ic_trashbin, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
    gravity = Gravity.CENTER_VERTICAL
    leftMargin = 4
}) {
    private val context: Activity

    init {
        this.context = context
    }
}
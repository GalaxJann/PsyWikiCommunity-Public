package com.jan.psywikikt.views

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import com.jan.psywikikt.R

class EditButton(
    context: Activity
) : IconButton(context, R.drawable.baseline_edit_24, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
    gravity = Gravity.CENTER_VERTICAL
    rightMargin = 4
    width = 150
}) {
    private val context: Activity

    init {
        this.context = context
    }
}
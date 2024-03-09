package com.jan.psywikikt.views

import android.app.Activity
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

open class IconButton(
    context: Activity,
    icon: Int,
    layoutParams: LinearLayout.LayoutParams
) {
    private val activity: Activity
    private val icon: Int
    private val layoutParams: LinearLayout.LayoutParams

    private val button = MaterialButton(context)

    init {
        this.activity = context
        this.icon = icon
        this.layoutParams = layoutParams

        button.apply {
            insetTop = 0
            insetBottom = 0
            iconPadding = 0
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        }
        button.icon = (ContextCompat.getDrawable(context, icon))
        button.layoutParams = layoutParams
    }

    fun setOnClick(onClick: (() -> Unit)) {
        button.setOnClickListener { onClick() }
    }

    fun getButton(): MaterialButton { return button }
}
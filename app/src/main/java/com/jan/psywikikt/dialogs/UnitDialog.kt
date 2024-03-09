package com.jan.psywikikt.dialogs

import android.app.Activity
import android.app.Dialog
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.jan.psywikikt.R
import com.jan.psywikikt.data.DrugUnit
import com.jan.psywikikt.data.DrugUnitDao
import kotlinx.coroutines.launch

class UnitDialog(
    context: Activity,
    lifecycleScope: LifecycleCoroutineScope,
    drugUnitDao: DrugUnitDao,
    updateUnitList: (() -> Unit)? = null
) {
    private val context: Activity
    private val lifecycleScope: LifecycleCoroutineScope
    private val drugUnitDao: DrugUnitDao
    private val dialog: Dialog
    private val updateUnitList: (() -> Unit)?

    private lateinit var dialogTitle: MaterialTextView
    private lateinit var unitText: TextInputEditText
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnOk: MaterialButton

    init {
        this.context = context
        this.lifecycleScope = lifecycleScope
        this.drugUnitDao = drugUnitDao
        this.dialog = Dialog(context)
        this.updateUnitList = updateUnitList
    }

    fun build() {
        dialog.setContentView(R.layout.dialog_unit)
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        this.dialogTitle = dialog.findViewById(R.id.dialogTitle)
        this.unitText = dialog.findViewById(R.id.unitText)
        this.btnCancel = dialog.findViewById(R.id.btnCancel)
        this.btnOk = dialog.findViewById(R.id.btnOk)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnOk.setOnClickListener { addUnitEntry(unitText.text.toString()) }
    }

    fun show() {
        dialog.show()
    }

    // Private
    private fun addUnitEntry(unit: String) {
        lifecycleScope.launch {
            try {
                drugUnitDao.addUnit(DrugUnit(0, unit, false))

                BaseDialog(
                    context,
                    "Entry added!<br><br>Unit: ${unit}",
                    title = "Unit Editor"
                )
                if(updateUnitList != null) updateUnitList!!() // Null assertion mustn't be deleted even it's redundant
            } catch(e: SQLiteConstraintException) {
                ErrorDialog(context, "This unit already exists. Please use another name").show()
            }
        }

        dialog.dismiss()
    }
}
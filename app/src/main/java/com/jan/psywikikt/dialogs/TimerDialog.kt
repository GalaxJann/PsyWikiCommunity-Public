package com.jan.psywikikt.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TimePicker
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.jan.psywikikt.LogActivity
import com.jan.psywikikt.R
import com.jan.psywikikt.data.DrugIndex
import com.jan.psywikikt.data.DrugIndexDao
import com.jan.psywikikt.data.DrugLog
import com.jan.psywikikt.data.DrugLogDao
import com.jan.psywikikt.data.DrugTimer
import com.jan.psywikikt.data.DrugTimerDao
import com.jan.psywikikt.data.DrugUnit
import com.jan.psywikikt.data.DrugUnitDao
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

class TimerDialog(
    activity: Activity,
    lifecycleScope: LifecycleCoroutineScope,
    drugTimerDao: DrugTimerDao,
    drugUnitDao: DrugUnitDao,
    drugName: String = "",
    selectedDate: LocalDate? = null,
    drugLogDao: DrugLogDao? = null,
    drugIndexDao: DrugIndexDao? = null
) {
    private val activity: Activity
    private val lifecycleScope: LifecycleCoroutineScope
    private val drugTimerDao: DrugTimerDao
    private val drugUnitDao: DrugUnitDao
    private val drugName: String
    private val selectedDate: LocalDate?
    private val drugLogDao: DrugLogDao?
    private val drugIndexDao: DrugIndexDao?
    private val dialog: Dialog

    private var selected: Boolean

    private lateinit var linearDialog: LinearLayout
    private lateinit var dialogTitle: MaterialTextView
    private lateinit var btnOk: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var substanceAutocomplete: AutoCompleteTextView
    private lateinit var dosageCheckBox: MaterialCheckBox
    private lateinit var notesCheckBox: MaterialCheckBox
    private lateinit var layoutDose: LinearLayout
    private lateinit var inputDose: TextInputEditText
    private lateinit var dropdownUnit: AutoCompleteTextView
    private lateinit var inputLayoutNotes: TextInputLayout
    private lateinit var inputNotes: TextInputEditText
    private lateinit var timePicker: TimePicker

    init {
        this.activity = activity
        this.lifecycleScope = lifecycleScope
        this.drugTimerDao = drugTimerDao
        this.drugUnitDao = drugUnitDao
        this.drugName = drugName
        this.selectedDate = selectedDate
        this.drugLogDao = drugLogDao
        this.drugIndexDao = drugIndexDao
        this.dialog = Dialog(activity)
        this.selected = false
    }

    @SuppressLint("SetTextI18n")
    fun build(setTimerViews: (() -> Unit)? = null): TimerDialog {
        dialog.setContentView(R.layout.dialog_timer)
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        this.linearDialog = dialog.findViewById(R.id.linearDialog)
        this.dialogTitle = dialog.findViewById(R.id.dialogTitle)
        this.btnOk = dialog.findViewById(R.id.btnOk)
        this.btnCancel = dialog.findViewById(R.id.btnCancel)
        this.substanceAutocomplete = dialog.findViewById(R.id.substanceAutocomplete)
        this.dosageCheckBox = dialog.findViewById(R.id.dosageCheckBox)
        this.notesCheckBox = dialog.findViewById(R.id.notesCheckBox)
        this.layoutDose = dialog.findViewById(R.id.layoutDose)
        this.inputDose = dialog.findViewById(R.id.inputDose)
        this.dropdownUnit = dialog.findViewById(R.id.dropdownUnit)
        this.inputLayoutNotes = dialog.findViewById(R.id.inputLayoutNotes)
        this.inputNotes = dialog.findViewById(R.id.inputNotes)
        this.timePicker = dialog.findViewById(R.id.timePicker)

        timePicker.setIs24HourView(true)
        if(this.drugName.isNotEmpty()) {
            substanceAutocomplete.setText(this.drugName)
        }


        // Substance SQLite-Search init
        substanceAutocomplete.doAfterTextChanged { text ->
            Log.d("Message", "Substance Search: ${text.toString()}")

            this.selected = false
            getDrugList(text.toString())
        }

        // Dismiss dropdown if
        substanceAutocomplete.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            Log.d("Item clicked", id.toString())

            this.selected = true
            substanceAutocomplete.clearFocus()
        }

        // Dropdown Init
        val units = mutableListOf<String>()
        for(unit in drugUnitDao.getAllUnits()) {
            units.add(unit.unit)
        }
        val unitAdapter: ArrayAdapter<String> = ArrayAdapter(activity, androidx.appcompat.R.layout.select_dialog_item_material, units)
        val defaultUnit: DrugUnit? = drugUnitDao.getDefaultUnit()
        dropdownUnit.apply {
            threshold = 1
            setAdapter(unitAdapter)
            setText(defaultUnit?.unit ?: "", false) // If defaultUnit not null
        }

        // LogActivity - Add date to Dialog
        if(selectedDate != null) {
            val dateText = MaterialTextView(activity)
            dateText.apply {
                typeface = ResourcesCompat.getFont(context, R.font.advent_pro_regular)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.main_card_subtitle_size))
                text = selectedDate.format(DateTimeFormatter.ofPattern("dd. MMMM yyyy"))
            }
            linearDialog.addView(dateText, 1)
        }

        // Checkbox handling for optional fields
        dosageCheckBox.setOnCheckedChangeListener { compoundButton, isChecked ->
            if(isChecked) {
                layoutDose.visibility = LinearLayout.VISIBLE
            } else {
                layoutDose.visibility = LinearLayout.GONE
                inputDose.setText("")
            }
        }

        notesCheckBox.setOnCheckedChangeListener { compoundButton, isChecked ->
            if(isChecked) {
                inputLayoutNotes.visibility = TextInputLayout.VISIBLE
            } else {
                inputLayoutNotes.visibility = TextInputLayout.GONE
                inputNotes.setText("")
            }
        }

        // ADD button
        btnOk.setOnClickListener {
            val localDate: LocalDateTime
            if(selectedDate != null) {
                localDate = selectedDate.atStartOfDay().withHour(timePicker.hour).withMinute(timePicker.minute).withSecond(0)
            } else {
                localDate = LocalDateTime.now().withHour(timePicker.hour).withMinute(timePicker.minute).withSecond(0)
            }
            val date = Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant())

            if(substanceAutocomplete.text!!.toString() == "") {
                ErrorDialog(activity, "<b>Substande name</b> is empty!").show()
                return@setOnClickListener
            }

            // Set nullable DB-fields to null if empty
            val (daoDose, daoUnit, daoNotes) = makeDaoData(inputDose, dropdownUnit, inputNotes)

            // If only one field is null
            if(dosageCheckBox.isChecked && ((daoDose == null && daoUnit != null) || (daoDose != null && daoUnit == null))) {
                ErrorDialog(activity, "Please fill both <b>Dose</b> and <b>Unit</b>, or none of them").show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                if(selectedDate != null && drugLogDao != null) {
                    // Add entry in drugLog table
                    drugLogDao.addLog(
                        DrugLog(
                            0,
                            substanceAutocomplete.text.toString(),
                            daoDose,
                            daoUnit,
                            daoNotes,
                            date
                        )
                    )

                    activity.finishAndRemoveTask()
                    val intent = Intent(activity, LogActivity::class.java)
                    // Pass date to restarted activity to jump back to this
                    intent.putExtra("date", selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    startActivity(activity, intent, null)
                    return@launch
                } else {
                    // Add entry to drugTimer table
                    drugTimerDao.addTimer(
                        DrugTimer(
                            0,
                            substanceAutocomplete.text.toString(),
                            daoDose,
                            daoUnit,
                            daoNotes,
                            date
                        )
                    )
                }
            }

            ErrorDialog(
                activity,
                "Entry added!<br><br>Substance: ${substanceAutocomplete.text}<br>Dosage: ${inputDose.text}${dropdownUnit.text}<br>Timestamp: $date<br>Notes: ${inputNotes.text}",
                title = "Timer - WIP"
            ).show()
            dialog.dismiss()
            // Run setTimerFields() if passed as parameter
            if(setTimerViews != null) {
                setTimerViews()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        return this
    }

    @SuppressLint("SetTextI18n")
    fun setEditMode(i: DrugTimer, setTimerViews: (() -> Unit)) {
        val time: LocalDateTime = i.timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

        dialogTitle.text = "Modify Timer"
        btnOk.text = "Save"

        // Prefilling
        if(i.dose != null && i.unit != null) {
            dosageCheckBox.isChecked = true
            inputDose.setText(i.dose.toString())
            dropdownUnit.setText(i.unit, false)
        }
        if(i.notes != null) {
            notesCheckBox.isChecked = true
            inputNotes.setText(i.notes)
        }

        timePicker.hour = time.hour
        timePicker.minute = time.minute

        // Override common btnOk listener
        btnOk.setOnClickListener {
            if(substanceAutocomplete.text.toString() == "") {
                ErrorDialog(activity, "<b>Substance name</b> is empty!").show()
                return@setOnClickListener
            }

            val (daoDose, daoUnit, daoNotes) = makeDaoData(inputDose, dropdownUnit, inputNotes)

            // If only one field is null
            if(dosageCheckBox.isChecked && ((daoDose == null && daoUnit != null) || (daoDose != null && daoUnit == null))) {
                ErrorDialog(activity, "Please fill both <b>Dose</b> and <b>Unit</b>, or none of them").show()
                return@setOnClickListener
            }

            val newTime: LocalDateTime = time.withHour(timePicker.hour).withMinute(timePicker.minute)
            val newDate = Date.from(newTime.atZone(ZoneId.systemDefault()).toInstant())
            lifecycleScope.launch { drugTimerDao.updateTimer(i.id, substanceAutocomplete.text.toString(), daoDose, daoUnit, daoNotes, newDate) }

            BaseDialog(
                activity,
                "Entry updated!<br><br>Substance: ${substanceAutocomplete.text}<br>Dosage: ${inputDose.text}${dropdownUnit.text}<br>Timestamp: $newDate<br>Notes: ${inputNotes.text}",
                title = "Timer - WIP"
            )
            dialog.dismiss()
            // Run setTimerFields() if passed as parameter
            setTimerViews()
        }
    }

    fun show() {
        dialog.show()
    }

    // Getter - Setter
    fun getBtnOk(): MaterialButton { return this.btnOk }
    fun getBtnCancel(): MaterialButton { return this.btnCancel }

    // Private
    private data class DaoData(val daoDose: Double?, val daoUnit: String?, val daoNotes: String?)

    private fun makeDaoData(inputDose: TextInputEditText, inputUnit: AutoCompleteTextView, inputNotes: TextInputEditText): DaoData {
        return DaoData(
            if(inputDose.text.isNullOrEmpty()) null else inputDose.text.toString().toDouble(),
            if(dropdownUnit.text.isNullOrEmpty()) null else inputUnit.text.toString(),
            if(inputNotes.text.isNullOrEmpty()) null else inputNotes.text.toString()
        )
    }

    // SQLite search query to search a substance
    private fun getDrugList(query: String) {
        if(drugIndexDao == null) {
            ErrorDialog(activity, "The DrugIndexDao is not provided in parameters!").show()
            return
        }

        val drugNames: MutableList<String> = mutableListOf()

        lifecycleScope.launch {
            val sqlResults: List<DrugIndex> = drugIndexDao.getSearchDrugs("%$query%", 5)
            for(drug in sqlResults) {
                drugNames.add(drug.name)
            }

            // Dropdown Init
            val drugNamesAdapter: ArrayAdapter<String> = ArrayAdapter(activity, androidx.appcompat.R.layout.select_dialog_item_material, drugNames)
            substanceAutocomplete.apply {
                threshold = 1
                setAdapter(drugNamesAdapter)
            }

            // Show drop down manually
            if(substanceAutocomplete.isPopupShowing && substanceAutocomplete.text.isEmpty()) {
                substanceAutocomplete.dismissDropDown()
            } else if(!substanceAutocomplete.isPopupShowing && !selected) {
                substanceAutocomplete.showDropDown()
            }
        }
    }
}
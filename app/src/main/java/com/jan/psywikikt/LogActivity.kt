package com.jan.psywikikt

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.setMargins
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textview.MaterialTextView
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.jan.psywikikt.data.DrugIndexDao
import com.jan.psywikikt.data.DrugLogDao
import com.jan.psywikikt.data.DrugTimerDao
import com.jan.psywikikt.data.DrugUnitDao
import com.jan.psywikikt.data.MainDatabase
import com.jan.psywikikt.databinding.ActivityLogBinding
import com.jan.psywikikt.databinding.CalendarDayLayoutBinding
import com.jan.psywikikt.dialogs.TimerDialog
import com.jan.psywikikt.views.DeleteButton
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

class LogActivity : AppCompatActivity() {
    // SQLite Database
    private lateinit var drugLogDao: DrugLogDao
    private lateinit var drugTimerDao: DrugTimerDao
    private lateinit var drugIndexDao: DrugIndexDao
    private lateinit var drugUnitDao: DrugUnitDao

    private lateinit var binding: ActivityLogBinding

    // CalendarView
    private var selectedDate: LocalDate? = LocalDate.now()

    // Views below CalendarView
    private lateinit var linearLogs: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.subtitle = "Calendar Logs"

        linearLogs = binding.linearLogs

        val calendar: CalendarView = binding.calendar

        // Database
        val db = MainDatabase.getInstance(this)
        drugLogDao = db.drugLogDao()
        drugTimerDao = db.drugTimerDao()
        drugIndexDao = db.drugIndexDao()
        drugUnitDao = db.drugUnitDao()

        // If date is passed from past activity
        if(intent.extras != null && intent.extras!!.containsKey("date")) {
            selectedDate = LocalDate.parse(intent.extras!!.getString("date"), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        }

        // Make log entries by timestamp
        var logsByTimestamp: JsonObject = buildLogJSON()
        if(logsByTimestamp.get(selectedDate.toString()) == null) {
            linearLogs.removeAllViews()
            setNoEntries(linearLogs)
        } else {
            val logEntries: JsonArray = logsByTimestamp.get(selectedDate.toString()).asJsonArray

            setLogsViews(binding.linearLogs, calendar, logEntries)
        }

        // FAB
        binding.addTimestamp.setOnClickListener {
            // Custom TimerDialog which passes selected day
            val timerDialog = TimerDialog(this, lifecycleScope, drugTimerDao, drugUnitDao, selectedDate = selectedDate, drugLogDao = drugLogDao, drugIndexDao = drugIndexDao)
            timerDialog.build().show()
        }

        // Calendar classes - use here to make variables available
        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView = CalendarDayLayoutBinding.bind(view).calendarDayText
            // Will be set when this container is bound
            lateinit var day: CalendarDay

            init {
                view.setOnClickListener {
                    // Date selection
                    if(day.position == DayPosition.MonthDate) {
                        val currentSelection = selectedDate
                        if(currentSelection != day.date) {
                            selectedDate = day.date
                            calendar.notifyDateChanged(day.date)
                            if(currentSelection != null) {
                                calendar.notifyDateChanged(currentSelection)
                            }
                            if(logsByTimestamp.has(selectedDate.toString())) {
                                logsByTimestamp = buildLogJSON()
                                if(logsByTimestamp.get(selectedDate.toString()) == null) {
                                    linearLogs.removeAllViews()
                                    return@setOnClickListener
                                }
                                val logEntries: JsonArray = logsByTimestamp.get(selectedDate.toString()).asJsonArray
                                setLogsViews(binding.linearLogs, calendar, logEntries)
                            } else {
                                binding.linearLogs.removeAllViews()
                                setNoEntries(linearLogs)
                            }
                        }
                    }
                }
            }
        }

        class MonthViewContainer(view: View) : ViewContainer(view) {
            val titlesContainer = view as TextView
        }

        val titlesContainer = findViewById<ViewGroup>(R.id.titlesContainer)
        calendar.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.textView.text = data.date.dayOfMonth.toString()
                container.day = data

                if(data.position == DayPosition.MonthDate) {
                    // Date selection
                    if(data.date == selectedDate) {
                        container.textView.setBackgroundResource(R.drawable.cal_day_selected_background)
                    } else {
                        container.textView.setBackgroundColor(Color.TRANSPARENT)
                    }
                } else {
                    // Make days gray out of selected month
                    container.textView.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(this@LogActivity, R.color.calendar_days_outofmonth)))
                }

                if(logsByTimestamp.has(data.date.toString())) {
                    if(data.date == selectedDate) {
                        container.textView.setBackgroundResource(R.drawable.cal_day_selected_background)
                    } else {
                        container.textView.setBackgroundResource(R.drawable.cal_day_available_background)
                    }
                }
            }
        }
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(12)
        val endMonth = currentMonth.plusMonths(12)
        val daysOfWeek = daysOfWeek()
        calendar.setup(startMonth, endMonth, daysOfWeek.first())
        calendar.scrollToMonth(currentMonth)
        // Mo - Fr text
        titlesContainer.children
            .map { it as TextView }
            .forEachIndexed { index, textView ->
                val dayOfWeek = daysOfWeek[index]
                val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                textView.text = title
            }
        // Month header title
        calendar.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                container.titlesContainer.text = data.yearMonth.format(DateTimeFormatter.ofPattern("MMMM - yyyy", Locale.ENGLISH))
            }
        }

        // If selected date was changed through intent.extras (see at the beginning of this class)
        if(selectedDate != LocalDate.now()) {
            calendar.scrollToMonth(YearMonth.of(selectedDate!!.year, selectedDate!!.month))
        }
    } @SuppressLint("SetTextI18n")
    private fun setLogsViews(linearLogs: LinearLayout, calendar: CalendarView, logEntries: JsonArray) {
        // Remove all views
        linearLogs.removeAllViews()

        // Create log views
        for(i in logEntries) {
            val entry = i.asJsonObject

            val cardView = CardView(this)
            val linearLog = LinearLayout(this)
            val linearVerticalLog = LinearLayout(this)
            val materialTextView = MaterialTextView(this)
            val materialSubTextView = MaterialTextView(this)
            val deleteButton = DeleteButton(this)
            val cardViewMargin = resources.getDimension(R.dimen.main_card_margin).toInt()
            val cardViewParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(cardViewMargin)
            }
            val materialTextViewParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                weight = 1.0f
                gravity = Gravity.TOP
            }
            cardView.apply {
                background = ContextCompat.getDrawable(applicationContext, R.drawable.main_card_background)
                layoutParams = cardViewParams
            }
            linearLog.apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = cardViewParams
            }
            linearVerticalLog.apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { weight = 1.0f }
            }
            materialTextView.apply {
                typeface = ResourcesCompat.getFont(context, R.font.advent_pro_bold)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.main_card_title_size))
                layoutParams = materialTextViewParams
                text = entry.get("name").asString
            }
            // Null handling and date formatting
            var dose = ""
            var unit = ""
            var notes = ""
            if(!entry.get("dose").isJsonNull && !entry.get("unit").isJsonNull) {
                dose = "Dose: ${entry.get("dose").asDouble}"
                unit = "${entry.get("unit").asString}\n"
            }
            if(!entry.get("notes").isJsonNull) {
                notes = "\nNotes: ${entry.get("notes").asString}"
            }
            val timestamp: LocalDateTime = LocalDateTime.ofInstant(Date(entry.get("timestamp").asLong).toInstant(), ZoneId.systemDefault())
            materialSubTextView.apply {
                typeface = ResourcesCompat.getFont(context, R.font.advent_pro_regular)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.main_card_text_size))
                layoutParams = materialTextViewParams
                text = "$dose${unit}Time: ${timestamp.format(DateTimeFormatter.ofPattern("HH:mm"))}$notes"
            }
            deleteButton.setOnClick {
                lifecycleScope.launch {
                    drugLogDao.removeLogById(entry.get("id").asInt)
                    val logsByTimestamp: JsonObject = buildLogJSON()
                    if(logsByTimestamp.get(selectedDate.toString()) == null) {
                        linearLogs.removeAllViews()
                        setNoEntries(linearLogs)
                        return@launch
                    }
                    val logEntries = logsByTimestamp.get(selectedDate.toString()).asJsonArray

                    setLogsViews(linearLogs, calendar, logEntries)
                }
            }

            linearLogs.addView(cardView)
            cardView.addView(linearLog)
            linearLog.addView(linearVerticalLog)
            linearVerticalLog.addView(materialTextView)
            linearVerticalLog.addView(materialSubTextView)
            linearLog.addView(deleteButton.getButton())
        }
    } private fun buildLogJSON() : JsonObject {
        val logs = drugLogDao.getAllLogs()
        val logsByTimestamp = JsonObject()
        for(log in logs) {
            val logEntry = JsonObject()
            logEntry.addProperty("id", log.id)
            logEntry.addProperty("name", log.name)
            logEntry.addProperty("dose", log.dose)
            logEntry.addProperty("unit", log.unit)
            logEntry.addProperty("notes", log.notes)
            logEntry.addProperty("timestamp", log.timestamp.time)

            val timestamp: LocalDateTime = LocalDateTime.ofInstant(log.timestamp.toInstant(), ZoneId.systemDefault())
            val timestampString = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            if(logsByTimestamp.has(timestampString)) {
                val entriesArray = logsByTimestamp.get(timestampString).asJsonArray
                entriesArray.add(logEntry)
            } else {
                val entriesArray = JsonArray()
                entriesArray.add(logEntry)
                logsByTimestamp.add(timestampString, entriesArray)
            }
        }

        return logsByTimestamp
    } @SuppressLint("SetTextI18n")
    private fun setNoEntries(linearLogs: LinearLayout) {
        val materialTextView = MaterialTextView(this@LogActivity)
        val materialTextViewParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            weight = 1.0f
            gravity = Gravity.TOP
        }
        materialTextViewParams.setMargins(64, 64, 64, 64)
        materialTextView.apply {
            typeface = ResourcesCompat.getFont(context, R.font.advent_pro_bold)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.main_card_title_size))
            layoutParams = materialTextViewParams
            text = "No entries"
        }
        linearLogs.addView(materialTextView)
    }
}


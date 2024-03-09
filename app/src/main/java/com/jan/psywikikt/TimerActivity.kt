package com.jan.psywikikt

import android.annotation.SuppressLint
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup.LayoutParams
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setMargins
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.textview.MaterialTextView
import com.jan.psywikikt.data.DrugIndexDao
import com.jan.psywikikt.data.DrugLog
import com.jan.psywikikt.data.DrugLogDao
import com.jan.psywikikt.data.DrugTimer
import com.jan.psywikikt.data.DrugTimerDao
import com.jan.psywikikt.data.DrugUnitDao
import com.jan.psywikikt.data.MainDatabase
import com.jan.psywikikt.databinding.ActivityTimerBinding
import com.jan.psywikikt.dialogs.BaseDialog
import com.jan.psywikikt.dialogs.ErrorDialog
import com.jan.psywikikt.dialogs.TimerDialog
import com.jan.psywikikt.views.DeleteButton
import com.jan.psywikikt.views.EditButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class TimerActivity : AppCompatActivity() {
    // SQLite
    private lateinit var drugTimerDao: DrugTimerDao
    private lateinit var drugLogDao: DrugLogDao
    private lateinit var drugUnitDao: DrugUnitDao
    private lateinit var drugIndexDao: DrugIndexDao

    private lateinit var binding: ActivityTimerBinding

    private lateinit var linearTimers: LinearLayout
    private lateinit var materialTextList: MutableList<MaterialTextView>
    private lateinit var materialSubTextList: MutableList<MaterialTextView>

    private lateinit var fadeIn: Animation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SQLite
        val db = MainDatabase.getInstance(this)
        drugTimerDao = db.drugTimerDao()
        drugLogDao = db.drugLogDao()
        drugUnitDao = db.drugUnitDao()
        drugIndexDao = db.drugIndexDao()

        // Add Timers to layout
        linearTimers = binding.linearTimers
        materialTextList = mutableListOf()
        materialSubTextList = mutableListOf()

        // FadeIn Animation
        fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.duration = 1000

        // Clock Update Task - Fill text
        setTimersViews(linearTimers, materialTextList, materialSubTextList)
        startTask(materialTextList, materialSubTextList)

        supportActionBar!!.subtitle = "Timer - WIP"

        // FAB
        binding.addTimestamp.setOnClickListener {
            // Custom TimerDialog
            val timerDialog = TimerDialog(this, lifecycleScope, drugTimerDao, drugUnitDao, drugIndexDao = drugIndexDao)
            timerDialog.build{ setTimersViews(linearTimers, materialTextList, materialSubTextList) }.show()
        }
    } override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.timer_menu, menu)
        return super.onCreateOptionsMenu(menu)
    } override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.itemReset -> {
                lifecycleScope.launch {
                    drugTimerDao.removeAllTimers()
                    setTimersViews(linearTimers, materialTextList, materialSubTextList)
                }
            } R.id.itemDrugLog -> {
                val intent = Intent(this, LogActivity::class.java)
                startActivity(intent)
            } R.id.itemMoveLogs -> {
                val dialog = BaseDialog(this, "Do you want to move your entries to <i>Calendar Logs</i>?", onPositive = { _, _ -> run {
                    val drugTimer: List<DrugTimer> = drugTimerDao.getAllTimers()
                    try {
                        lifecycleScope.launch {
                            for(i in drugTimer) {
                                drugLogDao.addLog(DrugLog(
                                    0,
                                    i.name,
                                    i.dose,
                                    i.unit,
                                    i.notes,
                                    i.timestamp
                                ))
                            }
                            drugTimerDao.removeAllTimers()
                            setTimersViews(linearTimers, materialTextList, materialSubTextList)
                        }
                        BaseDialog(this@TimerActivity, "Successfully moved timers to <i>Calendar Logs</i>", title = "Move Logs - WIP").show()
                    } catch (e: SQLiteConstraintException) {
                        ErrorDialog(this@TimerActivity, "SQLite Error").show()
                    }
                }}, title = "Move Logs - WIP")
                dialog.getDialog().setNegativeButton("Cancel", null)
                dialog.show()
            } R.id.itemStatistics -> {
                val intent = Intent(this, StatisticsActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SetTextI18n")
    private fun startTask(materialTextList: MutableList<MaterialTextView>, materialSubTextList: MutableList<MaterialTextView>) {
        val lifecycle = this

        // Unlimited async loop - Delay 1000
        lifecycle.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val timers: List<DrugTimer> = drugTimerDao.getAllTimers()

                    // Recreate clock values every second
                    var timestamp: LocalDateTime
                    var materialTextViewCount = 0
                    for(i in timers) {
                        timestamp = i.timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        val minute: String
                        if(timestamp.minute <= 9) {
                            minute = "0${timestamp.minute}"
                        } else {
                            minute = timestamp.minute.toString()
                        }
                        val hoursDiff: Long = ChronoUnit.HOURS.between(timestamp, LocalDateTime.now())
                        val minutesDiff: Long = ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now()) - (hoursDiff * 60)

                        // Null handling
                        var dose = ""
                        var unit = ""
                        val notes = if(i.notes != null) "\nNotes: ${i.notes}" else ""
                        if(i.unit != null && i.dose != null) {
                            dose = "Dose: ${i.dose}"
                            unit = "${i.unit}\n"
                        }

                        materialTextList[materialTextViewCount].text = i.name
                        materialSubTextList[materialTextViewCount].text = "Duration: ${hoursDiff}h ${minutesDiff}m\n${dose}${unit}Time: ${timestamp.hour}:$minute$notes"

                        materialTextViewCount++
                    }

                    delay(1000)
                }
            }
        }
    }

    private fun setTimersViews(linearTimers: LinearLayout, materialTextList: MutableList<MaterialTextView>, materialSubTextList: MutableList<MaterialTextView>) {
        val timers: List<DrugTimer> = drugTimerDao.getAllTimers()

        // Remove all timer views
        linearTimers.removeAllViews()
        materialTextList.clear()
        materialSubTextList.clear()

        // Create views
        for(i in timers) {
            val cardView = CardView(this)
            val linearTimer = LinearLayout(this)
            val linearVerticalTimer = LinearLayout(this)
            val materialTextView = MaterialTextView(this)
            val materialSubTextView = MaterialTextView(this)
            val editButton = EditButton(this)
            val deleteButton = DeleteButton(this)
            val cardViewMargin = resources.getDimension(R.dimen.main_card_margin).toInt()
            val cardViewParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(cardViewMargin)
            }
            val materialTextViewParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                weight = 1.0f
                gravity = Gravity.TOP
            }
            materialTextList.add(materialTextView)
            materialSubTextList.add(materialSubTextView)
            cardView.apply {
                background = ContextCompat.getDrawable(applicationContext, R.drawable.main_card_background)
                layoutParams = cardViewParams
                animation = fadeIn
            }
            linearTimer.apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = cardViewParams
            }
            linearVerticalTimer.apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { weight = 1.0f }
            }
            materialTextView.apply {
                typeface = ResourcesCompat.getFont(context, R.font.advent_pro_bold)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.main_card_title_size))
                layoutParams = materialTextViewParams
            }
            materialSubTextView.apply {
                typeface = ResourcesCompat.getFont(context, R.font.advent_pro_regular)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.main_card_text_size))
                layoutParams = materialTextViewParams
            }
            editButton.setOnClick {
                // Custom TimerDialog (Edit Mode)
                val time: LocalDate = i.timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                val timerDialog = TimerDialog(this, lifecycleScope, drugTimerDao, drugUnitDao, drugIndexDao = drugIndexDao, drugName = i.name, selectedDate = time)

                timerDialog.build()
                timerDialog.setEditMode(i, { setTimersViews(linearTimers, materialTextList, materialSubTextList) })
                timerDialog.show()
            }
            deleteButton.setOnClick {
                lifecycleScope.launch {
                    drugTimerDao.removeTimer(i)
                    setTimersViews(linearTimers, materialTextList, materialSubTextList)
                }
            }

            linearTimers.addView(cardView)
            cardView.addView(linearTimer)
            linearTimer.addView(linearVerticalTimer)
            linearVerticalTimer.addView(materialTextView)
            linearVerticalTimer.addView(materialSubTextView)
            linearTimer.addView(editButton.getButton())
            linearTimer.addView(deleteButton.getButton())
        }
    }
}
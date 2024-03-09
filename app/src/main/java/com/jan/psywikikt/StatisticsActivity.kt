 package com.jan.psywikikt

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.jan.psywikikt.data.DrugLog
import com.jan.psywikikt.data.DrugLogDao
import com.jan.psywikikt.data.DrugTimerDao
import com.jan.psywikikt.data.MainDatabase
import com.jan.psywikikt.databinding.ActivityStatisticsBinding
import com.jan.psywikikt.dialogs.BaseDialog
import com.jan.psywikikt.dialogs.CalendarDialog
import com.jan.psywikikt.dialogs.ErrorDialog
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

class StatisticsActivity : AppCompatActivity(), OnChartValueSelectedListener {
    // SQLite Database
    private lateinit var drugTimerDao: DrugTimerDao
    private lateinit var drugLogDao: DrugLogDao

    private lateinit var binding: ActivityStatisticsBinding

    // Date list --> for onValueSelected listener
    private val chartDates: MutableList<LocalDate> = mutableListOf()

    class ValFormatter(keyList: List<String>, chartDates: MutableList<LocalDate>) : ValueFormatter() {

        private val dates: ArrayList<String> = arrayListOf()
        init {
            for(key in keyList) {
                val shortDate = LocalDate.parse(key, DateTimeFormatter.ofPattern("dd-MM-yyyy")).format(DateTimeFormatter.ofPattern("dd-MM"))
                dates.add(shortDate)
                chartDates.add(LocalDate.parse(key, DateTimeFormatter.ofPattern("dd-MM-yyyy")))
            }
        }
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return dates.getOrNull(value.toInt()) ?: value.toString()
            //return super.getAxisLabel(value, axis)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SQLite
        val db = MainDatabase.getInstance(this)
        drugTimerDao = db.drugTimerDao()
        drugLogDao = db.drugLogDao()

        supportActionBar!!.subtitle = "Stats - Date Range - WIP"

        var startDate: Date? = null
        var endDate: Date? = null

        binding.buttonStartDate.setOnClickListener {
            val calendarDialog = CalendarDialog(this)
            calendarDialog.build()
            calendarDialog.setOnDismissListener {
                if (calendarDialog.date != null) {
                    startDate = calendarDialog.date!!
                    binding.textStartDate.text = "Start: ${startDate!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}"
                }
            }
            calendarDialog.show()
        }

        binding.buttonEndDate.setOnClickListener {
            val calendarDialog = CalendarDialog(this)
            calendarDialog.build()
            calendarDialog.setOnDismissListener {
                if (calendarDialog.date != null) {
                    endDate = calendarDialog.date!!
                    binding.textEndDate.text = "End: ${endDate!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}"
                }
            }
            calendarDialog.show()
        }

        binding.buttonRender.setOnClickListener {
            if(startDate == null || endDate == null) {
                ErrorDialog(this, "Error: You must select a start and end date!").show()
                return@setOnClickListener
            }

            val logList: List<DrugLog> = drugLogDao.getLogsDateRange(startDate!!.time, endDate!!.time)
            chartDates.clear()

            val map = mutableMapOf<String, Int>()    // Date and count
            for(log in logList) {
                val dateStr: String = log.timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))

                if(map.containsKey(dateStr)) {
                    // Entry exist
                    map.replace(dateStr, map.getValue(dateStr) + 1)
                } else {
                    // Entry doesn't exist, create one
                    map[dateStr] = 1
                }
            }

            Log.d("DateMap", map.toString())

            drawChart(map)
        }
    }

    private fun drawChart(dateMap: MutableMap<String, Int>) {
        // MPAndroidChart
        val data: MutableList<Entry> = mutableListOf()
        val reversedList = mutableListOf<String>()
        for(entry in dateMap) {
            reversedList.add(entry.key)
        }
        reversedList.reverse()
        var counter = 0
        for(key in reversedList) {
            val value: String = dateMap[key].toString()
            data.add(Entry(counter.toFloat(), value.toFloat()))
            counter += 1
        }
        val ld1 = LineDataSet(data, "Consume count per day")
        val textColor: Int = this.getColor(R.color.stat_text)

        Log.e("Data", data.toString())

        ld1.color = this.getColor(R.color.colorPrimary)
        ld1.setDrawValues(true)
        ld1.setDrawFilled(true)
        ld1.lineWidth = 3f
        ld1.fillColor = this.getColor(R.color.purple_700)
        ld1.fillAlpha = 85

        binding.mpChart.xAxis.labelRotationAngle = 0f

        binding.mpChart.axisRight.isEnabled = false
        binding.mpChart.setTouchEnabled(true)
        binding.mpChart.setPinchZoom(true)
        binding.mpChart.description.text = "Days"
        binding.mpChart.setNoDataText("No data yet!")
        binding.mpChart.xAxis.granularity = 1f
        binding.mpChart.xAxis.valueFormatter = ValFormatter(reversedList, chartDates)

        // Fit chart for dark theme
        binding.mpChart.data = LineData(ld1)
        binding.mpChart.data.setValueTextColor(textColor)
        binding.mpChart.xAxis.textColor = textColor
        binding.mpChart.axisRight.textColor = textColor
        binding.mpChart.axisLeft.textColor = textColor
        binding.mpChart.legend.textColor = textColor

        binding.mpChart.setOnChartValueSelectedListener(this)

        binding.mpChart.invalidate()
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        // val entryDatett: String = binding.mpChart.xAxis.valueFormatter.getAxisLabel(h!!.x, binding.mpChart.xAxis)
        val entryDate = chartDates[h!!.x.toInt()].toString()
        Log.e("ChartDates", entryDate)

        BaseDialog(
            this@StatisticsActivity,
            "Do you want to open this log with the selected day?<br><br><b>Consumed substances: ${h.y.toInt()}</b>",
            title = "Details - ${entryDate}",
            onPositive = {_, _ ->
                val intent = Intent(this, LogActivity::class.java)
                intent.putExtra("date", entryDate)
                startActivity(intent)
            }, onNegative = {dInterface, _ ->
                dInterface.dismiss()
            }).show()
    }

    override fun onNothingSelected() {}
}

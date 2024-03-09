package com.jan.psywikikt

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textview.MaterialTextView
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.jan.psywikikt.data.DrugIndexTripSitDao
import com.jan.psywikikt.data.MainDatabase
import com.jan.psywikikt.databinding.ActivityInfoTripsitBinding
import com.jan.psywikikt.dialogs.ErrorDialog
import kotlinx.coroutines.launch
import java.io.FileNotFoundException

class TripSitInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInfoTripsitBinding

    private lateinit var fadeIn: Animation

    private lateinit var substance: String

    private val db = MainDatabase.getInstance(this)
    private lateinit var drugIndexTripSitDao: DrugIndexTripSitDao

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInfoTripsitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SQLite
        drugIndexTripSitDao = db.drugIndexTripSitDao()

        // FadeIn Animation
        fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.duration = 1000

        substance = intent.extras!!.getString("substance").toString()
        supportActionBar!!.subtitle = "$substance - TripSit"

        lifecycleScope.launch {
            try {
                val json: JsonObject = JSONTripSit(this@TripSitInfoActivity).readJSON(drugIndexTripSitDao.getIDNameFromPrettyName(substance)!!)
                val emptyArray = JsonArray()
                emptyArray.add("Not found")
                Log.d("JSON", json.toString())

                addChips(
                    if(json.has("aliases")) json.getAsJsonArray("aliases") else emptyArray,
                    binding.synonymChips
                )
                addChips(
                    if(json.has("categories")) json.getAsJsonArray("categories") else emptyArray,
                    binding.categoryChips
                )
                if(json.has("formatted_effects")) {
                    addChips(
                        json.getAsJsonArray("formatted_effects"),
                        binding.effectChips
                    )
                } else {
                    binding.linearEffects.visibility = LinearLayout.GONE
                }

                // Set summary and caution text
                binding.summaryText.text = json.getAsJsonObject("properties").get("summary").asString
                if(json.getAsJsonObject("properties").has("avoid")) {
                    binding.cautionText.text = "Avoid " + json.getAsJsonObject("properties")["avoid"].asString
                    binding.cautionText.visibility = LinearLayout.VISIBLE
                }

                // Dosage card
                if(json.has("formatted_dose")) makeCards(json.getAsJsonObject("formatted_dose"), binding.linearDosage)
                if(json.has("dose_note")) {
                    val doseNote = MaterialTextView(this@TripSitInfoActivity)
                    binding.linearDosage.addView(doseNote)
                    val marginParams = doseNote.layoutParams as ViewGroup.MarginLayoutParams
                    marginParams.setMargins(0, resources.getDimension(R.dimen.main_card_text_margin).toInt(), 0, 0)

                    doseNote.apply {
                        typeface = ResourcesCompat.getFont(context, R.font.advent_pro_light)
                        text = json["dose_note"].asString.trim()
                        layoutParams = marginParams
                        setTextColor(ContextCompat.getColor(context, R.color.main_card_text_warning))
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.main_card_text_size))
                    }
                }

                // Duration card
                val durations = getJSONDurations(json)
                makeCards(durations, binding.linearDuration)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                ErrorDialog(this@TripSitInfoActivity, "JSON file not found! Update <b>TripSit</b> in <b>Settings</b>", onPositive = { _, _ -> finishAndRemoveTask() }).show()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                ErrorDialog(this@TripSitInfoActivity, "Substance name not found in JSON!", onPositive = { _, _ -> finishAndRemoveTask() }).show()
            }
        }
    } override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.info_tripsit_menu, menu)
        return super.onCreateOptionsMenu(menu)
    } override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.itemTripRedirect -> {
                val intent = Intent(this@TripSitInfoActivity, BrowserActivity::class.java)
                intent.putExtra("URL", "https://drugs.tripsit.me/$substance")
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addChips(items: JsonArray, chipGroup: ChipGroup) {
        for(i in items) {
            val chip = Chip(this)
            runOnUiThread {
                chip.text = i.asString
                chipGroup.addView(chip)
            }
        }
    }

    private fun getJSONDurations(json: JsonObject): JsonObject {
        // Combine multiple json objects to one
        val durations = JsonObject()
        val durationNames: Map<String, String> = mapOf("formatted_onset" to "Onset", "formatted_duration" to "Duration", "formatted_aftereffects" to "After effects")
        for(entryName in durationNames) {
            if(json.has(entryName.key)) {
                durations.add(entryName.value, json.getAsJsonObject(entryName.key))
            }
        }
        return durations
    }

    @SuppressLint("SetTextI18n")
    private fun makeCards(json: JsonObject?, parentView: LinearLayout) {
        // Dosage and duration cards
        for(roa: MutableMap.MutableEntry<String, JsonElement> in json!!.asMap()) {
            runOnUiThread {
                val roaTitle = MaterialTextView(this)
                val roaCard = LinearLayout(this)

                roaTitle.apply {
                    typeface = ResourcesCompat.getFont(context, R.font.advent_pro_regular)
                    text = roa.key
                    animation = fadeIn
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.main_card_subtitle_size))
                }
                roaCard.orientation = LinearLayout.HORIZONTAL

                parentView.addView(roaTitle)
                parentView.addView(roaCard)

                roaCard.updateLayoutParams {
                    width = LinearLayout.LayoutParams.MATCH_PARENT
                    height = LinearLayout.LayoutParams.WRAP_CONTENT
                }

                // Bottom margin of title
                val marginParams = roaTitle.layoutParams as ViewGroup.MarginLayoutParams
                marginParams.setMargins(0, 0, 0, resources.getDimension(R.dimen.main_card_subtitle_margin).toInt())
                roaTitle.layoutParams = marginParams

                // Dynamically create Sub-ROA cards
                val subCardMargin: Int = resources.getDimension(R.dimen.sub_card_margin).toInt()
                val subCardPadding: Int = resources.getDimension(R.dimen.sub_card_padding).toInt()
                val subCardParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
                subCardParams.setMargins(subCardMargin, subCardMargin, subCardMargin, subCardMargin)

                var unit = ""
                Log.d("ROA", roa.value.asJsonObject.asMap().toString())
                for(dose: MutableMap.MutableEntry<String, JsonElement> in roa.value.asJsonObject.asMap()) {
                    // Set unit of substance
                    if(dose.key == "_unit") {
                        unit = dose.value.asString
                        continue
                    }

                    val roaSubCard = LinearLayout(this)
                    val dosageTitle = MaterialTextView(this)
                    val dosageSubTitle = MaterialTextView(this)

                    roaSubCard.apply {
                        background = ContextCompat.getDrawable(applicationContext, R.drawable.sub_card_background)
                        orientation = LinearLayout.VERTICAL
                        animation = fadeIn
                    }
                    dosageTitle.apply {
                        typeface = ResourcesCompat.getFont(context, R.font.advent_pro_light)
                        gravity = Gravity.CENTER
                        animation = fadeIn
                        text = if(dose.key == "value") "General" else dose.key
                    }
                    dosageSubTitle.apply {
                        typeface = ResourcesCompat.getFont(context, R.font.advent_pro_light)
                        gravity = Gravity.CENTER
                        text = "${dose.value.asString} $unit"
                    }

                    roaCard.addView(roaSubCard)
                    roaSubCard.layoutParams = subCardParams
                    roaSubCard.setPadding(subCardPadding, subCardPadding, subCardPadding, subCardPadding)
                    roaSubCard.addView(dosageTitle)
                    roaSubCard.addView(dosageSubTitle)
                }
            }
        }
    }
}
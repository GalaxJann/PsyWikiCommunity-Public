package com.jan.psywikikt

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.jan.psywikikt.data.DrugFavourite
import com.jan.psywikikt.data.DrugFavouriteDao
import com.jan.psywikikt.data.DrugIndexDao
import com.jan.psywikikt.data.DrugTimerDao
import com.jan.psywikikt.data.DrugUnitDao
import com.jan.psywikikt.data.MainDatabase
import com.jan.psywikikt.databinding.ActivityInfoBinding
import com.jan.psywikikt.dialogs.ErrorDialog
import com.jan.psywikikt.dialogs.HTMLErrorDialog
import com.jan.psywikikt.dialogs.TimerDialog
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.lang.IllegalStateException
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Date

class InfoActivity : AppCompatActivity() {
    // SQLite Database
    private lateinit var drugFavouriteDao: DrugFavouriteDao
    private lateinit var drugTimerDao: DrugTimerDao
    private lateinit var drugIndexDao: DrugIndexDao
    private lateinit var drugUnitDao: DrugUnitDao

    private val client = OkHttpClient()
    private val gson = Gson()

    private lateinit var drugName: String

    private lateinit var rawDrugData: String
    private lateinit var drugData: JsonObject

    // TODO: Disable MenuItem if substance not found
    private lateinit var menuItem: MenuItem

    private lateinit var fadeIn: Animation

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FadeIn Animation
        fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.duration = 1000

        // Database
        val db = MainDatabase.getInstance(this)
        drugFavouriteDao = db.drugFavouriteDao()
        drugTimerDao = db.drugTimerDao()
        drugIndexDao = db.drugIndexDao()
        drugUnitDao = db.drugUnitDao()

        val binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drugName = intent.extras!!.getString("substance").toString()

        Thread {
            // Get substance data
            val json: JsonObject = gson.fromJson(getSubstanceDetails(drugName), JsonObject::class.java)
            val substances: JsonArray = json
                .getAsJsonObject("data")
                .getAsJsonArray("substances")
            if(substances.size() > 0) {
                drugData = substances[0].asJsonObject
            } else {
                runOnUiThread { supportActionBar!!.subtitle = "Substance not found!" }
                return@Thread
            }

            runOnUiThread {
                // Parse substance data
                if(json.has("updateDate")) {
                    // If offline data is used
                    val date: LocalDateTime = Date(json["updateDate"].asLong).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    val formatter: DateTimeFormatter = DateTimeFormatterBuilder().appendPattern("dd.MM.yyyy HH:mm").toFormatter()
                    supportActionBar!!.subtitle = "${drugData["name"].asString} - ${date.format(formatter)}"
                } else {
                    supportActionBar!!.subtitle = drugData["name"].asString
                }

                // Create chips
                val commonNames: JsonArray
                val chemicalClasses: JsonArray
                val psychoactiveClasses: JsonArray
                val NOT_FOUND: JsonArray = gson.fromJson("[\"Not found\"]", JsonArray::class.java)

                if(!drugData["commonNames"].isJsonNull) {
                    commonNames = gson.fromJson(drugData["commonNames"], JsonArray::class.java)
                } else {
                    commonNames = NOT_FOUND
                }

                if(!drugData["class"].isJsonNull) {
                    chemicalClasses = if(!drugData.getAsJsonObject("class")["chemical"].isJsonNull) {
                        drugData.getAsJsonObject("class").getAsJsonArray("chemical")
                    } else {
                        NOT_FOUND
                    }

                    psychoactiveClasses = if(!drugData.getAsJsonObject("class")["psychoactive"].isJsonNull) {
                        drugData.getAsJsonObject("class").getAsJsonArray("psychoactive")
                    } else {
                        NOT_FOUND
                    }
                } else {
                    chemicalClasses = NOT_FOUND
                    psychoactiveClasses = NOT_FOUND
                }

                addChips(commonNames, binding.commonsChips)
                addChips(chemicalClasses, binding.chemicalChips)
                addChips(psychoactiveClasses, binding.psychoactiveChips)

                for(roaRaw in drugData.getAsJsonArray("roas")) {
                    val roa: JsonObject = roaRaw.asJsonObject
                    val roaName: String = roa["name"].asString[0].uppercase() + roa["name"].asString.substring(1)

                    // Skip roa if no info available
                    if(roa["dose"].isJsonNull) {
                        continue
                    }

                    // Init elements
                    val roaTitle = MaterialTextView(this)
                    val durationROATitle = MaterialTextView(this)
                    val roaCard = LinearLayout(this)
                    val durationCard = LinearLayout(this)
                    roaTitle.apply {
                        typeface = ResourcesCompat.getFont(context, R.font.advent_pro_regular)
                        text = roaName
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.main_card_subtitle_size))
                        animation = fadeIn
                    }
                    durationROATitle.apply {
                        typeface = ResourcesCompat.getFont(context, R.font.advent_pro_regular)
                        text = roaName
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.main_card_subtitle_size))
                        animation = fadeIn
                    }
                    roaCard.orientation = LinearLayout.HORIZONTAL
                    durationCard.orientation = LinearLayout.HORIZONTAL

                    binding.linearDosage.addView(roaTitle)
                    binding.linearDosage.addView(roaCard)
                    binding.linearDuration.addView(durationROATitle)
                    binding.linearDuration.addView(durationCard)

                    roaCard.updateLayoutParams {
                        width = LayoutParams.MATCH_PARENT
                        height = LayoutParams.WRAP_CONTENT
                    }
                    durationCard.updateLayoutParams {
                        width = LayoutParams.MATCH_PARENT
                        height = LayoutParams.WRAP_CONTENT
                    }

                    // Set bottom margin of title
                    val marginParams = roaTitle.layoutParams as ViewGroup.MarginLayoutParams
                    marginParams.setMargins(0, 0, 0, resources.getDimension(R.dimen.main_card_subtitle_margin).toInt())
                    roaTitle.layoutParams = marginParams

                    // Dynamically create Sub-ROA cards
                    val dose: JsonObject = roa.getAsJsonObject("dose")
                    val unit: String = dose["units"].asString
                    dose.remove("units")    // Remove untis before iterating
                    val subCardMargin: Int = resources.getDimension(R.dimen.sub_card_margin).toInt()
                    val subCardPadding: Int = resources.getDimension(R.dimen.sub_card_padding).toInt()
                    val subCardParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f)
                    subCardParams.setMargins(subCardMargin, subCardMargin, subCardMargin, subCardMargin)

                    // Dynamically create dosage Sub-Cards
                    for(i in dose.asMap()) {
                        if(!i.value.isJsonNull) {
                            val title: String = i.key[0].uppercase() + i.key.substring(1)
                            val dosageTitle = MaterialTextView(this)
                            val dosageSubTitle = MaterialTextView(this)
                            val roaSubCard = LinearLayout(this)

                            roaSubCard.apply {
                                background = ContextCompat.getDrawable(applicationContext, R.drawable.sub_card_background)
                                orientation = LinearLayout.VERTICAL
                                animation = fadeIn
                            }
                            dosageTitle.apply {
                                typeface = ResourcesCompat.getFont(context, R.font.advent_pro_light)
                                setTextColor(ContextCompat.getColor(this@InfoActivity, R.color.info_subcard_text_color))
                                gravity = Gravity.CENTER
                                text = title
                            }
                            dosageSubTitle.apply {
                                typeface = ResourcesCompat.getFont(context, R.font.advent_pro_light)
                                setTextColor(ContextCompat.getColor(this@InfoActivity, R.color.info_subcard_text_color))
                                gravity = Gravity.CENTER
                            }

                            val df = DecimalFormat("0")
                            df.maximumFractionDigits = 4

                            if(!i.value.isJsonNull) {
                                try {
                                    // Dosage has min and max
                                    val value = i.value.asJsonObject
                                    dosageSubTitle.text = "${df.format(value["min"].asFloat)} - ${df.format(value["max"].asFloat)}$unit"
                                } catch (e: IllegalStateException) {
                                    // Dosage is single number
                                    val value = df.format(i.value.asFloat)
                                    dosageSubTitle.text = "$value$unit"
                                }
                            }

                            roaCard.addView(roaSubCard)
                            roaSubCard.layoutParams = subCardParams
                            roaSubCard.setPadding(subCardPadding, subCardPadding, subCardPadding, subCardPadding)
                            roaSubCard.addView(dosageTitle)
                            roaSubCard.addView(dosageSubTitle)
                        }
                    }

                    // Dynamically create duration Sub-Cards
                    val durations: JsonObject
                    if(roa["duration"].isJsonNull) {
                        durationROATitle.text = "No duration data given for this substance"
                        return@runOnUiThread
                    } else {
                        durations = roa.getAsJsonObject("duration")
                    }
                    for(type in listOf("total", "onset", "comeup", "peak", "offset", "afterglow")) {
                        addDurationSubCard(type, durations, durationCard, subCardParams, subCardPadding)
                    }
                }


                // Addiction Potential card
                if(!drugData["addictionPotential"].isJsonNull) {
                    binding.textAddiction.text = drugData["addictionPotential"].asString
                } else {
                    binding.textAddiction.text = "No addiction information found!"
                }

                // Interactions cards
                addInteractions("uncertainInteractions", drugData, binding.textUncertain, binding.uncertainChips, R.color.uncertain_chip_background)
                addInteractions("unsafeInteractions", drugData, binding.textUnsafe, binding.unsafeChips, R.color.unsafe_chip_background)
                addInteractions("dangerousInteractions", drugData, binding.textDangerous, binding.dangerousChips, R.color.dangerous_chip_background)

                // Update offline data if substance is favourite
                if(menuItem.isChecked && this::rawDrugData.isInitialized) {
                    // If favourite checkbox is set
                    lifecycleScope.launch {
                        drugFavouriteDao.updateByName(drugName, rawDrugData, Date())
                    }
                }

                // Get images
                /* val images: JsonArray = drugData.getAsJsonArray("images")
                val imageList: MutableList<String> = mutableListOf()
                for(i in images) {
                    imageList.add(i.asJsonObject["image"].asString)
                } */
            }
        }.start()
    } private fun getSubstanceDetails(substance: String): String {
        val jsonBody = JsonObject()
        jsonBody.add("variables", JsonObject())
        jsonBody.addProperty("query", "{substances(query: \"$substance\") {name url commonNames addictionPotential uncertainInteractions { name } unsafeInteractions { name } dangerousInteractions { name } class { chemical psychoactive } roas { name dose {units threshold light {min max} common {min max} strong {min max} heavy} duration {afterglow {min max units} comeup {min max units} duration {min max units} offset {min max units} onset {min max units} peak {min max units} total {min max units}} bioavailability {min max}}}}")

        val request: Request = Request.Builder()
            .url("https://api.psychonautwiki.org/")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody())
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if(!response.isSuccessful) {
                HTMLErrorDialog(response.code, this).show()

                // Get offline data if available
                val offlineData: List<DrugFavourite> = drugFavouriteDao.getFavourite(drugName)
                if(offlineData.size == 1) { return offlineData[0].substanceData }

                // Return empty substance data
                return "{\"data\": {\"substances\": []}}"
            }

            rawDrugData = response.body!!.string()
            return rawDrugData
        } catch (e: IOException) {
            // Connection probably lost

            val offlineData: List<DrugFavourite> = drugFavouriteDao.getFavourite(drugName)
            if(offlineData.size == 1) {
                val json: JsonObject = gson.fromJson(offlineData[0].substanceData, JsonObject::class.java)
                json.addProperty("updateDate", offlineData[0].updateDate.time)
                ErrorDialog(this, "Connection lost!<br><br>Display last offline data for this substance<br><br>Timestamp: ${offlineData[0].updateDate}").show()

                return json.toString()
            }

            // Return empty substance data
            return "{\"data\": {\"substances\": []}}"
        }
    } @SuppressLint("SetTextI18n")
    private fun addDurationSubCard(
        type: String,
        durations: JsonObject,
        durationCard: LinearLayout,
        subCardParams: LayoutParams,
        subCardPadding: Int
    ) {
        if(!durations[type].isJsonNull) {
            val durationTitle = MaterialTextView(this)
            val durationSubTitle = MaterialTextView(this)
            val durationSubCard = LinearLayout(this)
            val durationEntries = durations[type].asJsonObject

            durationSubCard.apply {
                background = ContextCompat.getDrawable(applicationContext, R.drawable.sub_card_background)
                orientation = LinearLayout.VERTICAL
            }
            durationTitle.apply {
                typeface = ResourcesCompat.getFont(context, R.font.advent_pro_light)
                setTextColor(ContextCompat.getColor(this@InfoActivity, R.color.info_subcard_text_color))
                gravity = Gravity.CENTER
                if(type.equals("afterglow")) {
                    text = "After effects"
                } else {
                    text = type[0].uppercase() + type.substring(1)
                }
            }

            val min: Int
            val max: Int
            if(!durationEntries["min"].isJsonNull) {
                min = durationEntries["min"].asInt
            } else { min = 0 }
            if(!durationEntries["max"].isJsonNull) {
                max = durationEntries["max"].asInt
            } else { max = 0 }

            durationSubTitle.apply {
                typeface = ResourcesCompat.getFont(context, R.font.advent_pro_light)
                setTextColor(ContextCompat.getColor(this@InfoActivity, R.color.info_subcard_text_color))
                gravity = Gravity.CENTER
                text = min.toString() + " - " + max + durationEntries["units"].asString[0]
            }

            durationCard.addView(durationSubCard)
            durationSubCard.layoutParams = subCardParams
            durationSubCard.setPadding(subCardPadding, subCardPadding, subCardPadding, subCardPadding)
            durationSubCard.animation = fadeIn
            durationSubCard.addView(durationTitle)
            durationSubCard.addView(durationSubTitle)
        }
    } private fun addInteractions(
        type: String,
        drugData: JsonObject,
        textView: MaterialTextView,
        chipGroup: ChipGroup,
        color: Int
    ) {
        if(!drugData[type].isJsonNull) {
            textView.visibility = MaterialTextView.VISIBLE

            for(i in drugData.getAsJsonArray(type)) {
                val chip = Chip(this)
                chip.apply {
                    text = i.asJsonObject["name"].asString
                    chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, color))
                    setTextColor(Color.BLACK)
                }
                chip.animation = fadeIn
                chipGroup.addView(chip)
            }
        }
    } private fun addChips(jsonArray: JsonArray, chipGroup: ChipGroup) {
        for(i in jsonArray) {
            val chip = Chip(this)
            chip.apply { text = i.asString }
            chip.animation = fadeIn
            chipGroup.addView(chip)
        }
    } override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu
        menuInflater.inflate(R.menu.info_menu, menu)
        return super.onCreateOptionsMenu(menu)
    } override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val favItem: MenuItem = menu!!.findItem(R.id.itemRemember)
        menuItem = favItem
        if(drugFavouriteDao.getFavourite(drugName).size == 1) {
            favItem.isChecked = true
        }
        return super.onPrepareOptionsMenu(menu)
    } override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.itemRemember -> {
                if(item.isChecked) {
                    item.isChecked = false
                    lifecycleScope.launch {
                        drugFavouriteDao.deleteFavouriteByName(drugName)
                    }
                } else {
                    item.isChecked = true
                    lifecycleScope.launch {
                        drugFavouriteDao.insertFavourite(DrugFavourite(0, drugName, rawDrugData, Date()))
                    }
                }
            } R.id.itemPsyRedirect -> {
                val intent = Intent(this@InfoActivity, BrowserActivity::class.java)
                intent.putExtra("URL", "https://psychonautwiki.org/wiki/$drugName")
                startActivity(intent)
            } R.id.itemEffects -> {
                val intent = Intent(this@InfoActivity, EffectsActivity::class.java)
                intent.putExtra("substance", drugName)
                startActivity(intent)
            } R.id.itemTimer -> {
                val timerDialog = TimerDialog(this, lifecycleScope, drugTimerDao, drugUnitDao, drugName = drugName, drugIndexDao = drugIndexDao)
                timerDialog.build().show()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

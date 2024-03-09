package com.jan.psywikikt

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.text.HtmlCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.DocumentFileCompat
import com.anggrayudi.storage.file.FileFullPath
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.jan.psywikikt.data.DrugFavouriteDao
import com.jan.psywikikt.data.DrugIndex
import com.jan.psywikikt.data.DrugIndexDao
import com.jan.psywikikt.data.DrugIndexTripSit
import com.jan.psywikikt.data.DrugIndexTripSitDao
import com.jan.psywikikt.data.DrugLog
import com.jan.psywikikt.data.DrugLogDao
import com.jan.psywikikt.data.DrugUnit
import com.jan.psywikikt.data.DrugUnitDao
import com.jan.psywikikt.data.MainDatabase
import com.jan.psywikikt.databinding.ActivitySettingsBinding
import com.jan.psywikikt.dialogs.ErrorDialog
import com.jan.psywikikt.dialogs.HTMLErrorDialog
import com.jan.psywikikt.dialogs.UnitDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.Date
import kotlin.IllegalStateException

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    // SQLite Database
    private lateinit var drugIndexDao: DrugIndexDao
    private lateinit var drugIndexTripSitDao: DrugIndexTripSitDao
    private lateinit var drugFavouriteDao: DrugFavouriteDao
    private lateinit var drugUnitDao: DrugUnitDao
    private lateinit var drugLogDao: DrugLogDao

    private val storageHelper = SimpleStorageHelper(this)
    private val client = OkHttpClient()
    private val gson = Gson()

    @SuppressLint("CommitPrefEdits", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val db = MainDatabase.getInstance(this)
        drugIndexDao = db.drugIndexDao()
        drugIndexTripSitDao = db.drugIndexTripSitDao()
        drugFavouriteDao = db.drugFavouriteDao()
        drugUnitDao = db.drugUnitDao()
        drugLogDao = db.drugLogDao()

        // Search mode menu
        val searchModeAdapter: ArrayAdapter<String> = ArrayAdapter<String>(this, androidx.appcompat.R.layout.select_dialog_item_material, arrayOf("Old Psychonaut Search - Not recommended", "API Search - Psychonaut", "API Search - TripSit"))
        binding.searchModeMenu.apply {
            threshold = 1
            setAdapter(searchModeAdapter)
            setText("Old Psychonaut Search - Not recommended", false)
        }
        if(sharedPreferences.getString("searchmode", "NULL") == "standardpsy") {
            binding.searchModeMenu.setText("Old Psychonaut Search - Not recommended", false)
            binding.SQLiteText.text = "Select another search method to use DB-Update"
        } else if(sharedPreferences.getString("searchmode", "NULL") == "apipsy") {
            binding.searchModeMenu.setText("API Search - Psychonaut", false)
            binding.SQLiteText.text = "API Database - Standby - Psychonaut-DB"
        } else if(sharedPreferences.getString("searchmode", "NULL") == "apitripsit") {
            binding.searchModeMenu.setText("API Search - TripSit", false)
            binding.SQLiteText.text = "API Database - Standby - TripSit-DB"
        }

        binding.buttonUpSQLite.setOnClickListener {
            // Update DrugIndex drugs with Psychonaut or TripSit Request
            if(sharedPreferences.getString("searchmode", "NULL") == "apitripsit") {
                Thread { getAPIDrugs(binding, true) }.start()
            } else if(sharedPreferences.getString("searchmode", "NULL") == "apipsy") {
                Thread { getAPIDrugs(binding, false) }.start()
            }
        }
        binding.searchModeMenu.setOnItemClickListener { adapterView, view, pos, id ->
            when(pos) {
                0 -> {
                    editor.putString("searchmode", "standardpsy")
                    binding.SQLiteText.text = "Select another search method to use DB-Update"
                }
                1 -> {
                    editor.putString("searchmode", "apipsy")
                    binding.SQLiteText.text = "API Database - Standby - Psychonaut-DB"
                }
                2 -> {
                    editor.putString("searchmode", "apitripsit")
                    binding.SQLiteText.text = "API Database - Standby - TripSit-DB"
                    ErrorDialog(
                        this,
                        "<h3>Offline-Use</h3><br>When you updated the <b>TripSit</b> database and json (via \"<i>UPDATE API DATABASE</i>\"), this search mode works completely offline, because all substance data has been downloaded to the device.<br><br><h3>Favourites</h3><br>Favourites aren't working currently with <b>TripSit</b> mode.<br>If you have favourites, they will open the <b>Psychonaut</b> entry furthermore!",
                        title = "Notice"
                    ).show()
                }
            }
            editor.apply()
        }

        // Units menu
        var selectedUnit: String? = null
        var unitsAdapter: ArrayAdapter<String>

        // Initialize dropdown
        getUnitList(binding)
        binding.buttonDefaultUnit.isChecked = true

        binding.unitMenu.setOnItemClickListener { adapterView, view, i, l ->
            // Workaround for NullPointerExceptions; probably bad solution for performance
            unitsAdapter = getUnitList(binding)
            selectedUnit = unitsAdapter.getItem(i)!!.toString()
            binding.unitMenu.setText(selectedUnit, false)
            if(selectedUnit!!.contains(" - Default")) {
                selectedUnit = selectedUnit!!.replace(" - Default", "")
            }

            setButtonVisibility(listOf(binding.buttonDefaultUnit, binding.buttonDeleteUnit),
                !drugUnitDao.getUnitByName(selectedUnit!!)!!.usedefault
            )
        }

        // Unit default button
        binding.buttonDefaultUnit.setOnClickListener {
            lifecycleScope.launch {
                drugUnitDao.setDefault(selectedUnit!!)
            }
            getUnitList(binding)
            setButtonVisibility(listOf(binding.buttonDefaultUnit, binding.buttonDeleteUnit), false)
        }

        // Unit delete button
        binding.buttonDeleteUnit.setOnClickListener {
            lifecycleScope.launch {
                drugUnitDao.deleteUnit(drugUnitDao.getUnitByName(selectedUnit!!)!!)
                getUnitList(binding)
            }
            setButtonVisibility(listOf(binding.buttonDefaultUnit, binding.buttonDeleteUnit), false)
        }

        // Unit add button
        binding.buttonAddUnit.setOnClickListener {
            val dialog = UnitDialog(this, lifecycleScope, drugUnitDao, updateUnitList = { getUnitList(binding) })
            dialog.build()
            dialog.show()
        }

        val dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/Psychonaut"
        val file = File(dirPath, "consumption_export.json")

        binding.exportLog.setOnClickListener {
            binding.exportLogStatus.text = "Consumption Log - Export\nStatus: Exporting"

            val dir = File(dirPath)
            if(!dir.exists()) {
                dir.mkdir()
            }

            if(file.exists()) {
                val dialog = MaterialAlertDialogBuilder(this@SettingsActivity)
                    .setTitle("Export")
                    .setMessage(HtmlCompat.fromHtml(
                        "Export file already exists, or folder cannot be accessed. Due to permission purposes since Android 11, this file cannot be overridden. Please delete folder in <b>/Download/Psychonaut</b>?",
                        HtmlCompat.FROM_HTML_MODE_COMPACT)
                    ).setNegativeButton("Ok", null)
                dialog.show()
                binding.exportLogStatus.text = "Consumption Log - Export\nStatus: Failed!"
            } else { exportLogToFile(file) }
        }

        binding.importLog.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(this@SettingsActivity)
                .setTitle("Import")
                .setPositiveButton("Yes", { _, _ -> importLogFile(dirPath) })
                .setNegativeButton("No", null)

            binding.exportLogStatus.text = "Consumption Log - Import\nStatus: Importing"

            if(file.exists()) {
                dialog.setMessage(HtmlCompat.fromHtml(
                    "Do you want to import the export file, located in <b>/Download/consumption_export.json</b> into the database of this app?<br><br><b>CAUTION: This action will reset your current log!</b>",
                    HtmlCompat.FROM_HTML_MODE_COMPACT)
                )
                dialog.show()
            } else {
                ErrorDialog(this, "Export file cannot be found").show()
                binding.exportLogStatus.text = "Consumption Log - Import\nStatus: Failed!"
            }
        }
    } @SuppressLint("SetTextI18n")
    private fun getAPIDrugs(binding: ActivitySettingsBinding, useTripSit: Boolean) {
        if(useTripSit) {
            // TripSit-DB and JSON
            val request: Request = Request.Builder()
                .url("https://raw.githubusercontent.com/TripSit/drugs/main/drugs.json")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            runOnUiThread { supportActionBar!!.subtitle = "Indexing..." }
            val response: Response
            try {
                response = client.newCall(request).execute()
                if(!response.isSuccessful) {
                    runOnUiThread { supportActionBar!!.subtitle = "Error!" }
                    HTMLErrorDialog(response.code, this).show()
                    return
                }
            } catch(e: IOException) {
                // Connection (probably) lost
                runOnUiThread { supportActionBar!!.subtitle = "Error!" }
                ErrorDialog(this, "Connection lost!").show()
                return
            }

            val responseJSON: JsonObject = gson.fromJson(response.body!!.string(), JsonObject::class.java)
            val jsonTripSit = JSONTripSit(this)
            jsonTripSit.writeJSON(responseJSON.toString())

            runOnUiThread {
                binding.SQLiteProgressBar.min = 0
                binding.SQLiteProgressBar.max = responseJSON.size()
                binding.SQLiteProgressBar.visibility = LinearLayout.VISIBLE
            }

            lifecycleScope.launch {
                var totalCount = 0
                var updateCount = 0
                var skipCount = 0
                // Reset TripSit SQLite if device has old TripSit-DB
                drugIndexTripSitDao.updateReset()
                for(drug in responseJSON.keySet()) {
                    try {
                        val synonyms: String
                        if(responseJSON.getAsJsonObject(drug).has("aliases")) {
                            synonyms = responseJSON.getAsJsonObject(drug).getAsJsonArray("aliases").toString()
                        } else {
                            synonyms = "[]"
                        }

                        drugIndexTripSitDao.insertDrug(DrugIndexTripSit(0, responseJSON.getAsJsonObject(drug).get("pretty_name").asString, drug, synonyms))
                        updateCount++
                        totalCount++
                    } catch(e: SQLiteConstraintException) {
                        skipCount++
                        totalCount++
                    }
                    runOnUiThread {
                        binding.SQLiteText.text = "TripSit Drug-Index Update successfully\nUpdated: $updateCount\nSkipped: $skipCount\nTotal indexed search items: $totalCount / ${responseJSON.size()}"
                        binding.SQLiteProgressBar.progress = totalCount
                    }
                }
                runOnUiThread {
                    supportActionBar!!.subtitle = null
                    binding.SQLiteProgressBar.visibility = LinearLayout.GONE
                }
            }
        } else {
            // Psychonaut-DB
            val jsonBody = JsonObject()
            jsonBody.add("variables", JsonObject())
            jsonBody.addProperty("query", "{substances(limit: 1000) { name commonNames class { psychoactive } }}")

            val request: Request = Request.Builder()
                .url("https://api.psychonautwiki.org/")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody())
                .build()

            runOnUiThread { supportActionBar!!.subtitle = "Indexing..." }
            val response: Response
            try {
                response = client.newCall(request).execute()
                if(!response.isSuccessful) {
                    runOnUiThread { supportActionBar!!.subtitle = "Error!" }
                    HTMLErrorDialog(response.code, this).show()
                    return
                }
            } catch(e: IOException) {
                // Connection (probably) lost
                ErrorDialog(this, "Connection lost!").show()
                runOnUiThread { supportActionBar!!.subtitle = "Error!" }
                return
            }

            val responseString = response.body!!.string()
            val responseJSON: JsonArray = gson.fromJson(responseString, JsonObject::class.java)
                .getAsJsonObject("data")
                .getAsJsonArray("substances")
            Log.d("Psychonaut response", responseJSON.toString())

            runOnUiThread {
                binding.SQLiteProgressBar.min = 0
                binding.SQLiteProgressBar.max = responseJSON.size()
                binding.SQLiteProgressBar.visibility = LinearLayout.VISIBLE
            }

            lifecycleScope.launch(Dispatchers.IO) {
                var totalCount = 0
                var updateCount = 0
                var skipCount = 0
                drugIndexDao.updateReset()
                for(drug in responseJSON) {
                    val name: String = drug.asJsonObject["name"].asString
                    var commonNames = ""
                    // If common names are available
                    if(!drug.asJsonObject["commonNames"].isJsonNull) {
                        commonNames = drug.asJsonObject.getAsJsonArray("commonNames").toString()
                        for(i in drug.asJsonObject.getAsJsonArray("commonNames")) { // Count every common name
                            if(i.asString != name) {
                                totalCount++
                            }
                        }
                    }

                    try {
                        if(!drug.asJsonObject["class"].isJsonNull) {
                            Log.e("DEBUG", drug.asJsonObject["class"].asJsonObject["psychoactive"].asJsonArray.get(0).asString)
                        } else {
                            Log.e("DEBUG NO CLASS", drug.toString())
                        }
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                        Log.e("JSON Error", drug.asJsonObject.toString())
                    }


                    try {
                        if(!drug.asJsonObject["class"].isJsonNull) {
                            drugIndexDao.insertDrug(DrugIndex(
                                0,
                                name,
                                commonNames,
                                if(!drug.asJsonObject["class"].asJsonObject["psychoactive"].isJsonNull) drug.asJsonObject["class"].asJsonObject["psychoactive"].asJsonArray.get(0).asString else null)
                            )
                        } else {
                            drugIndexDao.insertDrug(DrugIndex(0, name, commonNames, null))
                        }
                        updateCount++
                        totalCount++
                    } catch(e: SQLiteConstraintException) {
                        skipCount++
                        totalCount++
                    } catch (e: IllegalStateException) {
                        e.printStackTrace()
                        Log.e("JSON Error", drug.asJsonObject.toString())
                    }
                    runOnUiThread {
                        binding.SQLiteText.text = "Psychonaut Drug-Index Update successfully\nUpdated: $updateCount\nSkipped: $skipCount\nTotal indexed search items: $totalCount"
                        binding.SQLiteProgressBar.progress = updateCount + skipCount    // Total count doesn't work cause they count also the synonyms
                    }
                }

                runOnUiThread {
                    supportActionBar!!.subtitle = null
                    binding.SQLiteProgressBar.visibility = LinearLayout.GONE
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getUnitList(binding: ActivitySettingsBinding): ArrayAdapter<String> {
        val sqlUnits: List<DrugUnit> = drugUnitDao.getAllUnits()
        val units: MutableList<String> = mutableListOf()
        var defaultUnit: DrugUnit? = null
        for(unit in sqlUnits) {
            if(unit.usedefault) {
                defaultUnit = unit
                units.add("${unit.unit} - Default")
                continue
            }
            units.add(unit.unit)
        }
        val unitsAdapter: ArrayAdapter<String> = ArrayAdapter<String>(this, androidx.appcompat.R.layout.select_dialog_item_material, units)
        binding.unitMenu.apply {
            threshold = 1
            setAdapter(unitsAdapter)
            setText("${defaultUnit!!.unit} - Default", false)
        }

        return unitsAdapter
    }

    private fun setButtonVisibility(buttons: List<MaterialButton>, visible: Boolean) {
        for(button in buttons) {
            button.visibility = if(visible) LinearLayout.VISIBLE else LinearLayout.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun exportLogToFile(file: File) {
        val json = JsonArray()
        for(log in drugLogDao.getAllLogs()) {
            val logObject = JsonObject()

            logObject.addProperty("id", log.id)
            logObject.addProperty("name", log.name)
            logObject.addProperty("dose", log.dose)
            logObject.addProperty("unit", log.unit)
            logObject.addProperty("notes", log.notes)
            logObject.addProperty("timestamp", log.timestamp.time)

            json.add(logObject)
        }

        // TODO: Update export filename
        val outputStream = FileOutputStream(file)
        outputStream.write(json.toString().toByteArray())
        outputStream.close()

        binding.exportLogStatus.text = "Consumption Log - Export\nStatus: Completed"
    }

    private fun importLogFile(path: String) {
        storageHelper.openFolderPicker(initialPath = FileFullPath(this, path))

        storageHelper.onFolderSelected = { requestCode, folder ->
            try {
                val file: DocumentFile = DocumentFileCompat.fromFullPath(this, "$path/consumption_export.json")!!
                val fileContent = BufferedReader(InputStreamReader(contentResolver.openInputStream(file.uri))).readText()
                val json: JsonArray = gson.fromJson(fileContent, JsonArray::class.java)

                lifecycleScope.launch {
                    drugLogDao.reset()
                    for(logElement in json) {
                        val log = logElement.asJsonObject
                        drugLogDao.addLog(DrugLog(
                            id = log["id"].asInt,
                            name = log["name"].asString,
                            dose = if(!log["dose"].isJsonNull) log["dose"].asDouble else null,
                            unit = if(!log["unit"].isJsonNull) log["unit"].asString else null,
                            notes = if(!log["notes"].isJsonNull) log["notes"].asString else null,
                            timestamp = Date(log["timestamp"].asLong)
                        ))
                    }

                    binding.exportLogStatus.text = "Consumption Log - Import\nStatus: Completed"
                }
            } catch (e: JsonSyntaxException) {
                ErrorDialog(this, "Syntax from JSON file is wrong. Please check export file.")
            }
        }
    }
}

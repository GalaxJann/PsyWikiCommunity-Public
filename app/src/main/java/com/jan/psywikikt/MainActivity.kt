package com.jan.psywikikt

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.ListPopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.jan.psywikikt.adapter.FavouritesAdapter
import com.jan.psywikikt.adapter.FavouritesModel
import com.jan.psywikikt.adapter.MainSearchCustomAdapter
import com.jan.psywikikt.data.DrugFavouriteDao
import com.jan.psywikikt.data.DrugIndexDao
import com.jan.psywikikt.data.DrugIndexTripSitDao
import com.jan.psywikikt.data.DrugUnit
import com.jan.psywikikt.data.DrugUnitDao
import com.jan.psywikikt.data.MainDatabase
import com.jan.psywikikt.databinding.ActivityMainBinding
import com.jan.psywikikt.dialogs.BaseDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.lang.NullPointerException
import java.net.ConnectException
import java.net.UnknownHostException
import kotlin.IndexOutOfBoundsException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // SQLite Database
    private lateinit var drugFavouriteDao: DrugFavouriteDao
    private lateinit var drugIndexDao: DrugIndexDao
    private lateinit var drugIndexTripSitDao: DrugIndexTripSitDao
    private lateinit var drugUnitDao: DrugUnitDao

    // List
    private lateinit var listPopupWindow: ListPopupWindow
    private lateinit var mainSearchCustomAdapter: MainSearchCustomAdapter
    private lateinit var json: JsonObject
    private val substanceList: ArrayList<String> = ArrayList()
    private val substanceClassList: ArrayList<String> = ArrayList()

    // OkHttp Client
    private val client = OkHttpClient()
    private val gson = Gson()

    // SharedPreferences
    private lateinit var searchMode: String
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Build SQLite database
        val db = MainDatabase.getInstance(this)
        drugFavouriteDao = db.drugFavouriteDao()
        drugIndexDao = db.drugIndexDao()
        drugIndexTripSitDao = db.drugIndexTripSitDao()
        drugUnitDao = db.drugUnitDao()

        sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        searchMode = sharedPreferences.getString("searchmode", "NULL").toString()
        val initUnits: Boolean = sharedPreferences.getBoolean("initunits", false)

        if(searchMode == "NULL") {
            editor.putString("searchmode", "apipsy")
            editor.commit()
        } else if(searchMode == "apitripsit") {
            json = JSONTripSit(this@MainActivity).readJSON()
        }
        if(!initUnits) {
            editor.putBoolean("initunits", true)
            editor.apply()
            lifecycleScope.launch {
                drugUnitDao.addMultiUnits(listOf(
                    DrugUnit(0, "g", false),
                    DrugUnit(0, "mg", true),
                    DrugUnit(0, "\u00b5g", false),
                    DrugUnit(0, "ml", false),
                ))
            }
        }

        listPopupWindow = ListPopupWindow(this)

        binding.textInputLayout.setEndIconOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        binding.searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                val thread = Thread {
                    try {
                        getSearchResults(binding.searchBox.text.toString(), searchMode)
                    } catch (e: UnknownHostException) {
                        e.printStackTrace()
                        runOnUiThread { Toast.makeText(applicationContext, "Connection lost!", Toast.LENGTH_SHORT).show() }
                    }
                }
                if(thread.isAlive) {
                    thread.interrupt()
                    thread.start()
                } else {
                    thread.start()
                }
            }
        })

        // Popup list text
        mainSearchCustomAdapter = MainSearchCustomAdapter(this, substanceList, substanceClassList)

        listPopupWindow.apply {
            width = ListPopupWindow.MATCH_PARENT
            anchorView = binding.searchBox
            setAdapter(mainSearchCustomAdapter)
        }

        listPopupWindow.setOnItemClickListener { adapterView, view, i, _ ->
            listPopupWindow.dismiss()
            val intent = Intent(this, if(searchMode == "apitripsit") TripSitInfoActivity::class.java else InfoActivity::class.java)
            intent.putExtra("substance", substanceList[i])
            startActivity(intent)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate my menu
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.itemWebSearch -> {
                val intent = Intent(this, BrowserActivity::class.java)
                startActivity(intent)
            }
            R.id.itemSettings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.itemCredits -> {
                val version: PackageInfo
                if(android.os.Build.VERSION.SDK_INT >= 33) {
                    version = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    version = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
                }

                BaseDialog(this, "Version: ${version.versionName}<br><br>This App was created by<br><b>© 2023 - Jan's Software Development</b><br><br>Discord: asumyt<br><br>" +
                    "Main sources for data:<br><i>PsychonautWiki API and Site</i><br><i>TripSit Factsheet Site</i><br><br>" +
                    "Used libraries:<br><i>Material</i> - Design<br><i>Kotlin</i> - Programming language<br><i>OkHttp3</i> - API-Requests<br><i>Room</i> - SQLite DB abstraction layer<br><i>Gson</i> - JSON Abstraction<br><i>Jsoup</i> - HTML-Parser<br><i>kizitonwose - Calendar View</i><br><br>" +
                    "Icons: <i>Iconify.Design</i> and <i>Google Material Icons</i><br>" +
                    "Fonts: <i>Advent Pro</i> - Light, Regular, Bold" +
                    "Source code: <i>Will be published later, some privacy issues in my GitHub Commits</i>",
                    title = "<h3>Credits</h3>"
                ).show()
            }
            R.id.itemTimer -> {
                val intent = Intent(this, TimerActivity::class.java)
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }
    override fun onResume() {
        // onResume() to reload RecyclerView if favourites changed
        // Favourites RecyclerView
        val listFavourites = binding.listFavourites
        val favModelArrayList: ArrayList<FavouritesModel> = ArrayList<FavouritesModel>()
        for(drug in drugFavouriteDao.getAllFavourites()) {
            favModelArrayList.add(FavouritesModel(drug.name))
        }
        val favouritesAdapter = FavouritesAdapter(this, favModelArrayList)
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        listFavourites.apply {
            adapter = favouritesAdapter
            layoutManager = linearLayoutManager
        }

        // Set search mode (needed if return from Settings to MainActivity)
        searchMode = sharedPreferences.getString("searchmode", "NULL").toString()
        when(searchMode) {
            "standardpsy" -> supportActionBar!!.subtitle = "Psychonaut - Normal Search"
            "apipsy" -> supportActionBar!!.subtitle = "Psychonaut - API Search"
            "apitripsit" -> supportActionBar!!.subtitle = "TripSit - API Search"
        }
        super.onResume()
    }
    private fun getSearchResults(query: String, searchMode: String) {
        if(searchMode == "standardpsy") {
            val request: Request = Request.Builder().url("https://psychonautwiki.org/w/api.php?action=opensearch&format=json&formatversion=2&search=$query&namespace=0%7C102%7C108%7C130%7C140").build()

            if(query != "") {
                val response: Response
                try {
                    response = client.newCall(request).execute()
                } catch (e: ConnectException) {
                    runOnUiThread { Toast.makeText(applicationContext, "Connection lost!", Toast.LENGTH_SHORT).show() }
                    return
                }

                substanceList.clear()
                substanceClassList.clear()
                runOnUiThread { mainSearchCustomAdapter.notifyDataSetChanged() }
                val results: JsonArray = gson.fromJson(response.body!!.string(), JsonArray::class.java)[1].asJsonArray

                // If entries there
                for(i in results) {
                    val entry: String = i.asString
                    if(!entry.contains("/") && !entry.contains(" ")) {
                        substanceList.add(entry)
                        substanceClassList.add("<DISPLAYNONE>")
                    }
                }

                runOnUiThread {
                    try {
                        mainSearchCustomAdapter.notifyDataSetChanged()

                        if(substanceList.size > 0) {
                            listPopupWindow.show()
                        } else {
                            listPopupWindow.dismiss()
                        }
                    } catch (e: IndexOutOfBoundsException) {
                        e.printStackTrace()
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    }
                }
            } else {
                runOnUiThread { listPopupWindow.dismiss() }
            }
        } else if(searchMode == "apipsy") {
            lifecycleScope.launch(Dispatchers.IO) {
                if(query != "") {
                    val searchResults = drugIndexDao.getSearchDrugs("%$query%")
                    val totalCount: Int = drugIndexDao.getCountDrugs()

                    runOnUiThread {
                        if(totalCount == 0) {
                            Toast.makeText(applicationContext, "Bitte update die Datenbank in den Einstellungen", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                            startActivity(intent)
                        }

                        // Moved inside the UI-Thread to prevent IllegalStateException and IndexOutOfBoundsException
                        substanceList.clear()
                        substanceClassList.clear()
                        for(result in searchResults) {
                            // Add drug name to AutoComplete
                            substanceList.add(result.name)
                            substanceClassList.add(if(result.psychoactiveClass != null) result.psychoactiveClass!! else "<DISPLAYNONE>")
                        }

                        if(substanceList.size == substanceClassList.size) mainSearchCustomAdapter.notifyDataSetChanged()
                        if(substanceList.size > 0 && substanceClassList.size > 0 && substanceList.size == substanceClassList.size) {
                            listPopupWindow.show()
                        } else {
                            listPopupWindow.dismiss()
                        }
                    }
                } else {
                    runOnUiThread { listPopupWindow.dismiss() }
                }
            }
        } else if(searchMode == "apitripsit") {
            if(query != "") {
                lifecycleScope.launch(Dispatchers.IO) {
                    val searchResults = drugIndexTripSitDao.getSearchDrugs("%$query%")
                    val totalCount: Int = drugIndexTripSitDao.getCountDrugs()

                    if(!this@MainActivity::json.isInitialized) {
                        json = JSONTripSit(this@MainActivity).readJSON()
                    }

                    runOnUiThread {
                        if(totalCount == 0) {
                            Toast.makeText(applicationContext, "Bitte update die Datenbank in den Einstellungen", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                            startActivity(intent)
                        }

                        // Moved inside the UI-Thread to prevent IllegalStateException and IndexOutOfBoundsException
                        substanceList.clear()
                        substanceClassList.clear()
                        for(result in searchResults) {
                            substanceList.add(result.name)
                            if(!json.getAsJsonObject(result.idName).has("categories")) {
                                substanceClassList.add("<DISPLAYNONE>")
                            } else {
                                substanceClassList.add(json.getAsJsonObject(result.idName).getAsJsonArray("categories").joinToString(", ").replace("\"", ""))
                            }
                        }

                        if(substanceList.size == substanceClassList.size) mainSearchCustomAdapter.notifyDataSetChanged()

                        if(substanceList.size > 0 && substanceClassList.size > 0 && substanceList.size == substanceClassList.size) {
                            listPopupWindow.show()
                        } else {
                            listPopupWindow.dismiss()
                        }
                    }
                }
            } else {
                runOnUiThread { listPopupWindow.dismiss() }
            }
        }
    } /*private fun testDB() {
        lifecycleScope.launch(Dispatchers.IO) {
            // val favourite = drugFavouriteDao.getFavourite("ENTRY2")
            // Log.i("QUERY", favourite.toString())

            /*
            // Insert
            Log.i("MyTAG", "***** INSERTING 3 Books *****")
            bookDao.insertBook(Book(0, "Learn Java", "Jan"))
            bookDao.insertBook(Book(0, "Learn Python", "Michael"))
            bookDao.insertBook(Book(0, "Learn Perl", "Philip"))
            Log.i("MyTAG", "***** INSERTED 3 Books *****")
            // Query
            var books = bookDao.getAllBooks()
            Log.i("MyTAG", "***** ${books.size} books there")
            for(book in books) {
                Log.i("MyTAG", "id: ${book.id} name: ${book.name} author: ${book.author}")
            }

            // Update
            Log.i("MyTAG", "***** UPDATING 3 Books *****")
            bookDao.updateBook(Book(1, "Learn Python - Updated", "Michael2"))

            // Delete
            Log.i("MyTAG", "***** DELETING 3 Books *****")
            bookDao.deleteBook(Book(2, "Learn Perl", "Philip"))
            */
        }
    }*/
}
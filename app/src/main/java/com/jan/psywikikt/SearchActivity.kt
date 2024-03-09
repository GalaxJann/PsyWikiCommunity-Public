package com.jan.psywikikt

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_FILLED
import com.jan.psywikikt.adapter.SearchResultsAdapter
import com.jan.psywikikt.data.DrugIndex
import com.jan.psywikikt.data.DrugIndexDao
import com.jan.psywikikt.data.DrugIndexTripSitDao
import com.jan.psywikikt.data.MainDatabase
import com.jan.psywikikt.databinding.ActivitySearchBinding
import com.jan.psywikikt.dialogs.BaseDialog
import com.jan.psywikikt.dialogs.ErrorDialog
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding

    // SQLite Database
    private lateinit var drugIndexDao: DrugIndexDao
    private lateinit var drugIndexTripSitDao: DrugIndexTripSitDao

    private val filterList: ArrayList<String> = arrayListOf("Name", "Category")
    private val resultsList: ArrayList<String> = ArrayList()
    private val resultsSubList: ArrayList<String> = ArrayList()
    private lateinit var searchResultsAdapter: SearchResultsAdapter

    // SharedPreferences
    private lateinit var searchMode: String
    private lateinit var sPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Database init
        val db = MainDatabase.getInstance(this)
        drugIndexDao = db.drugIndexDao()
        drugIndexTripSitDao = db.drugIndexTripSitDao()

        sPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        searchMode = sPref.getString("searchmode", "NULL").toString()

        supportActionBar!!.subtitle = "Search - Advanced - ${if(searchMode == "apipsy") "Psychonaut" else if(searchMode == "apitripsit") "TripSit" else "Unsupported"}"

        // TODO: TripSit currently deactivated; get it to work
        if(searchMode == "standardpsy" || searchMode == "apitripsit") {
            ErrorDialog(this, "Not supported for this <b>Search mode</b>! Please change in <b>Settings</b>", onPositive = {_, _ ->
                finishAndRemoveTask()
            }).show()
        }

        searchResultsAdapter = SearchResultsAdapter(this, resultsList, resultsSubList)
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.listSearchResults.apply {
            adapter = searchResultsAdapter
            layoutManager = linearLayoutManager
        }

        binding.button.setOnClickListener {
            TransitionManager.beginDelayedTransition(binding.cardView, AutoTransition())

            binding.linearLayout.visibility = if(binding.linearLayout.visibility == LinearLayout.GONE) LinearLayout.VISIBLE else LinearLayout.GONE
        }

        val filterAdapter: ArrayAdapter<String> = ArrayAdapter<String>(this, androidx.appcompat.R.layout.select_dialog_item_material, filterList)

        binding.addFilter.setOnClickListener {
            if(binding.listFilters.children.count() < filterList.size) {
                addFilterView(binding.listFilters, filterAdapter)
            } else {
                ErrorDialog(this, "Maximum number of filters reached!").show()
            }
        }
        binding.btnDropdownFilter.setAdapter(filterAdapter)

        binding.btnSearch.setOnClickListener {
            var first = true
            var query = "SELECT * FROM ${if(searchMode == "apipsy") "substance_index" else "substance_index_tripsit"} WHERE"
            val listFilter: MutableList<String> = mutableListOf()

            for(linear in binding.listFilters.children) {
                val editText: EditText = ((linear as LinearLayout).children.first() as TextInputLayout).editText!!
                val filter = (linear.children.elementAt(1) as TextInputLayout).editText as MaterialAutoCompleteTextView

                // Check for double selected same filter
                if(filter.text.toString() in listFilter) {
                    ErrorDialog(this@SearchActivity, "You've used the same filter twice or more.").show()
                    return@setOnClickListener
                } else {
                    listFilter.add(filter.text.toString())
                }

                // Validation - Bad character in EditText
                if(!editText.text.contains(Regex("^[a-zA-Z0-9-,.]+\$"))) {
                    ErrorDialog(this, "Bad characters entered.\nAllowed: a-z, A-Z, 0-9, -, ., \",\"").show()
                    return@setOnClickListener
                }

                when(filter.text.toString()) {
                    "Name" -> query += "${if(!first) " AND" else ""} (name LIKE '%${editText.text}%' OR common_names LIKE '%${editText.text}%')"
                    "Category" -> query += "${if(!first) " AND" else ""} psychoactive_class LIKE '%${editText.text}%'"
                }

                if(first) first = false
            }

            getSearchResults(query, searchMode)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getSearchResults(query: String, searchMode: String) {
        resultsList.clear()
        resultsSubList.clear()

        when(searchMode) {
            "apipsy" -> {
                lifecycleScope.launch {
                    val searchResults: List<DrugIndex> = drugIndexDao.getFilterDrugs(SimpleSQLiteQuery(query))

                    if(searchResults.isEmpty()) {
                        BaseDialog(this@SearchActivity, "No search results found!", title = "Search").show()
                    }

                    for(i in searchResults) {
                        resultsList.add(i.name)
                        resultsSubList.add(if(i.psychoactiveClass != null) i.psychoactiveClass!! else "Class not found")
                    }

                    runOnUiThread { searchResultsAdapter.notifyDataSetChanged() }
                }
            } "apitripsit" -> {}
        }
    }

    private fun addFilterView(view: LinearLayout, filterAdapter: ArrayAdapter<String>) {
        // Create filter view
        val filterLinear = LinearLayout(this)
        val textInputLayout = TextInputLayout(this)
        val textInputFilter = TextInputLayout(ContextThemeWrapper(this, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_FilledBox_Dense_ExposedDropdownMenu))
        val searchBar = TextInputEditText(textInputLayout.context)
        val dropdownFilter = MaterialAutoCompleteTextView(ContextThemeWrapper(this, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_FilledBox_Dense_ExposedDropdownMenu))

        filterLinear.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
        }

        textInputLayout.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            hint = "Search"
            boxBackgroundMode = BOX_BACKGROUND_FILLED
            startIconDrawable = ContextCompat.getDrawable(this@SearchActivity, R.drawable.ic_pill)
        }

        textInputFilter.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f)
            hint = "Filter"
            boxBackgroundMode = BOX_BACKGROUND_FILLED
            startIconDrawable = ContextCompat.getDrawable(this@SearchActivity, R.drawable.baseline_filter_list_24)
        }

        searchBar.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            isSingleLine = true
        }

        dropdownFilter.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            inputType = EditorInfo.TYPE_NULL
            setAdapter(filterAdapter)
        }

        filterLinear.addView(textInputLayout)
        filterLinear.addView(textInputFilter)
        textInputLayout.addView(searchBar)
        textInputFilter.addView(dropdownFilter)
        view.addView(filterLinear)

        // Margin params
        val filterLinearMarginParams = filterLinear.layoutParams as ViewGroup.MarginLayoutParams
        val textInputMarginParams = textInputLayout.layoutParams as ViewGroup.MarginLayoutParams
        val textFilterMarginParams = textInputFilter.layoutParams as ViewGroup.MarginLayoutParams
        filterLinearMarginParams.setMargins(0, 0, 0, resources.getDimension(R.dimen.search_input_margin).toInt())
        textInputMarginParams.setMargins(0, 0, resources.getDimension(R.dimen.search_input_margin).toInt(), 0)
        textFilterMarginParams.setMargins(resources.getDimension(R.dimen.search_input_margin).toInt(), 0, 0, 0)

        filterLinear.layoutParams = filterLinearMarginParams
        textInputLayout.layoutParams = textInputMarginParams
        textInputFilter.layoutParams = textFilterMarginParams
    }
}
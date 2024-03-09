package com.jan.psywikikt

import android.annotation.SuppressLint
import android.app.ActionBar.LayoutParams
import android.os.Bundle
import android.util.TypedValue
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.textview.MaterialTextView
import com.google.gson.JsonArray
import com.jan.psywikikt.databinding.ActivityEffectsBinding
import com.jan.psywikikt.dialogs.ErrorDialog
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException

class EffectsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEffectsBinding

    private lateinit var drugName: String

    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEffectsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var cognitiveEffects: JsonArray?
        var physicalEffects: JsonArray?
        var visualEffects: JsonArray?
        var auditoryEffects: JsonArray?
        var multiSensoryEffects: JsonArray?
        var transpersonalEffects: JsonArray?
        var disconnectiveEffects: JsonArray?

        val availableEffects: MutableList<String> = mutableListOf()

        drugName = intent.extras!!.getString("substance").toString()
        supportActionBar!!.subtitle = drugName

        // Get available effects
        Thread {
            try {
                val htmlpsy = HTMLPsy(drugName)

                cognitiveEffects = htmlpsy.getSubjectiveEffectsAsJSON("Cognitive_effects")
                physicalEffects = htmlpsy.getSubjectiveEffectsAsJSON("Physical_effects")
                visualEffects = htmlpsy.getSubjectiveEffectsAsJSON("Visual_effects")
                auditoryEffects = htmlpsy.getSubjectiveEffectsAsJSON("Auditory_effects")
                multiSensoryEffects = htmlpsy.getSubjectiveEffectsAsJSON("Multi-sensory_effects")
                transpersonalEffects = htmlpsy.getSubjectiveEffectsAsJSON("Transpersonal_effects")
                disconnectiveEffects = htmlpsy.getSubjectiveEffectsAsJSON("Disconnective_effects")

                val link: String? = htmlpsy.getChemStructureSVG()

                runOnUiThread {
                    try {
                        val systematicName: String = htmlpsy.getSystematicName()!!
                        val textview = MaterialTextView(this)
                        val textviewMargin = resources.getDimension(R.dimen.main_card_margin).toInt()
                        val textviewParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                        textviewParams.setMargins(textviewMargin, 0, textviewMargin, 0)

                        textview.apply {
                            typeface = ResourcesCompat.getFont(context, R.font.advent_pro_regular)
                            text = systematicName
                            layoutParams = textviewParams
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.effects_text_size))
                        }

                        binding.linearEffectsParent.addView(textview, 1)
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    } catch (e: IndexOutOfBoundsException) {
                        e.printStackTrace()
                    }

                    binding.webview.apply {
                        clearCache(true)
                        clearFormData()
                        clearHistory()
                        clearMatches()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false
                        webViewClient = CustomWebClient()
                        webChromeClient = WebChromeClient()
                        setInitialScale(100)
                        loadUrl(link!!)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ErrorDialog(this, "Unbekannter Fehler: ${e.message}").show()
                return@Thread
            }

            // Add entries to "Select Effects" dropdown
            if(cognitiveEffects != null) availableEffects.add("Cognitive effects")
            if(physicalEffects != null) availableEffects.add("Physical effects")
            if(visualEffects != null) availableEffects.add("Visual effects")
            if(auditoryEffects != null) availableEffects.add("Auditory effects")
            if(multiSensoryEffects != null) availableEffects.add("Multisensory effects")
            if(transpersonalEffects != null) availableEffects.add("Transpersonal effects")
            if(disconnectiveEffects != null) availableEffects.add("Disconnective effects")

            val effectsSelectorAdapter: ArrayAdapter<String> = ArrayAdapter<String>(this, androidx.appcompat.R.layout.select_dialog_item_material, availableEffects)
            runOnUiThread {
                binding.selectorEffects.apply {
                    setAdapter(effectsSelectorAdapter)
                }
                binding.selectorEffectsMenu.hint = "Select effects"
                binding.titleEffects.text = "Select effects"

                binding.selectorEffects.setOnItemClickListener { adapterView, view, pos, id ->
                    binding.titleEffects.text = availableEffects[pos]

                    // Clear effects view
                    binding.linearEffects.removeAllViews()

                    when(availableEffects[pos]) {
                        "Cognitive effects" -> addEffectToLayout(cognitiveEffects)
                        "Physical effects" -> addEffectToLayout(physicalEffects)
                        "Visual effects" -> addEffectToLayout(visualEffects)
                        "Auditory effects" -> addEffectToLayout(auditoryEffects)
                        "Multisensory effects" -> addEffectToLayout(multiSensoryEffects)
                        "Transpersonal effects" -> addEffectToLayout(transpersonalEffects)
                        "Disconnective effects" -> addEffectToLayout(disconnectiveEffects)
                    }
                }
            }
        }.start()
    } private fun addEffectToLayout(effects: JsonArray?) {
        for(i in effects!!) {
            // LinearLayout
            val linearEffect = LinearLayout(this)
            val linearParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            linearParams.setMargins(8, 8, 8, 8)
            linearEffect.apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = linearParams
            }

            // MaterialTextViews
            val effectTitle = MaterialTextView(this)
            val titleParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            effectTitle.apply {
                typeface = ResourcesCompat.getFont(context, R.font.advent_pro_bold)
                layoutParams = titleParams
                text = i.asJsonObject["name"].asString
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.effects_title_size))
            }


            // Append view to LinearLayout
            linearEffect.addView(effectTitle)

            // If effect description is available
            if(i.asJsonObject["description"].asString != "") {
                val effectDescription = MaterialTextView(this)
                val descParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                descParams.setMargins(16, 16, 16, 16)

                effectDescription.apply {
                    typeface = ResourcesCompat.getFont(context, R.font.advent_pro_regular)
                    layoutParams = descParams
                    text = i.asJsonObject["description"].asString
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.effects_text_size))
                }
                linearEffect.addView(effectDescription)
            }
            binding.linearEffects.addView(linearEffect)
        }
    } private class CustomWebClient() : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            view!!.evaluateJavascript("document.getElementsByTagName(\"svg\")[0].style.filter = \"invert(1)\"; document.getElementsByTagName(\"svg\")[0].style.background = \"#121212\";", null)

            super.onPageFinished(view, url)
        }
    }
}
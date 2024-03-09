package com.jan.psywikikt

import android.util.Log
import com.google.gson.JsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException


class HTMLTripSit(drugName: String) {
    private val drugName: String
    private val document: Document

    init {
        this.drugName = drugName.replace(" ", "-").replace("\u03B1", "A")
        this.document = Jsoup.connect("https://drugs.tripsit.me/${this.drugName}").get()
    }

    fun getCategories(): JsonObject {
        val jsonObject: JsonObject = extractCategories(
            listOf(
                "bs-callout-dissociative",
                "bs-callout-stimulant",
                "bs-callout-psychedelic",
                "bs-callout-empathogen",
                "bs-callout-opioid",
                "bs-callout-habit-forming",
                "bs-callout-depressant",
                "bs-callout-common",
                "bs-callout-research-chemical",
                "bs-callout-tentative",
                "bs-callout-inactive",
                "bs-callout-barbiturate",
                "bs-callout-benzodiazepine",
                "bs-callout-nootropic"
            ),
            document = document
        )

        // TODO: Colors for Chips
        //val hashMap = hashMapOf<String, String>()
        //hashMap["bs-callout-common"] = "#420420"

        return jsonObject
    }

    fun getSummary(): String {
        val summary: Element = document
            .getElementsByClass("mainText")[0]
            .getElementsByClass("layoutPosition")[0]

        return summary.ownText()
    }

    fun getDosageAsJSON(): JsonObject? {
        try {
            val roas: Elements = document
                .getElementsByClass("flexDose")[0]
                .getElementsByClass("textFormat")
            val jsonObject = JsonObject()

            for(box in roas) {
                val json = JsonObject()

                val roa: String = box.getElementsByTag("th")[0].ownText()
                val dosages: Elements = box.getElementsByTag("tbody")[0].children()

                for(dosage in dosages) {
                    if(dosage.childrenSize() <= 1) {
                        throw NullPointerException("Get dosage error occured!")
                    } else {
                        json.addProperty(dosage.children()[0].ownText(), dosage.children()[1].ownText())
                    }
                }

                jsonObject.add(roa, json)
            }

            return jsonObject
        } catch(e: NullPointerException) {
            e.printStackTrace()
            return null
        }
    }

    fun getDurationAsJSON(): JsonObject? {
        try {
            val roas: Elements = document
                .getElementsByClass("flexDuration")[0]
                .getElementsByClass("textFormat")[0]
                .children()
            val jsonObject = JsonObject()

            for(box in roas) {
                val json = JsonObject()

                val roa: String = box.getElementsByTag("th")[0].ownText()
                var durations: Elements
                try {
                    durations = box.getElementsByTag("tbody")[0].children()
                } catch(e: IndexOutOfBoundsException) {
                    durations = box.getElementsByTag("tr")
                }

                for(item in durations) {
                    if(item.childrenSize() <= 1) {
                        continue
                    }

                    json.addProperty(item.children()[0].ownText(), item.children()[1].ownText())
                }

                jsonObject.add(roa, json)
            }

            return jsonObject
        } catch(e: NullPointerException) {
            e.printStackTrace()
            return null
        }
    }

    private fun extractCategories(categories: List<String>, document: Document): JsonObject {
        val jsonObject = JsonObject()

        for (i in categories) {
            val html: Elements = document.getElementsByClass(i)

            try {
                if(html.size > 0) {
                    val json = JsonObject()

                    val htmlTitle: String = html[0].getElementsByTag("h4")[0].getElementsByTag("a")[0].ownText()
                    val htmlText: String = html[0].getElementsByTag("p")[0].ownText()

                    json.addProperty("title", htmlTitle)
                    json.addProperty("text", htmlText)

                    jsonObject.add(i, json)
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("Error", "One category box has wrong format, skipping...")
            }
        }

        return jsonObject
    }
}
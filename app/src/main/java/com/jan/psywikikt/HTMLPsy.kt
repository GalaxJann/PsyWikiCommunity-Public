package com.jan.psywikikt

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import java.lang.NullPointerException

class HTMLPsy(drugName: String) {
    private val drugName: String
    private val document: Document

    init {
        this.drugName = drugName
        this.document = Jsoup.connect("https://psychonautwiki.org/wiki/$drugName").get()
    }

    fun getSubjectiveEffectsAsJSON(effectType: String): JsonArray? {
        // Could be: Physical_effects, Cognitive_effects, Visual_effects, Auditory_effects, Multi-sensory_effects, Transpersonal_effects, Disconnective_effects

        try {
            val effects: Elements = document
                .getElementById(effectType)!!
                .parent()!!
                .parent()!!
                .getElementsByClass("list-item")[0]
                .getElementsByTag("ul")[0].children()

            val array = JsonArray()

            // TODO: Get sub-effects (e.g. Delusion --> Delusions of sobriety, Compulsive redosing)
            for(i in effects) {
                val effect = JsonObject()
                var description = ""

                // Get effect description with <a>'s - get #text node of child elements
                // HTML-Structure: <li>(nodes[a,i,#text])</li>
                // TODO: Make <a> and <i> text bold
                for(i in i.childNodes()) {
                    when(i.nodeName()) {
                        // HTML-Structure: <a>#text</a>
                        "a" -> if(i.firstChild() is TextNode) description += (i.firstChild() as TextNode).text()
                        // HTML-Structure: <i><a>#text</a></i>
                        "i" -> if(i.firstChild()!!.firstChild() is TextNode) description += (i.firstChild()!!.firstChild() as TextNode).text()
                        // Output every #text node
                        "#text" -> if(i is TextNode) description += i.text()
                    }
                }

                effect.addProperty("name", i.getElementsByTag("a").first()?.ownText())
                effect.addProperty("link", i.getElementsByTag("a").first()?.attr("href"))
                effect.addProperty("description", description)
                array.add(effect)
            }

            return array
        } catch (e: NullPointerException) {
            // Effect type not found
            e.printStackTrace()
            return null
        }
    }

    fun getChemStructureSVG(): String? {
        try {
            val image: String = document
                .getElementsByClass("Image")[0]
                .getElementsByTag("a")[0]
                .attr("href")

            val imgDocument: Document = Jsoup.connect("https://psychonautwiki.org$image").get()
            return imgDocument
                .getElementById("file")!!
                .children()[0]
                .attr("href")
        } catch (e: NullPointerException) {
            // Image not found
            e.printStackTrace()
            return null
        }
    }

    fun getSystematicName(): String? {
        try {
            val systematicName: String = document
                .getElementsContainingOwnText("Systematic name")[0]
                .parent()!!
                .getElementsByClass("RowValues")[0]
                .getElementsByTag("i")[0]
                .ownText()

            return systematicName
        } catch (e: NullPointerException) {
            // Systematic name not found
            e.printStackTrace()
            return null
        }
    }
}
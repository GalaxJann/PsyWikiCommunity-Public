package com.jan.psywikikt

import android.app.Activity
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jan.psywikikt.dialogs.ErrorDialog
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.Throws

class JSONTripSit(activity: Activity) {
    private val activity: Activity
    private val gson = Gson()
    private val file: File
    private var json: JsonObject? = null

    init {
        this.activity = activity
        file = File(activity.filesDir, "tripsit_sub.json")
    }

    @Throws(FileNotFoundException::class, IllegalArgumentException::class)
    private fun read(drugName: String? = null): JsonObject {
        var json: JsonObject?

        if(file.exists()) {
            val fileContent = file.bufferedReader().readText()
            json = gson.fromJson(fileContent, JsonObject::class.java)

            if(drugName != null) {
                if(!json!!.has(drugName)) {
                    throw IllegalArgumentException("Substance name not found in JSON!")
                }

                json = json.getAsJsonObject(drugName)
            }
        } else {
            throw FileNotFoundException("JSON file not found!")
        }

        this.json = json
        return json
    }

    @Throws(FileNotFoundException::class)
    fun readJSON(): JsonObject {
        return read()
    }

    @Throws(FileNotFoundException::class, IllegalArgumentException::class)
    fun readJSON(drugName: String): JsonObject {
        return read(drugName)
    }

    @Throws(IOException::class)
    fun writeJSON(json: String) {
        val jsonFileContent: String = json

        try {
            val outputStream = FileOutputStream(File(activity.filesDir, "tripsit_sub.json"))
            outputStream.write(jsonFileContent.toByteArray())
            outputStream.close()
        } catch(e: IOException) {
            ErrorDialog(activity, "Unknown error, please report to Developer: $e<br><br>Discord: asumyt").show()
        }
    }
}
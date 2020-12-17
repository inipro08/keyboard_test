package com.inis.keyboard_test

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val context = this
    val keyMaps = mapOf(
        R.xml.number_en to "English",
        R.xml.number_vn to "VietNamese"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        val keyMapsRev: HashMap<String, Int> = HashMap()
        keyMaps.forEach { keyMapsRev[it.value] = it.key }

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        var prf = preferences.getString("languages", null)
        var setLangs = if (prf != null) JSONObject(prf) else JSONObject(keyMapsRev as Map<*, *>)

        for ((xml_id, lang) in keyMaps) {
            setLangs.has(lang)
            prf = preferences.getString("languages", null)
            setLangs = if (prf != null) JSONObject(prf) else JSONObject(keyMapsRev as Map<*, *>)
            setLangs.put(lang, xml_id)
            preferences.edit().putString("languages", setLangs.toString()).apply()
            Log.d("LOL", setLangs.toString())
        }
    }
}
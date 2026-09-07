package com.example.memscan.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.memscan.model.DataType
import com.example.memscan.model.FreezeProfile
import com.example.memscan.model.ProfileVariable
import com.example.memscan.model.ScanStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

class ProfileManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("memscan_profiles", Context.MODE_PRIVATE)

    private val _profiles = MutableStateFlow<List<FreezeProfile>>(emptyList())
    val profiles: StateFlow<List<FreezeProfile>> = _profiles.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        val jsonStr = prefs.getString("saved_profiles", null)
        val list = mutableListOf<FreezeProfile>()

        // Add built-in template for YouTube Studio
        val defaultStudioProfile = FreezeProfile(
            id = "default_youtube_studio",
            name = "YouTube Studio Analytics Boost",
            targetPackage = "com.google.android.apps.youtube.creator",
            variables = listOf(
                ProfileVariable("Total Views (28d)", "12450", DataType.INT32, ScanStrategy.NUMERIC_EXACT),
                ProfileVariable("Display Views", "12.5K", DataType.STRING_UTF16, ScanStrategy.STRING_UTF16),
                ProfileVariable("Subscribers (+28d)", "427", DataType.INT32, ScanStrategy.NUMERIC_EXACT),
                ProfileVariable("Est. Revenue ($)", "31.42", DataType.FLOAT32, ScanStrategy.NUMERIC_FLOAT)
            )
        )
        list.add(defaultStudioProfile)

        if (jsonStr != null) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    if (id == defaultStudioProfile.id) continue
                    val name = obj.getString("name")
                    val pkg = obj.getString("targetPackage")
                    val varsArr = obj.getJSONArray("variables")
                    val vars = mutableListOf<ProfileVariable>()
                    for (j in 0 until varsArr.length()) {
                        val vObj = varsArr.getJSONObject(j)
                        vars.add(
                            ProfileVariable(
                                label = vObj.getString("label"),
                                targetValueStr = vObj.getString("targetValueStr"),
                                dataType = DataType.valueOf(vObj.getString("dataType")),
                                preferredStrategy = ScanStrategy.valueOf(vObj.optString("preferredStrategy", ScanStrategy.NUMERIC_EXACT.name))
                            )
                        )
                    }
                    list.add(FreezeProfile(id, name, pkg, vars))
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        _profiles.value = list
    }

    fun saveProfile(profile: FreezeProfile) {
        _profiles.update { current ->
            val filtered = current.filterNot { it.id == profile.id }
            filtered + profile
        }
        persist()
    }

    fun deleteProfile(id: String) {
        _profiles.update { it.filterNot { item -> item.id == id } }
        persist()
    }

    private fun persist() {
        try {
            val array = JSONArray()
            for (p in _profiles.value) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("targetPackage", p.targetPackage)
                val varsArr = JSONArray()
                for (v in p.variables) {
                    val vObj = JSONObject()
                    vObj.put("label", v.label)
                    vObj.put("targetValueStr", v.targetValueStr)
                    vObj.put("dataType", v.dataType.name)
                    vObj.put("preferredStrategy", v.preferredStrategy.name)
                    varsArr.put(vObj)
                }
                obj.put("variables", varsArr)
                array.put(obj)
            }
            prefs.edit().putString("saved_profiles", array.toString()).apply()
        } catch (e: Exception) {
            // Log or ignore
        }
    }
}

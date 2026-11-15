package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * BEKASERVICE — სერვისცენტრის ფიზიკური მისამართის, Google Maps Share ბმულისა და
 * ლოკაციის ცენტრალიზებული მართვის სისტემა.
 *
 * უზრუნველყოფს:
 * 1. Google Maps-ის გაზიარების (Share Link) ბმულების, კოორდინატებისა და ფიზიკური მისამართის მართვას
 * 2. ლოკალურ ქეშირებას SharedPreferences-ში (ოფლაინ მუშაობისთვის)
 * 3. რეალურ დროში სინქრონიზაციას Firebase Firestore-ში: app_config/service_center_location
 * 4. Google Maps აპლიკაციისა და ბრაუზერის უსაფრთხო გამოძახებას მომხმარებლის სანავიგაციო მენიუდან.
 */

// ==============================================================================
// 1. მონაცემთა მოდელი (Data Model)
// ==============================================================================

data class ServiceCenterLocationConfig(
    val centerName: String = "BEKASERVICE",
    val physicalAddress: String = "კოსტავას N10, ახალციხე",
    val googleMapsUrl: String = "https://maps.app.goo.gl/w8G6k689mZqU8Z488",
    val latitude: Double = 41.6389,
    val longitude: Double = 42.9861,
    val phone: String = "+995 577 00 11 22",
    val workingHours: String = "10:00 – 20:00 (ყოველდღე)",
    val landmark: String = "ცენტრალურ მოედანთან, შესასვლელი ქუჩიდან",
    val services: List<String> = listOf(
        "ტელეფონის აქსესუარები",
        "პროგრამული მომსახურება",
        "ტელეფონის პროგრამული აღდგენა და განახლება",
        "პროგრამირება და განბლოკვა",
        "ტელეფონების შეკეთება",
        "სხვა ტექნიკური მომსახურება"
    ),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val updatedBy: String = "beqa1994@gmail.com"
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("centerName", centerName)
            put("physicalAddress", physicalAddress)
            put("googleMapsUrl", googleMapsUrl)
            put("latitude", latitude)
            put("longitude", longitude)
            put("phone", phone)
            put("workingHours", workingHours)
            put("landmark", landmark)
            val srvArray = JSONArray()
            services.forEach { srvArray.put(it) }
            put("services", srvArray)
            put("updatedAtMillis", updatedAtMillis)
            put("updatedBy", updatedBy)
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "configType" to "service_center_location",
            "centerName" to centerName,
            "physicalAddress" to physicalAddress,
            "googleMapsUrl" to googleMapsUrl,
            "latitude" to latitude,
            "longitude" to longitude,
            "phone" to phone,
            "workingHours" to workingHours,
            "landmark" to landmark,
            "services" to services,
            "updatedAtMillis" to updatedAtMillis,
            "updatedBy" to updatedBy
        )
    }

    companion object {
        val DEFAULT = ServiceCenterLocationConfig()

        fun fromJsonObject(json: JSONObject): ServiceCenterLocationConfig {
            val srvArray = json.optJSONArray("services") ?: JSONArray()
            val srvList = mutableListOf<String>()
            for (i in 0 until srvArray.length()) {
                val s = srvArray.optString(i, "")
                if (s.isNotBlank()) srvList.add(s)
            }
            return ServiceCenterLocationConfig(
                centerName = json.optString("centerName", "BEKASERVICE").ifBlank { "BEKASERVICE" },
                physicalAddress = json.optString("physicalAddress", "კოსტავას N10, ახალციხე").ifBlank { "კოსტავას N10, ახალციხე" },
                googleMapsUrl = json.optString("googleMapsUrl", "https://maps.app.goo.gl/w8G6k689mZqU8Z488").ifBlank { "https://maps.app.goo.gl/w8G6k689mZqU8Z488" },
                latitude = json.optDouble("latitude", 41.6389),
                longitude = json.optDouble("longitude", 42.9861),
                phone = json.optString("phone", "+995 577 00 11 22").ifBlank { "+995 577 00 11 22" },
                workingHours = json.optString("workingHours", "10:00 – 20:00 (ყოველდღე)").ifBlank { "10:00 – 20:00 (ყოველდღე)" },
                landmark = json.optString("landmark", "ცენტრალურ მოედანთან, შესასვლელი ქუჩიდან"),
                services = if (srvList.isNotEmpty()) srvList else DEFAULT.services,
                updatedAtMillis = json.optLong("updatedAtMillis", System.currentTimeMillis()),
                updatedBy = json.optString("updatedBy", "beqa1994@gmail.com")
            )
        }

        fun fromMap(map: Map<*, *>): ServiceCenterLocationConfig {
            val rawServices = map["services"] as? List<*> ?: emptyList<Any>()
            val srvList = rawServices.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
            return ServiceCenterLocationConfig(
                centerName = map["centerName"]?.toString()?.ifBlank { "BEKASERVICE" } ?: "BEKASERVICE",
                physicalAddress = map["physicalAddress"]?.toString()?.ifBlank { "კოსტავას N10, ახალციხე" } ?: "კოსტავას N10, ახალციხე",
                googleMapsUrl = map["googleMapsUrl"]?.toString()?.ifBlank { "https://maps.app.goo.gl/w8G6k689mZqU8Z488" } ?: "https://maps.app.goo.gl/w8G6k689mZqU8Z488",
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 41.6389,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: 42.9861,
                phone = map["phone"]?.toString()?.ifBlank { "+995 577 00 11 22" } ?: "+995 577 00 11 22",
                workingHours = map["workingHours"]?.toString()?.ifBlank { "10:00 – 20:00 (ყოველდღე)" } ?: "10:00 – 20:00 (ყოველდღე)",
                landmark = map["landmark"]?.toString() ?: "ცენტრალურ მოედანთან, შესასვლელი ქუჩიდან",
                services = if (srvList.isNotEmpty()) srvList else DEFAULT.services,
                updatedAtMillis = (map["updatedAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedBy = map["updatedBy"]?.toString() ?: "beqa1994@gmail.com"
            )
        }
    }
}

// ==============================================================================
// 2. მენეჯერი (ServiceCenterLocationManager)
// ==============================================================================

object ServiceCenterLocationManager {
    private const val PREFS_NAME = "bekaservice_service_center_prefs"
    private const val KEY_LOCATION_JSON = "key_service_center_location_json_v1"
    private const val KEY_HAS_CUSTOM = "key_has_custom_service_center_v1"

    var currentConfig by mutableStateOf(ServiceCenterLocationConfig.DEFAULT)
        private set

    val configStateVersion = mutableIntStateOf(0)

    fun init(context: Context) {
        val loaded = loadConfig(context)
        currentConfig = loaded
        configStateVersion.intValue++
    }

    fun loadConfig(context: Context): ServiceCenterLocationConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasCustom = prefs.getBoolean(KEY_HAS_CUSTOM, false)
        val jsonStr = prefs.getString(KEY_LOCATION_JSON, null)

        if (!hasCustom || jsonStr.isNullOrBlank()) {
            return ServiceCenterLocationConfig.DEFAULT
        }

        return try {
            val obj = JSONObject(jsonStr)
            ServiceCenterLocationConfig.fromJsonObject(obj)
        } catch (e: Exception) {
            Log.e("ServiceCenterLocation", "Error parsing cached service center config", e)
            ServiceCenterLocationConfig.DEFAULT
        }
    }

    fun saveLocalConfig(context: Context, config: ServiceCenterLocationConfig) {
        try {
            val root = config.toJsonObject()
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LOCATION_JSON, root.toString())
                .putBoolean(KEY_HAS_CUSTOM, true)
                .commit()

            currentConfig = config
            configStateVersion.intValue++
        } catch (e: Exception) {
            Log.e("ServiceCenterLocation", "Error saving local service center config", e)
        }
    }

    fun updateFromCloud(context: Context, config: ServiceCenterLocationConfig) {
        saveLocalConfig(context, config)
    }

    /**
     * Google Maps-ის გახსნის უნიფიცირებული მეთოდი.
     * მხარს უჭერს:
     * - Google Maps Share Link-ებს: https://maps.app.goo.gl/...
     * - ჩვეულებრივ Google Maps URL-ებს: https://goo.gl/maps/... ან https://www.google.com/maps/...
     * - კოორდინატებს: "41.6389, 42.9861"
     * - geo: სქემას
     */
    fun resolveGoogleMapsUri(rawInput: String, fallbackAddress: String): Uri {
        val clean = rawInput.trim()
        if (clean.isBlank()) {
            val encoded = Uri.encode("BEKASERVICE $fallbackAddress")
            return Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
        }

        // Web URL (maps.app.goo.gl, goo.gl, google.com/maps, etc.)
        if (clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) {
            return Uri.parse(clean)
        }

        // geo URI
        if (clean.startsWith("geo:", ignoreCase = true)) {
            return Uri.parse(clean)
        }

        // Coordinates format: "41.6389, 42.9861"
        val coordMatch = Regex("""^([+-]?\d+(?:\.\d+)?)\s*,\s*([+-]?\d+(?:\.\d+)?)$""").find(clean)
        if (coordMatch != null) {
            val lat = coordMatch.groupValues[1]
            val lng = coordMatch.groupValues[2]
            return Uri.parse("geo:$lat,$lng?q=$lat,$lng(BEKASERVICE)")
        }

        // Fallback: search query with clean text
        val encoded = Uri.encode(clean)
        return Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
    }

    fun openOnMap(context: Context, rawInput: String, fallbackAddress: String = "კოსტავას N10, ახალციხე") {
        val uri = resolveGoogleMapsUri(rawInput, fallbackAddress)
        try {
            // First attempt: generic ACTION_VIEW with map URI
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            try {
                // If geo scheme failed to resolve without Google Maps installed, fallback to web search
                val webUri = if (uri.scheme == "geo") {
                    val query = uri.schemeSpecificPart.substringAfter("q=", "41.6389,42.9861")
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
                } else uri
                val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "რუკის გახსნა ვერ მოხერხდა", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openDialer(context: Context, phone: String) {
        val clean = phone.trim()
        if (clean.isBlank()) return
        try {
            val digits = clean.filter { it.isDigit() }
            val hasPlus = clean.startsWith("+")
            val dial = if (hasPlus) "+$digits" else digits
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dial")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w("ServiceCenterLocation", "Could not open dialer: ${e.message}")
        }
    }
}

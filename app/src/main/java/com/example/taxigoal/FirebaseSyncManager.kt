package com.example.taxigoal

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import org.json.JSONObject

object FirebaseSyncManager {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    fun init(context: Context) {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings

        // Start sync only if user is logged in
        auth.currentUser?.let {
            startSync(context)
        }
    }

    fun startSync(context: Context) {
        val userId = auth.currentUser?.uid ?: return
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)

        Log.d("FirebaseSync", "Starting sync for user: $userId")

        // 1. Listen for Daily Records changes
        db.collection("users").document(userId).collection("daily_records")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                
                val historyJsonStr = prefs.getString("shift_history", "{}") ?: "{}"
                val history = JSONObject(historyJsonStr)

                snapshots?.forEach { doc ->
                    try {
                        val date = doc.id
                        val data = doc.data
                        val dayJson = JSONObject()
                        dayJson.put("income", (data["income"] as? Number)?.toInt() ?: 0)
                        dayJson.put("expenses", (data["expenses"] as? Number)?.toInt() ?: 0)
                        dayJson.put("net", (data["net"] as? Number)?.toInt() ?: 0)
                        
                        history.put(date, dayJson)
                    } catch (e: Exception) {
                        AppLogger.logError(context, "Firestore Daily Record Parsing Error", e)
                    }
                }

                // Recalculate total
                var totalNet = 0f
                val keys = history.keys()
                while (keys.hasNext()) {
                    totalNet += history.getJSONObject(keys.next()).optInt("net", 0).toFloat()
                }

                prefs.edit()
                    .putString("shift_history", history.toString())
                    .putFloat("total_accumulated", totalNet)
                    .apply()
                
                AppLogger.logInfo(context, "Firebase Sync: Daily records updated successfully")
            }

        // 2. Listen for Config changes (Goal, Theme)
        db.collection("users").document(userId).collection("config").document("goal")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                
                val data = snapshot.data ?: return@addSnapshotListener
                val target = (data["target_amount"] as? Number)?.toFloat() ?: 298000f
                val theme = data["current_theme"] as? String ?: "GOLD"
                
                val editor = prefs.edit()
                if (prefs.getFloat("goal_target", 298000f) != target) {
                    editor.putFloat("goal_target", target)
                }
                if (prefs.getString("app_theme_style", "GOLD") != theme) {
                    editor.putString("app_theme_style", theme)
                }
                editor.apply()
            }
    }

    fun uploadDayData(
        context: Context,
        date: String,
        income: Int,
        expenses: Int,
        net: Int,
        card: Int = 0,
        cash: Int = 0,
        fullJson: String? = null
    ) {
        val userId = auth.currentUser?.uid ?: return
        val data = mutableMapOf<String, Any>(
            "income" to income,
            "expenses" to expenses,
            "net" to net,
            "card" to card,
            "cash" to cash,
            "yandex_comm_pct" to CommissionManager.getYandexPercent(),
            "park_comm_pct" to CommissionManager.getParkPercent(),
            "social_fee" to CommissionManager.getSocialFee(),
            "updated_at" to System.currentTimeMillis()
        )
        
        fullJson?.let {
            try {
                val jsonObj = JSONObject(it)
                if (jsonObj.has("transactions")) {
                    data["transactions_raw"] = jsonObj.getJSONArray("transactions").toString()
                }
            } catch (e: Exception) {
                AppLogger.logError(context, "Firebase upload transactions parsing error", e)
            }
        }

        db.collection("users").document(userId).collection("daily_records").document(date)
            .set(data, SetOptions.merge())
    }

    fun updateConfig(userId: String, target: Float, theme: String) {
        val data = mapOf(
            "target_amount" to target.toDouble(),
            "current_theme" to theme
        )
        db.collection("users").document(userId).collection("config").document("goal")
            .set(data, SetOptions.merge())
    }
    
    fun getUserId() = auth.currentUser?.uid

    fun signOut(context: Context) {
        auth.signOut()
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

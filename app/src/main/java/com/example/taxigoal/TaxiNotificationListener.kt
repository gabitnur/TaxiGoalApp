package com.example.taxigoal

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaxiNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return
        val text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
        val title = sbn.notification.extras.getCharSequence("android.title")?.toString() ?: ""
        
        Log.d("TaxiNotif", "Package: $packageName | Title: $title | Text: $text")

        // 1. ALAMAN (Income)
        if (packageName == "org.telegram.messenger") {
            val pattern = Pattern.compile("(\\d+)\\s*₸\\s*-\\s*платёж принят")
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amount = matcher.group(1)?.toDoubleOrNull() ?: 0.0
                if (amount > 0) processTransaction(amount, isIncome = true, source = "Alaman")
            }
        }

        // 2. GOOGLE WALLET / BANKING (Expense)
        // Patterns for common banking apps in KZ (Kaspi, Halyk) and Google Wallet
        val expensePatterns = arrayOf(
            Pattern.compile("(?:Покупка|Оплата|Списание|Spisanie|Oplata):?\\s*(\\d+[\\s\\d]*)\\s*(?:₸|KZT|тенге)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+[\\s\\d]*)\\s*(?:₸|KZT)\\s*(?:потрачено|списано)", Pattern.CASE_INSENSITIVE)
        )

        val bankPackages = setOf(
            "kz.kaspi.mobile", 
            "kz.halykbank.marketing", 
            "com.google.android.apps.walletnfcrel",
            "com.google.android.apps.nfc.wallet"
        )

        if (bankPackages.contains(packageName)) {
            for (pattern in expensePatterns) {
                val matcher = pattern.matcher(text)
                if (matcher.find()) {
                    val amountStr = matcher.group(1)?.replace(" ", "") ?: ""
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) processTransaction(amount, isIncome = false, source = "Bank/Wallet")
                    break
                }
            }
        }
    }

    private fun processTransaction(amount: Double, isIncome: Boolean, source: String) {
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val history = try {
            JSONObject(prefs.getString("shift_history", "{}") ?: "{}")
        } catch (e: Exception) {
            AppLogger.logError(this, "Notification history parsing error", e)
            JSONObject()
        }
        
        val dayData = history.optJSONObject(today) ?: JSONObject().apply {
            put("card", 0.0); put("cash", 0.0); put("expenses", 0.0); put("net", 0.0)
        }

        var oldNet = dayData.optDouble("net", 0.0)
        var newCard = dayData.optDouble("card", 0.0)
        var newCash = dayData.optDouble("cash", 0.0)
        var newExpenses = dayData.optDouble("expenses", 0.0)

        if (isIncome) {
            // Assume digital notifications (Telegram/Wallet) are Card income
            newCard += amount
        } else {
            newExpenses += amount
        }

        val newNet = CommissionManager.calculateNet(newCard, newCash, newExpenses)
        val diffNet = newNet - oldNet

        dayData.put("card", newCard)
        dayData.put("expenses", newExpenses)
        dayData.put("net", newNet)

        // Добавляем авто-транзакцию
        val transaction = JSONObject().apply {
            put("time", SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
            put("card", if (isIncome) amount else 0.0)
            put("cash", 0.0)
            put("expenses", if (isIncome) 0.0 else amount)
            put("source", source)
        }
        val transactions = dayData.optJSONArray("transactions") ?: org.json.JSONArray()
        transactions.put(transaction)
        dayData.put("transactions", transactions)

        history.put(today, dayData)

        // БОЛЬШЕ НЕ ОБНОВЛЯЕМ ЦЕЛЬ АВТОМАТИЧЕСКИ
        prefs.edit()
            .putString("shift_history", history.toString())
            .apply()

        FirebaseSyncManager.uploadDayData(
            this, today,
            (newCard + newCash).toInt(),
            newExpenses.toInt(),
            newNet.toInt(),
            newCard.toInt(),
            newCash.toInt(),
            dayData.toString()
        )

        Handler(Looper.getMainLooper()).post {
            val emoji = if (isIncome) "💰" else "⛽"
            val sign = if (isIncome) "+" else "-"
            val msg = "$emoji $source: $sign${amount.toInt()} ₸\nNet: ${newNet.toInt()} ₸"
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }
}

package com.example.taxigoal

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope

class CalendarActivity : BaseActivity() {

    private lateinit var tvMonthYear: TextView
    private lateinit var gridCalendar: GridView
    private lateinit var tvMonthTotal: TextView
    
    private lateinit var btnTabFact: Button
    private lateinit var btnTabBank: Button
    private lateinit var btnTabSummary: Button
    private lateinit var btnTabCompare: Button
    
    private lateinit var cardCalendar: CardView
    private lateinit var cardSummary: CardView
    
    private lateinit var tvSummaryTotalFact: TextView
    private lateinit var tvSummaryTotalBank: TextView
    private lateinit var tvSummaryCash: TextView
    private lateinit var tvSummaryNet: TextView
    private lateinit var btnApproveReconciliation: Button
    
    private lateinit var btnUndo: android.widget.ImageButton

    private val calendar = Calendar.getInstance()
    private val DAILY_TARGET = 15000 
    
    private var currentTab = "FACT" // "FACT", "BANK", "SUMMARY", "COMPARE"
    
    private var backupHistoryFact: String? = null
    private var backupHistoryBank: String? = null
    private var backupTotalAccumulated: Float = 0.0f
    
    private val prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "shift_history" || key == "total_accumulated") {
            runOnUiThread { refreshData() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)
        
        getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefListener)

        tvMonthYear = findViewById(R.id.tvMonthYear)
        gridCalendar = findViewById(R.id.gridCalendar)
        tvMonthTotal = findViewById(R.id.tvMonthTotal)
        
        btnTabFact = findViewById(R.id.btnTabFact)
        btnTabBank = findViewById(R.id.btnTabBank)
        btnTabSummary = findViewById(R.id.btnTabSummary)
        btnTabCompare = findViewById(R.id.btnTabCompare)
        
        cardCalendar = findViewById(R.id.cardCalendar)
        cardSummary = findViewById(R.id.cardSummary)
        
        tvSummaryTotalFact = findViewById(R.id.tvSummaryTotalFact)
        tvSummaryTotalBank = findViewById(R.id.tvSummaryTotalBank)
        tvSummaryCash = findViewById(R.id.tvSummaryCash)
        tvSummaryNet = findViewById(R.id.tvSummaryNet)
        btnApproveReconciliation = findViewById(R.id.btnApproveReconciliation)
        
        btnUndo = findViewById(R.id.btnUndo)
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnOpenPdfImport).setOnClickListener {
            startActivity(Intent(this, PdfImportActivity::class.java))
        }
        btnUndo.setOnClickListener { performUndo() }
        
        setupTabs()
        refreshData()
    }
    
    private fun setupTabs() {
        btnTabFact.setOnClickListener { switchTab("FACT") }
        btnTabBank.setOnClickListener { switchTab("BANK") }
        btnTabSummary.setOnClickListener { switchTab("SUMMARY") }
        btnTabCompare.setOnClickListener { switchTab("COMPARE") }
        
        btnApproveReconciliation.setOnClickListener {
            Toast.makeText(this, getString(R.string.btn_approve_reconciliation), Toast.LENGTH_LONG).show()
            switchTab("SUMMARY")
        }
    }
    
    private fun switchTab(tab: String) {
        currentTab = tab
        // Note: Using hardcoded darkGrey color as fallback if not in theme yet
        val darkGrey = ColorStateList.valueOf(Color.parseColor("#2C2C2E"))
        
        btnTabFact.backgroundTintList = if (tab == "FACT") null else darkGrey
        btnTabBank.backgroundTintList = if (tab == "BANK") null else darkGrey
        btnTabSummary.backgroundTintList = if (tab == "SUMMARY") null else darkGrey
        btnTabCompare.backgroundTintList = if (tab == "COMPARE") null else darkGrey
        
        if (tab == "SUMMARY") {
            cardCalendar.visibility = View.GONE
            cardSummary.visibility = View.VISIBLE
            btnApproveReconciliation.visibility = View.VISIBLE
            calculateSummary()
        } else {
            cardCalendar.visibility = View.VISIBLE
            cardSummary.visibility = View.GONE
            btnApproveReconciliation.visibility = View.GONE
            refreshData()
        }
    }
    
    private fun refreshData() {
        try {
            val monthFormat = SimpleDateFormat("LLLL yyyy", Locale.getDefault())
            tvMonthYear.text = monthFormat.format(calendar.time).replaceFirstChar { it.uppercase() }

            val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
            val factJson = JSONObject(prefs.getString("shift_history", "{}") ?: "{}")
            val bankJson = JSONObject(prefs.getString("bank_history", "{}") ?: "{}")

            val daysList = ArrayList<CalendarDay>()
            val tempCal = calendar.clone() as Calendar
            tempCal.set(Calendar.DAY_OF_MONTH, 1)

            var firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 2
            if (firstDayOfWeek < 0) firstDayOfWeek = 6
            for (i in 0 until firstDayOfWeek) {
                daysList.add(CalendarDay(0, 0, 0, 0, false, "", 0, 0))
            }

            val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            var monthTotalNet = 0
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (day in 1..daysInMonth) {
                tempCal.set(Calendar.DAY_OF_MONTH, day)
                val dateStr = dateFormat.format(tempCal.time)

                val shift = factJson.optJSONObject(dateStr)
                val bank = bankJson.optJSONObject(dateStr)

                val factIncome = shift?.optInt("income", 0) ?: 0
                val factNet = shift?.optInt("net", 0) ?: 0
                val bankIncome = bank?.optInt("income", 0) ?: 0
                
                if (currentTab == "FACT") {
                    daysList.add(CalendarDay(day, factNet, factIncome, 0, shift != null, dateStr, factIncome, bankIncome))
                    monthTotalNet += factNet
                } else if (currentTab == "BANK") {
                    daysList.add(CalendarDay(day, bankIncome, bankIncome, 0, bank != null, dateStr, factIncome, bankIncome))
                    monthTotalNet += bankIncome
                } else if (currentTab == "COMPARE") {
                    val diff = factIncome - bankIncome
                    daysList.add(CalendarDay(day, diff, factIncome, 0, true, dateStr, factIncome, bankIncome))
                    monthTotalNet += diff
                }
            }

            tvMonthTotal.text = when(currentTab) {
                "FACT" -> getString(R.string.month_total_fact, monthTotalNet)
                "BANK" -> getString(R.string.month_total_bank, monthTotalNet)
                "COMPARE" -> getString(R.string.reconcile_fact_bank, monthTotalNet)
                else -> ""
            }
            
            gridCalendar.adapter = CalendarAdapter(daysList)
            gridCalendar.setOnItemClickListener { _, _, position, _ ->
                val selectedDay = daysList[position]
                if (selectedDay.dayNumber != 0) showDayDetailsDialog(selectedDay)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun calculateSummary() {
        try {
            val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
            val factJson = JSONObject(prefs.getString("shift_history", "{}") ?: "{}")
            val bankJson = JSONObject(prefs.getString("bank_history", "{}") ?: "{}")
            
            var totalFactIncome = 0
            var totalBankIncome = 0
            var totalFactNet = 0
            
            val tempCal = calendar.clone() as Calendar
            val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            for (day in 1..daysInMonth) {
                tempCal.set(Calendar.DAY_OF_MONTH, day)
                val dateStr = dateFormat.format(tempCal.time)
                val shift = factJson.optJSONObject(dateStr)
                val bank = bankJson.optJSONObject(dateStr)
                
                totalFactIncome += shift?.optInt("income", 0) ?: 0
                totalFactNet += shift?.optInt("net", 0) ?: 0
                totalBankIncome += bank?.optInt("income", 0) ?: 0
            }
            
            tvSummaryTotalFact.text = getString(R.string.summary_total_fact, formatMoney(totalFactIncome.toDouble()))
            tvSummaryTotalBank.text = getString(R.string.summary_total_bank, formatMoney(totalBankIncome.toDouble()))
            val cashOnHand = totalFactIncome - totalBankIncome
            tvSummaryCash.text = getString(R.string.summary_cash, formatMoney(cashOnHand.toDouble()))
            tvSummaryNet.text = getString(R.string.summary_net, formatMoney(totalFactNet.toDouble()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDayDetailsDialog(day: CalendarDay) {
        val title = "${getString(R.string.confirm_shift_title)}: ${day.dayNumber}"
        
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        val history = JSONObject(prefs.getString("shift_history", "{}") ?: "{}")
        val dayJson = history.optJSONObject(day.dateStr) ?: JSONObject()
        
        val card = dayJson.optInt("card", 0)
        val cash = dayJson.optInt("cash", 0)
        val expenses = dayJson.optInt("expenses", 0)
        val confirmed = dayJson.optDouble("confirmed_goal", 0.0)
        
        val sb = StringBuilder()
        sb.append("💳 Карта: $card ₸\n")
        sb.append("💵 Наличные: $cash ₸\n")
        if (expenses > 0) sb.append("⛽ Расход: $expenses ₸\n")
        sb.append("📊 Заработано (Net): ${day.netAmount} ₸\n")
        sb.append("🎯 ЗАЧИСЛЕНО В ЦЕЛЬ: ${confirmed.toInt()} ₸\n\n")
        
        // Список отдельных заказов/транзакций
        val transactions = dayJson.optJSONArray("transactions")
        if (transactions != null && transactions.length() > 0) {
            sb.append("📜 ИСТОРИЯ ЗА ДЕНЬ:\n")
            for (i in 0 until transactions.length()) {
                val t = transactions.getJSONObject(i)
                val time = t.optString("time", "--:--")
                val tCard = t.optDouble("card", 0.0)
                val tCash = t.optDouble("cash", 0.0)
                val tExp = t.optDouble("expenses", 0.0)
                val source = t.optString("source", "Ввод")
                
                val amount = if (tExp > 0) "-${tExp.toInt()}" else "+${(tCard + tCash).toInt()}"
                sb.append("• $time | $amount ₸ ($source)\n")
            }
        }

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(sb.toString())
            .setPositiveButton(getString(R.string.btn_back), null)
            
        if (currentTab == "FACT") {
            dialogBuilder.setNeutralButton("⚠️ Исправить ошибку") { _, _ -> showFixErrorDialog(day, dayJson.toString()) }
            if (day.hasShift) {
                dialogBuilder.setNegativeButton(getString(R.string.undo)) { _, _ -> deleteShift(day) }
            }
        }
        dialogBuilder.show()
    }

    private fun showFixErrorDialog(day: CalendarDay, currentJson: String) {
        val et = EditText(this).apply {
            hint = "Что исправить? (например: 'добавь 500 налом')"
        }
        AlertDialog.Builder(this)
            .setTitle("Исправить ошибку через AI")
            .setView(et)
            .setPositiveButton("Исправить") { _, _ ->
                val feedback = et.text.toString()
                if (feedback.isNotEmpty()) {
                    val progress = AlertDialog.Builder(this).setMessage("AI исправляет данные...").show()
                    MainScope().launch {
                        FeedbackManager.fixError(this@CalendarActivity, day.dateStr, currentJson, feedback) { success, error ->
                            progress.dismiss()
                            if (success) Toast.makeText(this@CalendarActivity, "Данные исправлены!", Toast.LENGTH_SHORT).show()
                            else Toast.makeText(this@CalendarActivity, "Ошибка: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditShiftDialog(day: CalendarDay) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val etIncome = EditText(this).apply {
            hint = getString(R.string.hint_extra)
            if (day.hasShift) setText(day.incomeAmount.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(etIncome)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_shift_title))
            .setView(layout)
            .setPositiveButton(getString(R.string.btn_save)) { _, _ ->
                val newInc = etIncome.text.toString().toIntOrNull() ?: 0
                updateShiftData(day, newInc, 0, newInc)
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun updateShiftData(day: CalendarDay, newInc: Int, newExp: Int, newNet: Int) {
        saveBackupState()
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        val key = if (currentTab == "FACT") "shift_history" else "bank_history"
        val history = JSONObject(prefs.getString(key, "{}") ?: "{}")
        val diffNet = newNet - day.netAmount
        val shift = JSONObject().apply {
            put("income", newInc)
            put("expenses", newExp)
            put("net", newNet)
        }
        history.put(day.dateStr, shift)
        prefs.edit().putString(key, history.toString()).apply()
        if (currentTab == "FACT") {
            val total = prefs.getFloat("total_accumulated", 0.0f)
            prefs.edit().putFloat("total_accumulated", (total + diffNet).coerceAtLeast(0.0f)).apply()
        }
        btnUndo.visibility = View.VISIBLE
        refreshData()
    }

    private fun deleteShift(day: CalendarDay) {
        saveBackupState()
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        val key = if (currentTab == "FACT") "shift_history" else "bank_history"
        val history = JSONObject(prefs.getString(key, "{}") ?: "{}")
        if (history.has(day.dateStr)) {
            history.remove(day.dateStr)
            prefs.edit().putString(key, history.toString()).apply()
            if (currentTab == "FACT") {
                val total = prefs.getFloat("total_accumulated", 0.0f)
                prefs.edit().putFloat("total_accumulated", (total - day.netAmount).coerceAtLeast(0.0f)).apply()
            }
            btnUndo.visibility = View.VISIBLE
            refreshData()
        }
    }
    
    private fun saveBackupState() {
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        backupHistoryFact = prefs.getString("shift_history", "{}")
        backupHistoryBank = prefs.getString("bank_history", "{}")
        backupTotalAccumulated = prefs.getFloat("total_accumulated", 0.0f)
    }
    
    private fun performUndo() {
        if (backupHistoryFact == null) return
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        prefs.edit()
            .putString("shift_history", backupHistoryFact)
            .putString("bank_history", backupHistoryBank)
            .putFloat("total_accumulated", backupTotalAccumulated)
            .apply()
        btnUndo.visibility = View.GONE
        refreshData()
        if (currentTab == "SUMMARY") calculateSummary()
    }

    override fun onDestroy() {
        super.onDestroy()
        getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    private fun formatMoney(amount: Double): String {
        return java.text.DecimalFormat("#,###", java.text.DecimalFormatSymbols(Locale.US).apply { groupingSeparator = ' ' }).format(amount.toInt())
    }

    data class CalendarDay(val dayNumber: Int, val netAmount: Int, val incomeAmount: Int, val expenseAmount: Int, val hasShift: Boolean, val dateStr: String, val factIncome: Int, val bankIncome: Int)

    inner class CalendarAdapter(private val days: List<CalendarDay>) : BaseAdapter() {
        override fun getCount(): Int = days.size
        override fun getItem(p: Int): Any = days[p]
        override fun getItemId(p: Int): Long = p.toLong()
        override fun getView(p: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(parent?.context).inflate(R.layout.item_calendar_day, parent, false)
            val tvDayNum = view.findViewById<TextView>(R.id.tvDayNum)
            val tvDayPct = view.findViewById<TextView>(R.id.tvDayPercent)
            val container = view.findViewById<View>(R.id.dayContainer)
            val day = days[p]
            if (day.dayNumber == 0) {
                container.visibility = View.INVISIBLE
            } else {
                container.visibility = View.VISIBLE
                tvDayNum.text = day.dayNumber.toString()
                if (currentTab == "COMPARE") {
                    tvDayPct.text = "${day.factIncome}/${day.bankIncome}"
                    tvDayPct.textSize = 8f
                    val diff = day.factIncome - day.bankIncome
                    when {
                        diff == 0 && day.factIncome > 0 -> container.setBackgroundColor(Color.parseColor("#2E7D32"))
                        diff > 0 -> container.setBackgroundColor(Color.parseColor("#F57C00"))
                        diff < 0 -> container.setBackgroundColor(Color.parseColor("#D32F2F"))
                        else -> container.setBackgroundColor(Color.TRANSPARENT)
                    }
                } else if (day.hasShift) {
                    val pct = ((day.netAmount.toDouble() / DAILY_TARGET) * 100).toInt()
                    tvDayPct.text = "$pct%"
                    when {
                        pct >= 100 -> container.setBackgroundColor(Color.parseColor("#2E7D32"))
                        pct >= 50 -> container.setBackgroundColor(Color.parseColor("#F57C00"))
                        else -> container.setBackgroundColor(Color.parseColor("#D32F2F"))
                    }
                } else {
                    tvDayPct.text = "-"
                    container.setBackgroundColor(Color.TRANSPARENT)
                }
            }
            return view
        }
    }
}

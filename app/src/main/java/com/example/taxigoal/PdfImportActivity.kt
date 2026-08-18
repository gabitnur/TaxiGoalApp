package com.example.taxigoal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.json.JSONObject
import java.util.regex.Pattern

class PdfImportActivity : BaseActivity() {

    private lateinit var btnSelectPdf: Button
    private lateinit var btnApprovePdf: Button
    private lateinit var cardPreview: androidx.cardview.widget.CardView
    private lateinit var tvPdfParsedResult: TextView

    private val parsedShifts = HashMap<String, ShiftData>()

    data class ShiftData(var income: Int, var expenses: Int)

    private val selectPdfLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { parsePdf(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_import)

        PDFBoxResourceLoader.init(applicationContext)

        btnSelectPdf = findViewById(R.id.btnSelectPdf)
        btnApprovePdf = findViewById(R.id.btnApprovePdf)
        cardPreview = findViewById(R.id.cardPreview)
        tvPdfParsedResult = findViewById(R.id.tvPdfParsedResult)

        findViewById<View>(R.id.btnBackFromPdf).setOnClickListener { finish() }
        btnSelectPdf.setOnClickListener { selectPdfLauncher.launch("application/pdf") }
        btnApprovePdf.setOnClickListener { saveParsedDataToCalendar() }
    }

    private fun parsePdf(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val fullText = stripper.getText(document)
            document.close()

            parsedShifts.clear()
            val lines = fullText.split("\n")
            val datePattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})")

            for (line in lines) {
                val matcher = datePattern.matcher(line)
                if (matcher.find()) {
                    val rawDate = matcher.group(1) ?: continue
                    val parts = rawDate.split(".")
                    if (parts.size == 3) {
                        val formattedDate = "${parts[2]}-${parts[1]}-${parts[0]}"
                        val numbers = line.replace("[^0-9\\+\\-]".toRegex(), " ")
                            .trim().split("\\s+".toRegex())

                        for (numStr in numbers) {
                            val valInt = numStr.toIntOrNull() ?: continue
                            if (valInt in 100..500000) {
                                val current = parsedShifts.getOrDefault(formattedDate, ShiftData(0, 0))
                                if (line.contains("+") || line.lowercase().contains("пополнение")) {
                                    current.income += valInt
                                } else {
                                    current.expenses += valInt
                                }
                                parsedShifts[formattedDate] = current
                            }
                        }
                    }
                }
            }

            if (parsedShifts.isEmpty()) {
                Toast.makeText(this, "Не удалось найти транзакции", Toast.LENGTH_LONG).show()
                return
            }

            val sb = StringBuilder()
            sb.append("${getString(R.string.parsed_data_title)}\n\n")
            for ((date, data) in parsedShifts) {
                sb.append("📅 $date\n   +${data.income} ₸ | -${data.expenses} ₸\n\n")
            }
            tvPdfParsedResult.text = sb.toString()
            cardPreview.visibility = View.VISIBLE
            btnApprovePdf.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveParsedDataToCalendar() {
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        val history = JSONObject(prefs.getString("bank_history", "{}") ?: "{}")
        for ((dateStr, data) in parsedShifts) {
            history.put(dateStr, JSONObject().apply {
                put("income", data.income)
                put("expenses", data.expenses)
                put("net", data.income - data.expenses)
            })
        }
        prefs.edit().putString("bank_history", history.toString()).apply()
        Toast.makeText(this, getString(R.string.btn_approve_pdf), Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, CalendarActivity::class.java))
        finish()
    }
}

package com.example.taxigoal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

class LogViewerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)

        val tvLogContent = findViewById<TextView>(R.id.tvLogContent)
        val btnBack = findViewById<Button>(R.id.btnBackFromLogs)
        val btnClear = findViewById<ImageButton>(R.id.btnClearLogs)
        val btnShare = findViewById<Button>(R.id.btnShareLogs)

        refreshLogs(tvLogContent)

        btnBack.setOnClickListener { finish() }

        btnClear.setOnClickListener {
            AppLogger.clearLogs(this)
            refreshLogs(tvLogContent)
            Toast.makeText(this, "Логи очищены", Toast.LENGTH_SHORT).show()
        }

        btnShare.setOnClickListener {
            val content = AppLogger.getLogFileContent(this)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "TaxiGoal Debug Logs")
                putExtra(Intent.EXTRA_TEXT, content)
            }
            startActivity(Intent.createChooser(shareIntent, "Отправить логи"))
        }
    }

    private fun refreshLogs(tv: TextView) {
        tv.text = AppLogger.getLogFileContent(this)
    }
}

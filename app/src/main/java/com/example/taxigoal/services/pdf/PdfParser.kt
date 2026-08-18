package com.example.taxigoal.services.pdf

import android.content.Context
import android.net.Uri
import com.example.taxigoal.data.entities.FinancialTransaction
import com.example.taxigoal.utils.AppLogger
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.regex.Pattern

object PdfParser {

    suspend fun parseBankPdf(context: Context, uri: Uri): Result<List<FinancialTransaction>> = withContext(Dispatchers.IO) {
        try {
            PDFBoxResourceLoader.init(context)
            AppLogger.info("PdfParser", "PARSE_START", "Reading PDF")
            
            val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            inputStream.close()

            if (text.isBlank()) throw Exception("PDF is empty or protected")

            val transactions = mutableListOf<FinancialTransaction>()
            val lines = text.split("\n")
            
            // Simple regex for amount and date (KZT/₸)
            val datePattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})")
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

            lines.forEach { line ->
                val matcher = datePattern.matcher(line)
                if (matcher.find()) {
                    // Very basic parsing logic for demonstration/test
                    // In real app we'd search for patterns like "+ 5000" or "- 2000"
                    val numbers = line.replace(Regex("[^0-9.]"), " ").split(" ").filter { it.isNotBlank() }
                    numbers.forEach { numStr ->
                        val amount = numStr.toDoubleOrNull() ?: 0.0
                        if (amount > 100) { // filter noise
                            transactions.add(FinancialTransaction(
                                userId = uid,
                                date = Date(),
                                amount = amount,
                                category = "Импорт",
                                description = line.take(50),
                                type = if (line.contains("+")) "INCOME" else "EXPENSE",
                                source = "BANK"
                            ))
                        }
                    }
                }
            }

            AppLogger.info("PdfParser", "PARSE_SUCCESS", "Found ${transactions.size} operations")
            Result.success(transactions)
        } catch (e: Exception) {
            AppLogger.error("PdfParser", "PARSE_ERROR", e.message ?: "Unknown PDF error")
            Result.failure(e)
        }
    }
}

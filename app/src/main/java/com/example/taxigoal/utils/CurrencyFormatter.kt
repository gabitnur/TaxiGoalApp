package com.example.taxigoal.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

object CurrencyFormatter {
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ' '
    }
    private val formatter = DecimalFormat("#,###", symbols)

    fun format(amount: Double): String {
        return "${formatter.format(amount.toLong())} ₸"
    }

    fun format(amount: Int): String {
        return "${formatter.format(amount.toLong())} ₸"
    }
}

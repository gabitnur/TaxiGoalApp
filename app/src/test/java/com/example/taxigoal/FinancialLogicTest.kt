package com.example.taxigoal

import com.example.taxigoal.utils.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.ceil

class FinancialLogicTest {

    @Test
    fun testCurrencyFormatting() {
        assertEquals("1 000 ₸", CurrencyFormatter.format(1000.0))
        assertEquals("10 000 ₸", CurrencyFormatter.format(10000.0))
        assertEquals("1 250 500 ₸", CurrencyFormatter.format(1250500.0))
    }

    @Test
    fun testProfitCalculation() {
        val income = 30000.0
        val commission = income * 0.18
        val fuel = 6000.0
        val food = 2000.0
        val mileage = 150.0
        val depreciationPerKm = 15.0
        
        val totalExpenses = commission + fuel + food + (mileage * depreciationPerKm)
        val profit = income - totalExpenses
        
        assertEquals(14350.0, profit, 0.1)
    }

    @Test
    fun testGoalForecast() {
        val target = 1000000.0
        val current = 100000.0
        val avgDailyProfit = 15000.0
        
        val remaining = target - current
        val days = ceil(remaining / avgDailyProfit).toInt()
        
        assertEquals(60, days)
    }
}

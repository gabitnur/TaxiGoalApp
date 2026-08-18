package com.example.taxigoal

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object CommissionManager {

    private const val KEY_YANDEX_PERCENT = "yandex_commission_percent"
    private const val KEY_PARK_PERCENT = "park_commission_percent"
    private const val KEY_SOCIAL_FEE = "social_extra_fee"

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            setConfigSettingsAsync(configSettings)
            setDefaultsAsync(mapOf(
                KEY_YANDEX_PERCENT to 18.0,
                KEY_PARK_PERCENT to 3.0,
                KEY_SOCIAL_FEE to 200.0
            ))
        }
    }

    fun init() {
        remoteConfig.fetchAndActivate()
    }

    fun getYandexPercent() = remoteConfig.getDouble(KEY_YANDEX_PERCENT)
    fun getParkPercent() = remoteConfig.getDouble(KEY_PARK_PERCENT)
    fun getSocialFee() = remoteConfig.getDouble(KEY_SOCIAL_FEE)

    /**
     * Net = (Card + Cash) - (Yandex Commission + Park Commission) - (Expenses / Wallet / Fuel)
     */
    fun calculateNet(card: Double, cash: Double, autoExpenses: Double = 0.0): Double {
        val gross = card + cash
        val yandexComm = gross * (getYandexPercent() / 100.0)
        val parkComm = gross * (getParkPercent() / 100.0)
        val socialFee = getSocialFee()
        
        return gross - (yandexComm + parkComm + socialFee + autoExpenses)
    }
}

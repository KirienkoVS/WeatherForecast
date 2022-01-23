package com.example.weatherforecast

import androidx.annotation.DrawableRes

data class Weather(
    @DrawableRes
    var weatherIcon: Int,
    var date: String,
    var weather: String,
    var dayTemperature: Double,
    var nightTemperature: Double
)

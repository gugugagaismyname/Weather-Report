package com.example.weatherreport.adapters

data class WeatherModel(
    val city: String,
    val time: String,
    val condition: String,
    val currentTemp: String,
    val maxTemp: String,
    val minTemp: String,
    val imageURL: String,
    val hours: String
)

package com.example.flightsearchapp


data class flight(
    val depAirport: Airport,
    val destAirport: Airport,
    var isFavorite: Boolean = false
)

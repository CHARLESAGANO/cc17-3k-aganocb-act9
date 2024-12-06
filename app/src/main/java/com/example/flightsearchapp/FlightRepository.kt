package com.example.flightsearchapp


import kotlinx.coroutines.flow.Flow

class FlightRepository(private val airportDao: FlightDao) {
    fun searchAirports(query: String): Flow<List<Airport>> {
        return airportDao.getAirportsBySearchQuery("%${query}%")
    }

    fun getDestinationAirports(departureCode: String): Flow<List<Airport>> {
        return airportDao.getAllAirportsExcept(departureCode)
    }

    suspend fun getAirportByCode(code: String): Airport? {
        return airportDao.getAirportByCode(code)
    }
}

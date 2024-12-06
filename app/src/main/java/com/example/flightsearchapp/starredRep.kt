package com.example.flightsearchapp


import kotlinx.coroutines.flow.Flow

class starredRep(private val faveDa: faveDa) {
    fun Fave(): Flow<List<fave>> {
        return faveDa.getAllFave()
    }
    suspend fun addFavorite(fave: fave) {
        faveDa.insertFavorite(fave)
    }
    suspend fun getFave(departureCode: String, destinationCode: String): fave? {
        return faveDa.getFavorite(departureCode, destinationCode)
    }
    suspend fun remFave(fave: fave) {
        faveDa.deleteFavorite(fave)
    }
    fun faveRoute(departureCode: String, destinationCode: String): Flow<Boolean> {
        return faveDa.fave(departureCode, destinationCode)
    }


}
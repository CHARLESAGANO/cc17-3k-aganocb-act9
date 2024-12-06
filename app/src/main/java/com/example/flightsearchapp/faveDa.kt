package com.example.flightsearchapp


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface faveDa {
    @Query("SELECT * FROM favorite")
    fun getAllFave(): Flow<List<fave>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(fave: fave)
    @Delete
    suspend fun deleteFavorite(fave: fave)
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM favorite 
            WHERE departure_code = :departureCode 
            AND destination_code = :destinationCode
        )
    """)
    fun fave(departureCode: String, destinationCode: String): Flow<Boolean>
    @Query("""
        SELECT * FROM favorite 
        WHERE departure_code = :departureCode 
        AND destination_code = :destinationCode 
        LIMIT 1
    """)
    suspend fun getFavorite(departureCode: String, destinationCode: String): fave?
}
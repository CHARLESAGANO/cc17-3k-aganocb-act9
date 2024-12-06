package com.example.flightsearchapp
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "airport")
data class Airport(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "iata_code") val iataCode: String,
    @ColumnInfo(name = "passengers") val passengers: Int
)
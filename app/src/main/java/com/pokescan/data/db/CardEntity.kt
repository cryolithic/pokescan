package com.pokescan.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val supertype: String,       // Pokémon, Trainer, Energy
    val subtypes: String,        // JSON array stored as string
    val hp: String?,
    val types: String?,          // JSON array: Fire, Water, etc.
    val setId: String,
    val setName: String,
    val setSeries: String,
    val number: String,
    val rarity: String?,
    val imageUrlSmall: String,
    val imageUrlLarge: String,
    val marketPrice: Double?,    // TCGPlayer market price in USD
    val lowPrice: Double?,
    val highPrice: Double?,
    val priceUpdatedAt: Long?,   // epoch millis
    val addedAt: Long = System.currentTimeMillis(),
    val quantity: Int = 1,
    val condition: String = "NM", // NM, LP, MP, HP, DMG
    val notes: String = "",
    val isFoil: Boolean = false,
    val artist: String = "",
    val nationalPokedexNumbers: String = "", // JSON array
)

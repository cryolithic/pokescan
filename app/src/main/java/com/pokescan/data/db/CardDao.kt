package com.pokescan.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY addedAt DESC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY name ASC")
    fun getAllCardsSortedByName(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY marketPrice DESC NULLS LAST")
    fun getAllCardsSortedByValue(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY setName ASC, number ASC")
    fun getAllCardsSortedBySet(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY types ASC, name ASC")
    fun getAllCardsSortedByType(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY rarity ASC, name ASC")
    fun getAllCardsSortedByRarity(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun getCardById(id: String): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE name LIKE '%' || :query || '%' OR setName LIKE '%' || :query || '%'")
    fun searchCards(query: String): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: String)

    @Query("SELECT COUNT(*) FROM cards")
    fun getCardCount(): Flow<Int>

    @Query("SELECT SUM(marketPrice * quantity) FROM cards WHERE marketPrice IS NOT NULL")
    fun getTotalCollectionValue(): Flow<Double?>

    @Query("SELECT COUNT(DISTINCT setId) FROM cards")
    fun getUniqueSetCount(): Flow<Int>

    @Query("SELECT setName, COUNT(*) as count FROM cards GROUP BY setName ORDER BY count DESC")
    fun getCardCountBySet(): Flow<List<SetCount>>

    @Query("SELECT types, COUNT(*) as count FROM cards WHERE types IS NOT NULL GROUP BY types ORDER BY count DESC")
    fun getCardCountByType(): Flow<List<TypeCount>>

    @Query("SELECT rarity, COUNT(*) as count FROM cards WHERE rarity IS NOT NULL GROUP BY rarity ORDER BY count DESC")
    fun getCardCountByRarity(): Flow<List<RarityCount>>

    @Query("UPDATE cards SET marketPrice = :price, lowPrice = :low, highPrice = :high, priceUpdatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCardPrice(id: String, price: Double?, low: Double?, high: Double?, updatedAt: Long)
}

data class SetCount(val setName: String, val count: Int)
data class TypeCount(val types: String, val count: Int)
data class RarityCount(val rarity: String, val count: Int)

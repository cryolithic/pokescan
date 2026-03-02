package com.pokescan.data.repository

import com.google.gson.Gson
import com.pokescan.data.api.ApiCard
import com.pokescan.data.api.PokemonTcgApi
import com.pokescan.data.api.bestPrice
import com.pokescan.data.db.CardDao
import com.pokescan.data.db.CardEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

enum class SortOrder {
    DATE_ADDED, NAME, VALUE, SET, TYPE, RARITY
}

@Singleton
class CardRepository @Inject constructor(
    private val cardDao: CardDao,
    private val api: PokemonTcgApi,
    private val gson: Gson,
) {

    fun getCards(sortOrder: SortOrder = SortOrder.DATE_ADDED): Flow<List<CardEntity>> {
        return when (sortOrder) {
            SortOrder.DATE_ADDED -> cardDao.getAllCards()
            SortOrder.NAME -> cardDao.getAllCardsSortedByName()
            SortOrder.VALUE -> cardDao.getAllCardsSortedByValue()
            SortOrder.SET -> cardDao.getAllCardsSortedBySet()
            SortOrder.TYPE -> cardDao.getAllCardsSortedByType()
            SortOrder.RARITY -> cardDao.getAllCardsSortedByRarity()
        }
    }

    fun getCardById(id: String): Flow<CardEntity?> = cardDao.getCardById(id)

    fun searchCards(query: String): Flow<List<CardEntity>> = cardDao.searchCards(query)

    fun getCardCount(): Flow<Int> = cardDao.getCardCount()

    fun getTotalCollectionValue(): Flow<Double?> = cardDao.getTotalCollectionValue()

    fun getUniqueSetCount(): Flow<Int> = cardDao.getUniqueSetCount()

    fun getCardCountBySet() = cardDao.getCardCountBySet()

    fun getCardCountByType() = cardDao.getCardCountByType()

    fun getCardCountByRarity() = cardDao.getCardCountByRarity()

    suspend fun addCard(card: CardEntity) = cardDao.insertCard(card)

    suspend fun updateCard(card: CardEntity) = cardDao.updateCard(card)

    suspend fun deleteCard(id: String) = cardDao.deleteCardById(id)

    /** Search the Pokemon TCG API for cards matching [query]. */
    suspend fun searchOnlineCards(query: String): Result<List<ApiCard>> = runCatching {
        api.searchCards(query = "name:$query*").data
    }

    /** Fetch full card details + prices from the API and refresh the local DB entry. */
    suspend fun refreshCardPrices(cardId: String): Result<Unit> = runCatching {
        val apiCard = api.getCard(cardId).data
        val priceDetail = apiCard.tcgplayer?.prices?.bestPrice(isFoil = false)
        cardDao.updateCardPrice(
            id = cardId,
            price = priceDetail?.market,
            low = priceDetail?.low,
            high = priceDetail?.high,
            updatedAt = System.currentTimeMillis(),
        )
    }

    fun apiCardToEntity(apiCard: ApiCard, quantity: Int = 1, condition: String = "NM", isFoil: Boolean = false): CardEntity {
        val priceDetail = apiCard.tcgplayer?.prices?.bestPrice(isFoil)
        return CardEntity(
            id = apiCard.id,
            name = apiCard.name,
            supertype = apiCard.supertype,
            subtypes = gson.toJson(apiCard.subtypes ?: emptyList<String>()),
            hp = apiCard.hp,
            types = gson.toJson(apiCard.types ?: emptyList<String>()),
            setId = apiCard.set.id,
            setName = apiCard.set.name,
            setSeries = apiCard.set.series,
            number = apiCard.number,
            rarity = apiCard.rarity,
            imageUrlSmall = apiCard.images.small,
            imageUrlLarge = apiCard.images.large,
            marketPrice = priceDetail?.market,
            lowPrice = priceDetail?.low,
            highPrice = priceDetail?.high,
            priceUpdatedAt = System.currentTimeMillis(),
            quantity = quantity,
            condition = condition,
            isFoil = isFoil,
            artist = apiCard.artist ?: "",
            nationalPokedexNumbers = gson.toJson(apiCard.nationalPokedexNumbers ?: emptyList<Int>()),
        )
    }
}

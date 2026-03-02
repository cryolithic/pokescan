package com.pokescan.data.api

import com.google.gson.annotations.SerializedName

data class CardSearchResponse(
    val data: List<ApiCard>,
    val page: Int,
    val pageSize: Int,
    val count: Int,
    val totalCount: Int,
)

data class SingleCardResponse(
    val data: ApiCard,
)

data class ApiCard(
    val id: String,
    val name: String,
    val supertype: String,
    val subtypes: List<String>?,
    val hp: String?,
    val types: List<String>?,
    val set: ApiSet,
    val number: String,
    val rarity: String?,
    val images: ApiImages,
    val tcgplayer: ApiTcgPlayer?,
    val artist: String?,
    @SerializedName("nationalPokedexNumbers")
    val nationalPokedexNumbers: List<Int>?,
)

data class ApiSet(
    val id: String,
    val name: String,
    val series: String,
    val releaseDate: String?,
    val images: ApiSetImages?,
)

data class ApiSetImages(
    val symbol: String?,
    val logo: String?,
)

data class ApiImages(
    val small: String,
    val large: String,
)

data class ApiTcgPlayer(
    val url: String?,
    val updatedAt: String?,
    val prices: ApiPrices?,
)

data class ApiPrices(
    val normal: ApiPriceDetail?,
    val holofoil: ApiPriceDetail?,
    val reverseHolofoil: ApiPriceDetail?,
    @SerializedName("1stEditionNormal")
    val firstEditionNormal: ApiPriceDetail?,
    @SerializedName("1stEditionHolofoil")
    val firstEditionHolofoil: ApiPriceDetail?,
)

data class ApiPriceDetail(
    val low: Double?,
    val mid: Double?,
    val high: Double?,
    val market: Double?,
    val directLow: Double?,
)

fun ApiPrices.bestPrice(isFoil: Boolean): ApiPriceDetail? {
    return if (isFoil) {
        holofoil ?: reverseHolofoil ?: firstEditionHolofoil ?: normal
    } else {
        normal ?: holofoil ?: reverseHolofoil ?: firstEditionNormal
    }
}

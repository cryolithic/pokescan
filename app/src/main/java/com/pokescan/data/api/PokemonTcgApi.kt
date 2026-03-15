package com.pokescan.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface PokemonTcgApi {

    @GET("cards")
    suspend fun searchCards(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("orderBy") orderBy: String = "name",
    ): CardSearchResponse

    @GET("cards/{id}")
    suspend fun getCard(
        @Path("id") id: String,
    ): SingleCardResponse

    // Search by card name only
    suspend fun searchByName(name: String, pageSize: Int = 10): CardSearchResponse =
        searchCards(query = "name:\"$name\"", pageSize = pageSize)

    // Search by name + set
    suspend fun searchByNameAndSet(name: String, setId: String): CardSearchResponse =
        searchCards(query = "name:\"$name\" set.id:$setId")
}

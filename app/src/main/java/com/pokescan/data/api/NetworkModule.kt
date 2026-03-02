package com.pokescan.data.api

import com.pokescan.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .apply {
                        if (BuildConfig.POKEMON_TCG_API_KEY.isNotBlank()) {
                            header("X-Api-Key", BuildConfig.POKEMON_TCG_API_KEY)
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun providePokemonTcgApi(okHttpClient: OkHttpClient): PokemonTcgApi {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.POKEMON_TCG_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PokemonTcgApi::class.java)
    }
}

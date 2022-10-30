package ysports.app.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import ysports.app.api.leagues.LeaguesResponse
import ysports.app.api.matches.MatchesResponse

interface JsonApi {

    @GET
    fun getLeagues(@Url path: String): Call<LeaguesResponse>

    @GET
    fun getMatches(@Url path: String): Call<MatchesResponse>

    companion object {
        fun create(BASE_URL: String): JsonApi {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BASIC

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .build()
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(JsonApi::class.java)
        }
    }
}
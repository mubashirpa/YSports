package ysports.app.api.newsapi.org

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NewsApi {

    @GET("v2/{endpoints}")
    fun getNews(
        @Path("endpoints") endpoints: String,
        @Query("apiKey") apiKey: String,
        @Query("category") category: String,
        @Query("country") country: String
    ): Call<NewsResponse>

    companion object {
        private const val BASE_URL = "https://newsapi.org/"

        fun create(): NewsApi {
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
            return retrofit.create(NewsApi::class.java)
        }
    }
}
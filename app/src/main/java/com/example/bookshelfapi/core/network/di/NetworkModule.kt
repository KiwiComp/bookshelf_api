package com.example.bookshelfapi.core.network.di

import com.example.bookshelfapi.BuildConfig
import com.example.bookshelfapi.core.network.api.OpenLibraryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** Open Library asks clients to identify themselves with a User-Agent. */
internal const val USER_AGENT =
    "Bookshelf/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        createRetrofit(BuildConfig.API_BASE_URL, okHttpClient, json)

    @Provides
    @Singleton
    fun provideOpenLibraryApi(retrofit: Retrofit): OpenLibraryApi = retrofit.create()

    internal fun createRetrofit(
        baseUrl: String,
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}

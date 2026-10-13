package com.example.bookshelfapi.core.data.di

import android.content.Context
import com.example.bookshelfapi.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

private const val COVERS_DIRECTORY = "covers"

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** The folder in internal storage where covers of saved books are kept. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CoversDirectory

/** The base URL that cover image URLs are built from. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CoversBaseUrl

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @CoversDirectory
    fun provideCoversDirectory(@ApplicationContext context: Context): File =
        File(context.filesDir, COVERS_DIRECTORY)

    @Provides
    @CoversBaseUrl
    fun provideCoversBaseUrl(): String = BuildConfig.COVERS_BASE_URL
}

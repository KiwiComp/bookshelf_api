package com.example.bookshelfapi.core.database.di

import android.content.Context
import androidx.room3.Room
import com.example.bookshelfapi.core.database.AppDatabase
import com.example.bookshelfapi.core.database.dao.BookDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "bookshelf.db"

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder<AppDatabase>(context, DATABASE_NAME).build()

    @Provides
    fun provideBookDao(database: AppDatabase): BookDao = database.bookDao()
}

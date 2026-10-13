package com.example.bookshelfapi.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.example.bookshelfapi.core.database.dao.BookDao
import com.example.bookshelfapi.core.database.entity.BookEntity

@Database(entities = [BookEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}

package com.example.bookshelfapi.core.testing

import com.example.bookshelfapi.core.data.di.DataModule
import com.example.bookshelfapi.core.data.repository.BookRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
abstract class TestDataModule {

    @Binds
    abstract fun bindBookRepository(repository: FakeBookRepository): BookRepository
}

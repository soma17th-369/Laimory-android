package com.soma369.laimory.core.collection.di

import android.content.Context
import androidx.room.Room
import com.soma369.laimory.core.collection.database.CollectionDatabase
import com.soma369.laimory.core.collection.database.MIGRATION_1_2
import com.soma369.laimory.core.collection.database.SourceItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CollectionDatabaseModule {
    @Provides
    @Singleton
    fun provideCollectionDatabase(
        @ApplicationContext context: Context,
    ): CollectionDatabase =
        Room
            .databaseBuilder(context, CollectionDatabase::class.java, CollectionDatabase.NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideSourceItemDao(database: CollectionDatabase): SourceItemDao = database.sourceItemDao()
}

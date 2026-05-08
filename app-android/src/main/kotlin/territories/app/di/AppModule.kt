package territories.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import territories.app.data.GameConfigHolder
import territories.app.data.GameRepository
import territories.app.data.db.AppDatabase
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "territories_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGameConfigHolder(): GameConfigHolder = GameConfigHolder()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "territories.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideGameRepository(db: AppDatabase): GameRepository =
        GameRepository(db.gameDao(), db.moveDao())
}

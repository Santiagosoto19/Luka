package co.luka.di

import android.content.Context
import co.luka.data.db.DatabaseFactory
import co.luka.data.db.LukaDatabase
import co.luka.data.db.Seed
import co.luka.data.security.DatabasePassphrase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
        passphrase: DatabasePassphrase,
    ): LukaDatabase {
        val db = DatabaseFactory.open(context, passphrase.getOrCreate())
        runBlocking {
            if (db.categoryDao().count() == 0) db.categoryDao().insertAll(Seed.categories())
            if (db.sourceDao().count() == 0) db.sourceDao().insertAll(Seed.sources())
        }
        return db
    }
}

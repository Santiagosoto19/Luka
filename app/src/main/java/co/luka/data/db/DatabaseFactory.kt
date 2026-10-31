package co.luka.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseFactory {
    fun open(context: Context, passphrase: ByteArray): LukaDatabase {
        try {
            System.loadLibrary("sqlcipher")
        } catch (error: UnsatisfiedLinkError) {
            throw IllegalStateException("No se pudo cargar SQLCipher. Luka no abre la base en claro.", error)
        }
        return Room.databaseBuilder(context, LukaDatabase::class.java, "luka.db")
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .addMigrations(LukaMigrations.partialIndexes)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    LukaMigrations.createIndexes(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.query("PRAGMA cipher_version").use { cursor ->
                        val version = if (cursor.moveToFirst()) cursor.getString(0) else null
                        if (version.isNullOrBlank()) {
                            throw IllegalStateException(
                                "SQLCipher no respondió a PRAGMA cipher_version. Luka no abre la base en claro.",
                            )
                        }
                    }
                }
            })
            .build()
    }
}

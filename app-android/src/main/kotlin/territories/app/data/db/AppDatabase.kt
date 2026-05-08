package territories.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities  = [GameEntity::class, MoveEntity::class],
    version   = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
    abstract fun moveDao(): MoveDao
}

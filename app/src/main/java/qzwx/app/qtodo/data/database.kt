package qzwx.app.qtodo.data

import android.content.Context
import androidx.room.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@TypeConverters(Converters::class)
@Database(
    entities = [Todo::class,Diary::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao() : TodoDao
    abstract fun diaryDao() : DiaryDao

    companion object {
        @Volatile
        private var INSTANCE : AppDatabase? = null

        fun getDatabase(context : Context) : AppDatabase {
            return INSTANCE?:synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qtodo_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDate(value : LocalDate?) : String? {
        return value?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(value : String?) : LocalDate? {
        return value?.let {
            LocalDate.parse(
                it,
                dateFormatter
            )
        }
    }

    @TypeConverter
    fun fromLocalDateTime(value : LocalDateTime?) : String? {
        return value?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun toLocalDateTime(value : String?) : LocalDateTime? {
        return value?.let {
            LocalDateTime.parse(
                it,
                dateTimeFormatter
            )
        }
    }
}
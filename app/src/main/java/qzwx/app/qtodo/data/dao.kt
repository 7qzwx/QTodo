package qzwx.app.qtodo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TodoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: Todo): Long
    
    @Update
    suspend fun updateTodo(todo: Todo)
    
    @Delete
    suspend fun deleteTodo(todo: Todo)
    
    @Query("SELECT * FROM todos WHERE id = :id")
    fun getTodoById(id: Long): Flow<Todo?>
    
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, priority DESC, dueDate ASC, createdAt DESC")
    fun getAllTodos(): Flow<List<Todo>>
    
    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY priority DESC, dueDate ASC, createdAt DESC")
    fun getActiveTodos(): Flow<List<Todo>>
    
    @Query("SELECT * FROM todos WHERE isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedTodos(): Flow<List<Todo>>
}

@Dao
interface DiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiary(diary: Diary): Long
    
    @Update
    suspend fun updateDiary(diary: Diary)
    
    @Delete
    suspend fun deleteDiary(diary: Diary)
    
    @Query("SELECT * FROM diaries WHERE id = :id")
    fun getDiaryById(id: Long): Flow<Diary?>
    
    @Query("SELECT * FROM diaries ORDER BY date DESC")
    fun getAllDiaries(): Flow<List<Diary>>
    
    @Query("SELECT * FROM diaries WHERE date = :date")
    fun getDiaryByDate(date: LocalDate): Flow<Diary?>
    
    @Query("SELECT * FROM diaries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getDiariesBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Diary>>
}


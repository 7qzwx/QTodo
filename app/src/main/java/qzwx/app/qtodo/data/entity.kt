package qzwx.app.qtodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 0, // 0: 低, 1: 中, 2: 高
    val dueDate: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity(tableName = "diaries")
data class Diary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate = LocalDate.now(),
    val content: String,
    val mood: Int = 0, // 0: 一般, 1: 开心, 2: 难过, 3: 兴奋, 4: 疲惫
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)


package qzwx.app.qtodo.widgets

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import qzwx.app.qtodo.data.AppDatabase
import qzwx.app.qtodo.data.Diary
import qzwx.app.qtodo.data.Todo
import java.time.LocalDateTime

/**
 * 小组件数据服务类，用于获取待办事项和日记数据
 */
class WidgetDataService(private val context: Context) {

    private val TAG = "WidgetDataService"

    // 获取活跃的待办事项列表（未完成的）
    fun getActiveTodos(limit: Int = 5): List<Todo> {
        return try {
            val database = AppDatabase.getDatabase(context)
            runBlocking {
                withContext(Dispatchers.IO) {
                    database.todoDao().getActiveTodos().first().take(limit)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取待办事项失败", e)
            emptyList()
        }
    }

    // 获取今日已完成的待办事项列表
    fun getTodayCompletedTodos(limit: Int = 2): List<Todo> {
        return try {
            val database = AppDatabase.getDatabase(context)
            runBlocking {
                withContext(Dispatchers.IO) {
                    // 获取所有已完成的待办事项
                    val allCompleted = database.todoDao().getCompletedTodos().first()
                    
                    // 今天的开始时间（凌晨0点）
                    val today = LocalDateTime.now().toLocalDate().atStartOfDay()
                    
                    // 过滤出今天完成的待办事项（根据更新时间）
                    val todayCompleted = allCompleted.filter { it.updatedAt.isAfter(today) }
                    
                    // 返回限定数量的结果
                    todayCompleted.take(limit)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取今日已完成待办事项失败", e)
            emptyList()
        }
    }

    // 获取最近的日记列表
    fun getRecentDiaries(limit: Int = 3): List<Diary> {
        return try {
            val database = AppDatabase.getDatabase(context)
            runBlocking {
                withContext(Dispatchers.IO) {
                    database.diaryDao().getAllDiaries().first().take(limit)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取日记失败", e)
            emptyList()
        }
    }

    // 获取待办事项的总数
    fun getTodoCount(): Pair<Int, Int> {
        return try {
            val database = AppDatabase.getDatabase(context)
            runBlocking {
                withContext(Dispatchers.IO) {
                    val allTodos = database.todoDao().getAllTodos().first()
                    val activeTodos = allTodos.filter { !it.isCompleted }
                    Pair(activeTodos.size, allTodos.size)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取待办事项计数失败", e)
            Pair(0, 0)
        }
    }
}
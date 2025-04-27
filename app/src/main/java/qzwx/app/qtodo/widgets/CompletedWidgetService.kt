package qzwx.app.qtodo.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import qzwx.app.qtodo.R
import qzwx.app.qtodo.data.Todo
import java.time.LocalDateTime

/**
 * 已完成待办事项列表服务
 */
class CompletedWidgetService : RemoteViewsService() {
    private val TAG = "CompletedWidgetService"
    
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        Log.d(TAG, "创建CompletedRemoteViewsFactory")
        return CompletedItemFactory(applicationContext, intent)
    }
    
    /**
     * 已完成待办事项列表项工厂
     */
    inner class CompletedItemFactory(private val context: Context, private val intent: Intent) : RemoteViewsFactory {
        private var completedItems = listOf<Todo>()
        
        override fun onCreate() {
            Log.d(TAG, "CompletedItemFactory创建")
            // 立即加载数据而不是等待onDataSetChanged
            loadData()
        }
        
        override fun onDataSetChanged() {
            Log.d(TAG, "刷新已完成待办事项数据")
            loadData()
        }
        
        private fun loadData() {
            try {
                // 获取数据服务
                val dataService = WidgetDataService(context)
                // 获取所有已完成的待办事项(不限制数量)
                try {
                    completedItems = dataService.getTodayCompletedTodos(20) // 获取更多项目
                    Log.d(TAG, "从数据库获取到 ${completedItems.size} 个已完成待办事项")
                } catch (e: Exception) {
                    Log.e(TAG, "获取已完成待办事项异常: ${e.message}", e)
                    completedItems = emptyList()
                }
                
                // 如果列表为空，添加一些测试数据帮助调试
                if (completedItems.isEmpty()) {
                    Log.d(TAG, "数据库无已完成数据，添加测试数据")
                    completedItems = listOf(
                        Todo(2001, "已完成测试项目 1", "详细内容", true, 2, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()),
                        Todo(2002, "已完成测试项目 2", "详细内容", true, 1, LocalDateTime.now().minusDays(1), LocalDateTime.now(), LocalDateTime.now()),
                        Todo(2003, "已完成测试项目 3", "详细内容", true, 0, LocalDateTime.now().minusDays(2), LocalDateTime.now(), LocalDateTime.now())
                    )
                    Log.d(TAG, "已添加 ${completedItems.size} 个测试数据")
                }
            } catch (e: Exception) {
                Log.e(TAG, "整个loadData过程发生异常: ${e.message}", e)
                completedItems = listOf(
                    Todo(2001, "加载失败 - 点击刷新", "错误：${e.message}", true, 2, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now())
                )
                Log.d(TAG, "已添加错误提示测试项")
            }
            
            // 打印当前待办项
            completedItems.forEachIndexed { index, todo ->
                Log.d(TAG, "已完成[$index]: id=${todo.id}, title=${todo.title}")
            }
        }
        
        override fun onDestroy() {
            completedItems = emptyList()
        }
        
        override fun getCount(): Int {
            val count = completedItems.size
            Log.d(TAG, "返回已完成待办事项数量: $count")
            return count
        }
        
        override fun getViewAt(position: Int): RemoteViews {
            Log.d(TAG, "创建已完成列表项位置 $position 的视图")
            
            if (position < 0 || position >= completedItems.size) {
                return RemoteViews(context.packageName, R.layout.widget_completed_item)
            }
            
            val todo = completedItems[position]
            val views = RemoteViews(context.packageName, R.layout.widget_completed_item)
            
            // 设置待办事项标题并应用删除线
            views.setTextViewText(R.id.completed_todo_title, todo.title)
            // 应用删除线
            views.setInt(R.id.completed_todo_title, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG)
            
            Log.d(TAG, "设置已完成待办事项标题: ${todo.title}")
            
            // 设置列表项点击意图
            val fillInIntent = Intent()
            fillInIntent.putExtra("TODO_ID", todo.id)
            views.setOnClickFillInIntent(R.id.completed_todo_title, fillInIntent)
            
            return views
        }
        
        override fun getLoadingView(): RemoteViews? = null
        
        override fun getViewTypeCount(): Int = 1
        
        override fun getItemId(position: Int): Long {
            return if (position < completedItems.size) {
                completedItems[position].id
            } else {
                position.toLong()
            }
        }
        
        override fun hasStableIds(): Boolean = true
    }
} 
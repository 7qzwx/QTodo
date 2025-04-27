package qzwx.app.qtodo.widgets

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import qzwx.app.qtodo.R
import qzwx.app.qtodo.data.Todo
import java.time.LocalDateTime

/**
 * 小组件列表内容服务
 */
class WidgetRemoteViewsService : RemoteViewsService() {
    private val TAG = "WidgetRemoteViewsService"
    
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        Log.d(TAG, "创建RemoteViewsFactory")
        return TodoItemFactory(applicationContext, intent)
    }
    
    /**
     * 待办事项列表项工厂
     */
    inner class TodoItemFactory(private val context: Context, private val intent: Intent) : RemoteViewsFactory {
        private var todoItems = listOf<Todo>()
        
        override fun onCreate() {
            Log.d(TAG, "TodoItemFactory创建")
            // 立即加载数据而不是等待onDataSetChanged
            loadData()
        }
        
        override fun onDataSetChanged() {
            Log.d(TAG, "刷新待办事项数据")
            loadData()
        }
        
        private fun loadData() {
            try {
                // 获取数据服务
                val dataService = WidgetDataService(context)
                // 获取活跃的待办事项
                try {
                    todoItems = dataService.getActiveTodos(20) // 获取更多项目
                    Log.d(TAG, "从数据库获取到 ${todoItems.size} 个待办事项")
                } catch (e: Exception) {
                    Log.e(TAG, "获取待办事项异常: ${e.message}", e)
                    todoItems = emptyList()
                }
                
                // 始终添加一些测试数据用于调试
                if (todoItems.isEmpty()) {
                    Log.d(TAG, "数据库无数据，添加测试待办事项")
                    todoItems = listOf(
                        Todo(1001, "测试待办事项 1", "详细内容", false, 2, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()),
                        Todo(1002, "测试待办事项 2", "详细内容", false, 1, LocalDateTime.now().plusDays(1), LocalDateTime.now(), LocalDateTime.now()),
                        Todo(1003, "测试待办事项 3", "详细内容", false, 0, LocalDateTime.now().plusDays(2), LocalDateTime.now(), LocalDateTime.now())
                    )
                    Log.d(TAG, "已添加 ${todoItems.size} 个测试数据")
                }
            } catch (e: Exception) {
                Log.e(TAG, "整个loadData过程发生异常: ${e.message}", e)
                todoItems = listOf(
                    Todo(1001, "加载失败 - 点击刷新", "错误：${e.message}", false, 2, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now())
                )
                Log.d(TAG, "已添加错误提示测试项")
            }
            
            // 打印当前待办项
            todoItems.forEachIndexed { index, todo ->
                Log.d(TAG, "待办[$index]: id=${todo.id}, title=${todo.title}")
            }
        }
        
        override fun onDestroy() {
            todoItems = emptyList()
        }
        
        override fun getCount(): Int {
            val count = todoItems.size
            Log.d(TAG, "返回待办事项数量: $count")
            return count
        }
        
        override fun getViewAt(position: Int): RemoteViews {
            Log.d(TAG, "创建列表项位置 $position 的视图")
            
            if (position < 0 || position >= todoItems.size) {
                return RemoteViews(context.packageName, R.layout.widget_todo_item)
            }
            
            val todo = todoItems[position]
            val views = RemoteViews(context.packageName, R.layout.widget_todo_item)
            
            // 设置待办事项标题
            views.setTextViewText(R.id.todo_item_title, todo.title)
            Log.d(TAG, "设置待办事项标题: ${todo.title}")
            
            // 设置列表项点击意图
            val fillInIntent = Intent()
            fillInIntent.putExtra("TODO_ID", todo.id)
            views.setOnClickFillInIntent(R.id.todo_item_title, fillInIntent)
            
            return views
        }
        
        override fun getLoadingView(): RemoteViews? = null
        
        override fun getViewTypeCount(): Int = 1
        
        override fun getItemId(position: Int): Long {
            return if (position < todoItems.size) {
                todoItems[position].id
            } else {
                position.toLong()
            }
        }
        
        override fun hasStableIds(): Boolean = true
    }
} 
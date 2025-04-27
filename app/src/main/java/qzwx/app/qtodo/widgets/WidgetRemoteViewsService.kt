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
        return TodoItemFactory(applicationContext, intent)
    }
    
    /**
     * 待办事项列表项工厂
     */
    inner class TodoItemFactory(private val context: Context, private val intent: Intent) : RemoteViewsFactory {
        private var todoItems = listOf<Todo>()
        
        override fun onCreate() {
            // 立即加载数据而不是等待onDataSetChanged
            loadData()
        }
        
        override fun onDataSetChanged() {
            loadData()
        }
        
        private fun loadData() {
            try {
                // 获取数据服务
                val dataService = WidgetDataService(context)
                // 获取活跃的待办事项
                try {
                    todoItems = dataService.getActiveTodos(20) // 获取更多项目
                } catch (e: Exception) {
                    Log.e(TAG, "获取待办事项异常", e)
                    todoItems = emptyList()
                }
                
                // 如果列表为空但处于开发调试模式，添加测试数据
                if (todoItems.isEmpty() && context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                    todoItems = listOf(
                        Todo(1001, "测试待办事项 1", "详细内容", false, 2, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()),
                        Todo(1002, "测试待办事项 2", "详细内容", false, 1, LocalDateTime.now().plusDays(1), LocalDateTime.now(), LocalDateTime.now()),
                        Todo(1003, "测试待办事项 3", "详细内容", false, 0, LocalDateTime.now().plusDays(2), LocalDateTime.now(), LocalDateTime.now())
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载待办事项失败", e)
                todoItems = emptyList()
            }
        }
        
        override fun onDestroy() {
            todoItems = emptyList()
        }
        
        override fun getCount(): Int {
            return todoItems.size
        }
        
        override fun getViewAt(position: Int): RemoteViews {
            if (position < 0 || position >= todoItems.size) {
                return RemoteViews(context.packageName, R.layout.widget_todo_item)
            }
            
            val todo = todoItems[position]
            val views = RemoteViews(context.packageName, R.layout.widget_todo_item)
            
            // 设置待办事项标题
            views.setTextViewText(R.id.todo_item_title, todo.title)
            
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
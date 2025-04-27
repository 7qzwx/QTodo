package qzwx.app.qtodo.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import qzwx.app.qtodo.R

/**
 * 简单的待办事项小组件
 */
class SimpleAppWidget : AppWidgetProvider() {
    private val TAG = "SimpleAppWidget"
    
    companion object {
        const val ACTION_REFRESH_WIDGET = "qzwx.app.qtodo.widgets.ACTION_REFRESH_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "小组件更新被触发，共有 ${appWidgetIds.size} 个实例")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    /**
     * 接收广播事件
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // 处理刷新操作
        if (ACTION_REFRESH_WIDGET == intent.action) {
            Log.d(TAG, "收到手动刷新广播")
            // 获取所有小组件ID
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, SimpleAppWidget::class.java)
            )
            
            // 更新所有小组件
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    /**
     * 更新小组件内容
     */
    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            Log.d(TAG, "开始更新小组件 ID: $appWidgetId")
            
            // 创建小组件视图
            val views = RemoteViews(context.packageName, R.layout.widget_simple)
            
            // 设置默认文本
            views.setTextViewText(R.id.widget_title, "QTodo待办事项")
            
            // 设置设置按钮点击事件
            val settingsIntent = Intent(context, WidgetSettingsActivity::class.java)
            settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val pendingSettingsIntent = PendingIntent.getActivity(
                context, 
                appWidgetId, 
                settingsIntent, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            views.setOnClickPendingIntent(R.id.widget_btn_settings, pendingSettingsIntent)
            
            // 设置刷新按钮点击事件
            val refreshIntent = Intent(context, SimpleAppWidget::class.java)
            refreshIntent.action = ACTION_REFRESH_WIDGET
            val pendingRefreshIntent = PendingIntent.getBroadcast(
                context, 
                appWidgetId, 
                refreshIntent, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, pendingRefreshIntent)
            
            // 获取数据服务
            val dataService = WidgetDataService(context)
            
            // 更新待办事项计数
            try {
                val (activeTodos, totalTodos) = dataService.getTodoCount()
                views.setTextViewText(R.id.widget_todo_count, "$activeTodos/$totalTodos 项任务")
                Log.d(TAG, "待办事项计数: $activeTodos/$totalTodos")
            } catch (e: Exception) {
                Log.e(TAG, "获取待办事项计数失败: ${e.message}")
                views.setTextViewText(R.id.widget_todo_count, "0/0 项任务")
            }
            
            // 先隐藏所有待办事项文本
            views.setViewVisibility(R.id.todo_title_1, View.GONE)
            views.setViewVisibility(R.id.todo_title_2, View.GONE)
            views.setViewVisibility(R.id.todo_title_3, View.GONE)
            
            // 获取活跃的待办事项
            try {
                val todos = dataService.getActiveTodos(3)
                Log.d(TAG, "获取到 ${todos.size} 个待办事项")
                
                // 显示待办事项
                if (todos.isEmpty()) {
                    views.setViewVisibility(R.id.no_todos_text, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.no_todos_text, View.GONE)
                    
                    // 更新第一个待办事项
                    if (todos.size > 0) {
                        views.setViewVisibility(R.id.todo_title_1, View.VISIBLE)
                        views.setTextViewText(R.id.todo_title_1, todos[0].title)
                        Log.d(TAG, "设置待办事项1: ${todos[0].title}")
                    }
                    
                    // 更新第二个待办事项
                    if (todos.size > 1) {
                        views.setViewVisibility(R.id.todo_title_2, View.VISIBLE)
                        views.setTextViewText(R.id.todo_title_2, todos[1].title)
                        Log.d(TAG, "设置待办事项2: ${todos[1].title}")
                    }
                    
                    // 更新第三个待办事项
                    if (todos.size > 2) {
                        views.setViewVisibility(R.id.todo_title_3, View.VISIBLE)
                        views.setTextViewText(R.id.todo_title_3, todos[2].title)
                        Log.d(TAG, "设置待办事项3: ${todos[2].title}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理待办事项失败: ${e.message}")
                views.setViewVisibility(R.id.no_todos_text, View.VISIBLE)
            }
            
            // 更新小组件
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d(TAG, "小组件更新完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "小组件更新过程中发生异常: ${e.message}")
        }
    }
}
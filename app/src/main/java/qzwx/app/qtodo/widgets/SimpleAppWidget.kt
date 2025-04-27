package qzwx.app.qtodo.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
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
        const val EXTRA_ITEM_POSITION = "qzwx.app.qtodo.widgets.EXTRA_ITEM_POSITION"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }
    
    /**
     * 接收广播事件
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_REFRESH_WIDGET -> {
                // 获取所有小组件ID
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, SimpleAppWidget::class.java)
                )
                
                // 重要：先通知数据集变化，再更新小组件
                for (appWidgetId in appWidgetIds) {
                    try {
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.todo_list_view)
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.completed_list_view)
                    } catch (e: Exception) {
                        Log.e(TAG, "通知数据变化失败", e)
                    }
                }
                
                // 更新所有小组件
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            "qzwx.app.qtodo.widgets.ACTION_SHOW_ACTIVE" -> {
                // 显示待办列表
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = RemoteViews(context.packageName, R.layout.widget_simple_v2)
                    
                    // 更新UI状态
                    views.setViewVisibility(R.id.todo_list_view, View.VISIBLE)
                    views.setViewVisibility(R.id.completed_list_view, View.GONE)
                    views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE)
                    views.setViewVisibility(R.id.completed_empty_view, View.GONE)
                    
                    // 更新按钮样式
                    views.setTextColor(R.id.btn_show_active, context.resources.getColor(android.R.color.white))
                    views.setTextColor(R.id.btn_show_completed, context.resources.getColor(android.R.color.darker_gray))
                    views.setInt(R.id.btn_show_active, "setBackgroundColor", context.resources.getColor(android.R.color.holo_green_light))
                    views.setInt(R.id.btn_show_completed, "setBackgroundColor", context.resources.getColor(android.R.color.white))
                    
                    // 更新小组件
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    
                    // 刷新数据
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.todo_list_view)
                }
            }
            "qzwx.app.qtodo.widgets.ACTION_SHOW_COMPLETED" -> {
                // 显示已完成列表
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = RemoteViews(context.packageName, R.layout.widget_simple_v2)
                    
                    // 更新UI状态
                    views.setViewVisibility(R.id.todo_list_view, View.GONE)
                    views.setViewVisibility(R.id.completed_list_view, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_empty_view, View.GONE)
                    views.setViewVisibility(R.id.completed_empty_view, View.VISIBLE)
                    
                    // 更新按钮样式
                    views.setTextColor(R.id.btn_show_active, context.resources.getColor(android.R.color.darker_gray))
                    views.setTextColor(R.id.btn_show_completed, context.resources.getColor(android.R.color.white))
                    views.setInt(R.id.btn_show_active, "setBackgroundColor", context.resources.getColor(android.R.color.white))
                    views.setInt(R.id.btn_show_completed, "setBackgroundColor", context.resources.getColor(android.R.color.holo_green_light))
                    
                    // 更新小组件
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    
                    // 刷新数据
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.completed_list_view)
                }
            }
            Intent.ACTION_VIEW -> {
                // 处理列表项点击操作
                val todoId = intent.getLongExtra("TODO_ID", -1)
                if (todoId != -1L) {
                    // 打开主应用并导航到详情页
                    val mainIntent = Intent()
                    mainIntent.setClassName(context.packageName, "qzwx.app.qtodo.MainActivity")
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    mainIntent.putExtra("navigate_to", "todo_detail")
                    mainIntent.putExtra("todo_id", todoId)
                    context.startActivity(mainIntent)
                }
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
            // 创建小组件视图
            val views = RemoteViews(context.packageName, R.layout.widget_simple_v2)
            
            // 设置默认文本
            views.setTextViewText(R.id.widget_title, "QTodo待办清单")
            
            // 创建跳转到主应用的Intent
            val mainIntent = Intent()
            mainIntent.setClassName(context.packageName, "qzwx.app.qtodo.MainActivity")
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // 设置整个小组件标题点击事件 - 跳转到主页的待办清单tab
            val todoIntent = Intent(mainIntent)
            todoIntent.putExtra("navigate_to", "todo")
            
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            val pendingTodoIntent = PendingIntent.getActivity(
                context, 
                1001, 
                todoIntent, 
                pendingIntentFlags
            )
            views.setOnClickPendingIntent(R.id.widget_title, pendingTodoIntent)
            
            // 设置设置按钮点击事件
            val settingsIntent = Intent(context, WidgetSettingsActivity::class.java)
            settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pendingSettingsIntent = PendingIntent.getActivity(
                context, 
                appWidgetId, 
                settingsIntent, 
                pendingIntentFlags
            )
            views.setOnClickPendingIntent(R.id.widget_btn_settings, pendingSettingsIntent)
            
            // 设置刷新按钮点击事件
            val refreshIntent = Intent(context, SimpleAppWidget::class.java)
            refreshIntent.action = ACTION_REFRESH_WIDGET
            val pendingRefreshIntent = PendingIntent.getBroadcast(
                context, 
                appWidgetId, 
                refreshIntent, 
                pendingIntentFlags
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, pendingRefreshIntent)
            
            // 设置切换按钮的点击事件
            // 待办按钮点击
            val activeIntent = Intent(context, SimpleAppWidget::class.java)
            activeIntent.action = "qzwx.app.qtodo.widgets.ACTION_SHOW_ACTIVE"
            activeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val pendingActiveIntent = PendingIntent.getBroadcast(
                context,
                101,
                activeIntent,
                pendingIntentFlags
            )
            views.setOnClickPendingIntent(R.id.btn_show_active, pendingActiveIntent)
            
            // 已完成按钮点击
            val completedIntent = Intent(context, SimpleAppWidget::class.java)
            completedIntent.action = "qzwx.app.qtodo.widgets.ACTION_SHOW_COMPLETED"
            completedIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val pendingCompletedIntent = PendingIntent.getBroadcast(
                context,
                102,
                completedIntent,
                pendingIntentFlags
            )
            views.setOnClickPendingIntent(R.id.btn_show_completed, pendingCompletedIntent)
            
            // 设置两个ListView的适配器
            // 待办列表适配器
            val activeServiceIntent = Intent(context, WidgetRemoteViewsService::class.java)
            activeServiceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            activeServiceIntent.putExtra("LIST_TYPE", "ACTIVE")
            // 确保Intent是唯一的，添加时间戳
            activeServiceIntent.data = Uri.parse("widget://qtodo/widget_id/$appWidgetId/active?t=${System.currentTimeMillis()}")
            views.setRemoteAdapter(R.id.todo_list_view, activeServiceIntent)
            
            // 已完成列表适配器
            val completedServiceIntent = Intent(context, CompletedWidgetService::class.java)
            completedServiceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // 确保Intent是唯一的，添加时间戳
            completedServiceIntent.data = Uri.parse("widget://qtodo/widget_id/$appWidgetId/completed?t=${System.currentTimeMillis()}")
            views.setRemoteAdapter(R.id.completed_list_view, completedServiceIntent)
            
            // 设置空视图
            views.setEmptyView(R.id.todo_list_view, R.id.widget_empty_view)
            views.setEmptyView(R.id.completed_list_view, R.id.completed_empty_view)
            
            // 设置列表项点击事件模板
            val itemClickIntent = Intent(context, SimpleAppWidget::class.java)
            itemClickIntent.action = Intent.ACTION_VIEW
            itemClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // 确保Intent是唯一的
            itemClickIntent.data = Uri.parse("widget://qtodo/widget_id/$appWidgetId/item_click")
            
            val clickPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                itemClickIntent,
                pendingIntentFlags
            )
            views.setPendingIntentTemplate(R.id.todo_list_view, clickPendingIntent)
            views.setPendingIntentTemplate(R.id.completed_list_view, clickPendingIntent)
            
            // 获取数据服务
            val dataService = WidgetDataService(context)
            
            // 更新待办事项计数
            try {
                val (activeTodos, totalTodos) = dataService.getTodoCount()
                views.setTextViewText(R.id.widget_todo_count, "$activeTodos/$totalTodos 项待办事项")
            } catch (e: Exception) {
                Log.e(TAG, "获取待办事项计数失败", e)
                views.setTextViewText(R.id.widget_todo_count, "0/0 项待办事项")
            }
            
            // 先更新小组件
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // 然后通知数据变化
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.todo_list_view)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.completed_list_view)
            
        } catch (e: Exception) {
            Log.e(TAG, "小组件更新过程中发生异常", e)
            
            // 尝试显示基本内容
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_simple_v2)
                views.setTextViewText(R.id.widget_title, "QTodo待办清单")
                views.setTextViewText(R.id.widget_todo_count, "加载中...")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e2: Exception) {
                Log.e(TAG, "小组件恢复显示失败", e2)
            }
        }
    }
}
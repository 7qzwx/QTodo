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
        Log.d(TAG, "小组件更新被触发，共有 ${appWidgetIds.size} 个实例")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        Log.d(TAG, "小组件onUpdate完成")
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "小组件已启用")
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "小组件已禁用")
    }
    
    /**
     * 接收广播事件
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "小组件接收到广播：${intent.action}")
        super.onReceive(context, intent)
        
        // 处理刷新操作
        if (ACTION_REFRESH_WIDGET == intent.action) {
            Log.d(TAG, "收到手动刷新广播")
            
            // 获取所有小组件ID
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, SimpleAppWidget::class.java)
            )
            
            Log.d(TAG, "准备刷新 ${appWidgetIds.size} 个小组件")
            
            // 重要：先通知数据集变化，再更新小组件
            for (appWidgetId in appWidgetIds) {
                try {
                    Log.d(TAG, "通知小组件 $appWidgetId 数据变化")
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.todo_list_view)
                } catch (e: Exception) {
                    Log.e(TAG, "通知数据变化失败: ${e.message}", e)
                }
            }
            
            // 更新所有小组件
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        } else if (Intent.ACTION_VIEW == intent.action) {
            // 处理列表项点击操作
            val todoId = intent.getLongExtra("TODO_ID", -1)
            if (todoId != -1L) {
                Log.d(TAG, "用户点击了待办项目: ID = $todoId")
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
            Log.d(TAG, "创建RemoteViews完成")
            
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
            Log.d(TAG, "设置标题点击事件完成")
            
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
            Log.d(TAG, "设置设置按钮点击事件完成")
            
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
            Log.d(TAG, "设置刷新按钮点击事件完成")
            
            // 设置ListView的适配器
            val serviceIntent = Intent(context, WidgetRemoteViewsService::class.java)
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // 确保Intent是唯一的，添加时间戳
            serviceIntent.data = Uri.parse("widget://qtodo/widget_id/$appWidgetId?t=${System.currentTimeMillis()}")
            views.setRemoteAdapter(R.id.todo_list_view, serviceIntent)
            Log.d(TAG, "设置ListView适配器完成，URI: widget://qtodo/widget_id/$appWidgetId?t=${System.currentTimeMillis()}")
            
            // 设置空视图
            views.setEmptyView(R.id.todo_list_view, R.id.widget_empty_view)
            
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
            Log.d(TAG, "设置列表项点击事件模板完成")
            
            // 获取数据服务
            val dataService = WidgetDataService(context)
            
            // 更新待办事项计数
            try {
                val (activeTodos, totalTodos) = dataService.getTodoCount()
                views.setTextViewText(R.id.widget_todo_count, "$activeTodos/$totalTodos 项待办事项")
                Log.d(TAG, "待办事项计数: $activeTodos/$totalTodos")
            } catch (e: Exception) {
                Log.e(TAG, "获取待办事项计数失败: ${e.message}")
                views.setTextViewText(R.id.widget_todo_count, "0/0 项待办事项")
            }
            
            // 更新已完成待办事项
            try {
                val completedTodos = dataService.getTodayCompletedTodos(2)
                Log.d(TAG, "获取今日已完成待办事项: ${completedTodos.size} 项")
                
                if (completedTodos.isEmpty()) {
                    // 没有已完成的待办事项
                    views.setViewVisibility(R.id.no_completed_text, View.VISIBLE)
                    views.setViewVisibility(R.id.completed_item_1, View.GONE)
                    views.setViewVisibility(R.id.completed_item_2, View.GONE)
                } else {
                    // 隐藏提示文本
                    views.setViewVisibility(R.id.no_completed_text, View.GONE)
                    
                    // 设置第一个已完成待办事项
                    if (completedTodos.size > 0) {
                        val item1 = completedTodos[0]
                        views.setViewVisibility(R.id.completed_item_1, View.VISIBLE)
                        
                        // 应用删除线样式 - 使用设置标志的方式
                        views.setTextViewText(R.id.completed_title_1, item1.title)
                        // 注意：RemoteViews不支持直接设置字体样式，只能通过设置标志
                        views.setInt(R.id.completed_title_1, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG)
                    } else {
                        views.setViewVisibility(R.id.completed_item_1, View.GONE)
                    }
                    
                    // 设置第二个已完成待办事项
                    if (completedTodos.size > 1) {
                        val item2 = completedTodos[1]
                        views.setViewVisibility(R.id.completed_item_2, View.VISIBLE)
                        
                        // 应用删除线样式 - 使用设置标志的方式
                        views.setTextViewText(R.id.completed_title_2, item2.title)
                        // 注意：RemoteViews不支持直接设置字体样式，只能通过设置标志
                        views.setInt(R.id.completed_title_2, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG)
                    } else {
                        views.setViewVisibility(R.id.completed_item_2, View.GONE)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取今日已完成待办事项失败: ${e.message}", e)
                views.setViewVisibility(R.id.no_completed_text, View.VISIBLE)
                views.setViewVisibility(R.id.completed_item_1, View.GONE)
                views.setViewVisibility(R.id.completed_item_2, View.GONE)
            }
            
            // 先更新小组件
            Log.d(TAG, "准备更新AppWidget: $appWidgetId")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // 然后通知数据变化
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.todo_list_view)
            Log.d(TAG, "小组件更新完成，已通知数据变化")
            
        } catch (e: Exception) {
            Log.e(TAG, "小组件更新过程中发生异常: ${e.message}")
            e.printStackTrace()
            
            // 尝试显示基本内容
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_simple)
                views.setTextViewText(R.id.widget_title, "QTodo待办清单")
                views.setTextViewText(R.id.widget_todo_count, "加载中...")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "小组件异常后恢复显示")
            } catch (e2: Exception) {
                Log.e(TAG, "小组件恢复显示失败: ${e2.message}")
            }
        }
    }
}
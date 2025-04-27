# QTodo 桌面小组件开发文档

## 1. 概述

本文档记录了QTodo应用的桌面小组件开发过程，包括创建过程、实现细节和后续开发计划。

## 2. 小组件基本信息

- 小组件名称：SimpleAppWidget
- 大小：4×2
- 类型：信息显示小组件
- 更新周期：30分钟（可配置）

## 3. 目录结构

```
app/src/main/
├── java/qzwx/app/qtodo/widgets/       # 小组件相关代码
│   ├── SimpleAppWidget.kt            # 小组件主类
│   ├── WidgetDataService.kt          # 小组件数据服务
│   └── docs/                         # 文档
│       └── Widget_Development_Guide.md  # 开发说明文档
│
├── res/
│   ├── drawable/
│   │   ├── widget_background.xml     # 小组件背景
│   │   └── widget_preview.xml        # 小组件预览图
│   ├── layout/
│   │   └── widget_simple.xml         # 小组件布局
│   └── xml/
│       └── simple_app_widget_info.xml # 小组件配置信息
│
└── AndroidManifest.xml               # 应用清单（包含小组件注册）
```

## 4. 开发步骤

### 4.1 创建小组件文件夹结构

首先创建专门的文件夹来存放小组件相关代码：

```bash
mkdir -p app/src/main/java/qzwx/app/qtodo/widgets
```

### 4.2 创建小组件实现类

创建`SimpleAppWidget.kt`类，继承自`AppWidgetProvider`：

```kotlin
package qzwx.app.qtodo.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import qzwx.app.qtodo.R
import java.time.format.DateTimeFormatter

class SimpleAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // 创建小组件视图
        val views = RemoteViews(context.packageName, R.layout.widget_simple)
        
        // 获取数据服务
        val dataService = WidgetDataService(context)
        
        // 更新待办事项计数
        val (activeTodos, totalTodos) = dataService.getTodoCount()
        views.setTextViewText(R.id.widget_todo_count, "$activeTodos/$totalTodos")
        
        // 获取活跃的待办事项
        val todos = dataService.getActiveTodos(3)
        
        // 显示待办事项
        if (todos.isEmpty()) {
            views.setViewVisibility(R.id.no_todos_text, View.VISIBLE)
            views.setViewVisibility(R.id.todo_item_1, View.GONE)
            views.setViewVisibility(R.id.todo_item_2, View.GONE)
            views.setViewVisibility(R.id.todo_item_3, View.GONE)
        } else {
            views.setViewVisibility(R.id.no_todos_text, View.GONE)
            
            // 更新第一个待办事项
            if (todos.size > 0) {
                views.setViewVisibility(R.id.todo_item_1, View.VISIBLE)
                views.setTextViewText(R.id.todo_title_1, todos[0].title)
                
                // 设置优先级颜色
                val priorityColor = when (todos[0].priority) {
                    2 -> "#FF5722" // 高优先级：红色
                    1 -> "#FF9800" // 中优先级：橙色
                    else -> "#4CAF50" // 低优先级：绿色
                }
                views.setInt(R.id.todo_priority_1, "setBackgroundColor", android.graphics.Color.parseColor(priorityColor))
            } else {
                views.setViewVisibility(R.id.todo_item_1, View.GONE)
            }
            
            // 更新第二个待办事项
            if (todos.size > 1) {
                views.setViewVisibility(R.id.todo_item_2, View.VISIBLE)
                views.setTextViewText(R.id.todo_title_2, todos[1].title)
                
                // 设置优先级颜色
                val priorityColor = when (todos[1].priority) {
                    2 -> "#FF5722" // 高优先级：红色
                    1 -> "#FF9800" // 中优先级：橙色
                    else -> "#4CAF50" // 低优先级：绿色
                }
                views.setInt(R.id.todo_priority_2, "setBackgroundColor", android.graphics.Color.parseColor(priorityColor))
            } else {
                views.setViewVisibility(R.id.todo_item_2, View.GONE)
            }
            
            // 更新第三个待办事项
            if (todos.size > 2) {
                views.setViewVisibility(R.id.todo_item_3, View.VISIBLE)
                views.setTextViewText(R.id.todo_title_3, todos[2].title)
                
                // 设置优先级颜色
                val priorityColor = when (todos[2].priority) {
                    2 -> "#FF5722" // 高优先级：红色
                    1 -> "#FF9800" // 中优先级：橙色
                    else -> "#4CAF50" // 低优先级：绿色
                }
                views.setInt(R.id.todo_priority_3, "setBackgroundColor", android.graphics.Color.parseColor(priorityColor))
            } else {
                views.setViewVisibility(R.id.todo_item_3, View.GONE)
            }
        }
        
        // 获取最近日记
        val diaries = dataService.getRecentDiaries(2)
        
        // 显示日记
        if (diaries.isEmpty()) {
            views.setViewVisibility(R.id.no_diaries_text, View.VISIBLE)
            views.setViewVisibility(R.id.diary_item_1, View.GONE)
            views.setViewVisibility(R.id.diary_item_2, View.GONE)
        } else {
            views.setViewVisibility(R.id.no_diaries_text, View.GONE)
            
            // 日期格式化
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            
            // 更新第一个日记
            if (diaries.size > 0) {
                views.setViewVisibility(R.id.diary_item_1, View.VISIBLE)
                views.setTextViewText(R.id.diary_date_1, diaries[0].date.format(dateFormatter))
                views.setTextViewText(R.id.diary_content_1, diaries[0].content)
            } else {
                views.setViewVisibility(R.id.diary_item_1, View.GONE)
            }
            
            // 更新第二个日记
            if (diaries.size > 1) {
                views.setViewVisibility(R.id.diary_item_2, View.VISIBLE)
                views.setTextViewText(R.id.diary_date_2, diaries[1].date.format(dateFormatter))
                views.setTextViewText(R.id.diary_content_2, diaries[1].content)
            } else {
                views.setViewVisibility(R.id.diary_item_2, View.GONE)
            }
        }
        
        // 更新小组件
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
```

### 4.3 创建小组件数据服务

创建`WidgetDataService.kt`类，负责从数据库获取数据：

```kotlin
package qzwx.app.qtodo.widgets

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import qzwx.app.qtodo.data.AppDatabase
import qzwx.app.qtodo.data.Diary
import qzwx.app.qtodo.data.Todo

/**
 * 小组件数据服务类，用于获取待办事项和日记数据
 */
class WidgetDataService(private val context: Context) {

    // 获取活跃的待办事项列表（未完成的）
    fun getActiveTodos(limit: Int = 5): List<Todo> {
        val database = AppDatabase.getDatabase(context)
        return runBlocking {
            withContext(Dispatchers.IO) {
                database.todoDao().getActiveTodos().first().take(limit)
            }
        }
    }

    // 获取最近的日记列表
    fun getRecentDiaries(limit: Int = 3): List<Diary> {
        val database = AppDatabase.getDatabase(context)
        return runBlocking {
            withContext(Dispatchers.IO) {
                database.diaryDao().getAllDiaries().first().take(limit)
            }
        }
    }

    // 获取待办事项的总数
    fun getTodoCount(): Pair<Int, Int> {
        val database = AppDatabase.getDatabase(context)
        return runBlocking {
            withContext(Dispatchers.IO) {
                val allTodos = database.todoDao().getAllTodos().first()
                val activeTodos = allTodos.filter { !it.isCompleted }
                Pair(activeTodos.size, allTodos.size)
            }
        }
    }
}
```

### 4.4 创建小组件布局

创建`widget_simple.xml`布局文件，包含待办事项和日记显示区域：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="12dp"
    android:background="@drawable/widget_background">

    <!-- 头部标题和统计信息 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingBottom="6dp">

        <TextView
            android:id="@+id/widget_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="QTodo"
            android:textSize="16sp"
            android:textStyle="bold"
            android:textColor="#FFFFFF"/>

        <TextView
            android:id="@+id/widget_todo_count"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="0/0"
            android:textSize="14sp"
            android:textColor="#EEEEEE"/>
    </LinearLayout>

    <!-- 待办事项列表 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="#44000000"
        android:padding="8dp"
        android:layout_marginBottom="6dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="待办事项"
            android:textSize="14sp"
            android:textStyle="bold"
            android:textColor="#FFFFFF"
            android:paddingBottom="4dp"/>

        <!-- 待办事项项目模板 -->
        <!-- (省略了详细代码，请参考完整布局文件) -->
    </LinearLayout>

    <!-- 日记列表 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="#44000000"
        android:padding="8dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="最近日记"
            android:textSize="14sp"
            android:textStyle="bold"
            android:textColor="#FFFFFF"
            android:paddingBottom="4dp"/>

        <!-- 日记项目模板 -->
        <!-- (省略了详细代码，请参考完整布局文件) -->
    </LinearLayout>
</LinearLayout>
```

### 4.5 创建小组件背景

创建`widget_background.xml`背景：

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" 
    android:shape="rectangle">
    <corners android:radius="12dp" />
    <solid android:color="#333333" />
    <stroke 
        android:width="1dp"
        android:color="#444444" />
</shape>
```

### 4.6 创建小组件配置文件

创建`simple_app_widget_info.xml`配置文件：

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:initialKeyguardLayout="@layout/widget_simple"
    android:initialLayout="@layout/widget_simple"
    android:minWidth="250dp"
    android:minHeight="110dp"
    android:previewImage="@drawable/widget_preview"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="1800000"
    android:widgetCategory="home_screen">
</appwidget-provider>
```

### 4.7 注册小组件

在`AndroidManifest.xml`中注册小组件：

```xml
<!-- 注册小组件 -->
<receiver
    android:name="qzwx.app.qtodo.widgets.SimpleAppWidget"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/simple_app_widget_info" />
</receiver>
```

## 5. 小组件配置说明

在`simple_app_widget_info.xml`中配置了以下内容：

- `android:minWidth="250dp"` 和 `android:minHeight="110dp"`：定义小组件的最小尺寸，对应4×2的网格大小。
- `android:updatePeriodMillis="1800000"`：定义小组件自动更新的周期为30分钟（1800000毫秒）。
- `android:resizeMode="horizontal|vertical"`：允许用户在水平和垂直方向调整小组件大小。
- `android:widgetCategory="home_screen"`：指定小组件可以放置在主屏幕上。

## 6. 数据展示功能

小组件现在能够从数据库中读取并展示以下内容：

### 6.1 待办事项展示

- 显示待办事项的数量统计（如：5/10，表示有5个活跃待办事项，总共10个）
- 最多显示3个未完成的待办事项，按优先级排序
- 根据优先级使用不同颜色标识：
  - 高优先级：红色 (#FF5722)
  - 中优先级：橙色 (#FF9800)
  - 低优先级：绿色 (#4CAF50)
- 当没有待办事项时，显示"没有待办事项"提示

### 6.2 日记展示

- 最多显示2个最近的日记
- 显示日记的日期和内容
- 日记内容过长时会被截断并显示省略号
- 当没有日记时，显示"没有最近日记"提示

### 6.3 数据更新

- 小组件每30分钟自动从数据库更新一次数据
- 当用户添加或修改待办事项和日记时，可能需要手动刷新小组件才能看到最新内容

## 7. 开发过程中的问题及解决

在开发过程中遇到的主要问题包括：

1. **小组件类无法解析**：
   - 问题：在AndroidManifest.xml中使用相对路径`.widgets.SimpleAppWidget`导致类无法被解析
   - 解决方法：使用完整的包名`qzwx.app.qtodo.widgets.SimpleAppWidget`

2. **文件创建问题**：
   - 问题：部分文件创建后内容为空
   - 解决方法：重新创建相关文件并确保内容正确写入

3. **数据库访问**：
   - 问题：小组件需要在主线程访问数据库
   - 解决方法：使用runBlocking和withContext切换到IO线程执行数据库操作

## 8. 后续开发计划

未来小组件开发计划：

1. **添加交互功能**：
   - 允许用户通过点击小组件直接标记任务为已完成
   - 添加快速创建任务的功能

2. **数据同步**：
   - 实现小组件数据与主应用的实时同步
   - 添加手动刷新按钮

3. **主题自定义**：
   - 提供多种小组件样式供用户选择
   - 支持跟随系统暗/亮模式自动切换主题

4. **界面优化**：
   - 添加待办事项的截止日期显示
   - 优化任务列表显示，可能使用ListView小组件

5. **优化性能**：
   - 减少小组件更新对电池的消耗
   - 优化小组件渲染性能

## 9. 参考资料

- [Android 开发者文档 - App Widgets](https://developer.android.com/guide/topics/appwidgets)
- [RemoteViews 文档](https://developer.android.com/reference/android/widget/RemoteViews)
- [AppWidgetProvider 文档](https://developer.android.com/reference/android/appwidget/AppWidgetProvider) 
package qzwx.app.qtodo.page.calendarpage

// 使用Material3的滑动相关API
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import qzwx.app.qtodo.data.AppDatabase
import qzwx.app.qtodo.data.Diary
import qzwx.app.qtodo.data.Todo
import qzwx.app.qtodo.page.diarypage.AddEditDiaryDialog
import qzwx.app.qtodo.page.todopage.AddTodoDialog
import qzwx.app.qtodo.repository.DiaryRepository
import qzwx.app.qtodo.repository.TodoRepository
import qzwx.app.viewmodel.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

// 日期单元格状态
data class DateCellState(
    val hasTodo: Boolean = false,
    val hasCompletedTodo: Boolean = false,
    val hasDiary: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarPage(
    modifier: Modifier = Modifier
) {
    // 获取数据库和仓库
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val todoRepository = TodoRepository(database.todoDao())
    val diaryRepository = DiaryRepository(database.diaryDao())
    
    // 创建ViewModel
    val todoViewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(todoRepository)
    )
    val diaryViewModel: DiaryViewModel = viewModel(
        factory = DiaryViewModelFactory(diaryRepository)
    )
    
    // 日历状态
    var isWeekView by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    
    // 对话框显示状态
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var showAddDiaryDialog by remember { mutableStateOf(false) }
    
    // 收集所有待办和日记数据
    val allTodos by todoViewModel.allTodos.collectAsState(initial = emptyList())
    val allDiaries by diaryViewModel.allDiaries.collectAsState(initial = emptyList())
    
    // 为日期单元格计算状态
    val dateCellStates = remember(allTodos, allDiaries) {
        val states = mutableMapOf<LocalDate, DateCellState>()
        
        // 处理待办事项
        allTodos.forEach { todo ->
            val date = LocalDate.from(todo.createdAt)
            val currentState = states[date] ?: DateCellState()
            
            states[date] = if (todo.isCompleted) {
                currentState.copy(hasTodo = true, hasCompletedTodo = true)
            } else {
                currentState.copy(hasTodo = true)
            }
        }
        
        // 处理日记
        allDiaries.forEach { diary ->
            val date = diary.date
            val currentState = states[date] ?: DateCellState()
            states[date] = currentState.copy(hasDiary = true)
        }
        
        states
    }
    
    // 当前选中日期的待办和日记
    val selectedDateTodos = allTodos.filter { LocalDate.from(it.createdAt) == selectedDate }
    val selectedDateDiaries = allDiaries.filter { it.date == selectedDate }
    
    // 展开/折叠状态
    var todoExpanded by remember { mutableStateOf(true) }
    var diaryExpanded by remember { mutableStateOf(true) }
    
    // 添加动画效果的状态
    var animateCalendarChange by remember { mutableStateOf(false) }
    val calendarTransition = updateTransition(
        targetState = Pair(currentYearMonth, isWeekView),
        label = "CalendarTransition"
    )
    
    val calendarAlpha by calendarTransition.animateFloat(
        label = "CalendarAlpha",
        transitionSpec = { tween(300) }
    ) { _ ->
        if (animateCalendarChange) 0f else 1f
    }
    
    // 动画执行完毕后重置状态
    LaunchedEffect(calendarAlpha) {
        if (calendarAlpha == 0f) {
            animateCalendarChange = false
        }
    }
    
    // 处理添加待办的函数
    val handleAddTodo = { title: String, description: String, priority: Int, dueDate: LocalDateTime? ->
        // 创建一个以选中日期为创建日期的待办事项
        val todo = Todo(
            title = title,
            description = description,
            priority = priority,
            dueDate = dueDate,
            createdAt = selectedDate.atTime(LocalTime.now()),
            updatedAt = LocalDateTime.now()
        )
        
        // 使用仓库直接插入待办事项
        CoroutineScope(Dispatchers.IO).launch {
            todoRepository.insertTodo(todo)
        }
    }
    
    // 处理添加日记的函数
    val handleAddDiary = { date: LocalDate, content: String, mood: Int ->
        // 忽略传入的date参数，使用选中的日期
        val diary = Diary(
            date = selectedDate,
            content = content,
            mood = mood,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        // 使用仓库直接插入日记
        CoroutineScope(Dispatchers.IO).launch {
            diaryRepository.insertDiary(diary)
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CalendarTopBar(
                currentYearMonth = currentYearMonth,
                selectedDate = selectedDate,
                isWeekView = isWeekView,
                onPrevious = {
                    animateCalendarChange = true
                    if (isWeekView) {
                        selectedDate = selectedDate.minusWeeks(1)
                        currentYearMonth = YearMonth.from(selectedDate)
                    } else {
                        currentYearMonth = currentYearMonth.minusMonths(1)
                        // 确保selectedDate也在当前月份内
                        if (!selectedDate.isInSameMonthAs(currentYearMonth)) {
                            selectedDate = currentYearMonth.atDay(1)
                        }
                    }
                },
                onNext = {
                    animateCalendarChange = true
                    if (isWeekView) {
                        selectedDate = selectedDate.plusWeeks(1)
                        currentYearMonth = YearMonth.from(selectedDate)
                    } else {
                        currentYearMonth = currentYearMonth.plusMonths(1)
                        // 确保selectedDate也在当前月份内
                        if (!selectedDate.isInSameMonthAs(currentYearMonth)) {
                            selectedDate = currentYearMonth.atDay(1)
                        }
                    }
                },
                onTodayClick = {
                    animateCalendarChange = true
                    selectedDate = LocalDate.now()
                    currentYearMonth = YearMonth.now()
                }
            )
        }
    ) { paddingValues ->
        // 内容区域
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            // 使用LazyColumn替代Column
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp)
            ) {
                // 日历卡片
                item {
                    var dragOffset by remember { mutableStateOf(0f) }
                    val dragThreshold = 100f
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    dragOffset += delta
                                    // 颠倒滑动方向：下滑查看周视图，上滑查看月视图
                                    if (dragOffset > dragThreshold && isWeekView) {
                                        isWeekView = false
                                        dragOffset = 0f
                                    } else if (dragOffset < -dragThreshold && !isWeekView) {
                                        isWeekView = true
                                        dragOffset = 0f
                                    }
                                },
                                onDragStopped = { dragOffset = 0f }
                            ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        // 滑动提示
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(calendarAlpha)
                        ) {
                            AnimatedContent(
                                targetState = isWeekView,
                                transitionSpec = {
                                    if (targetState && !initialState) {
                                        // 从月视图切换到周视图
                                        (slideInVertically { height -> -height } + fadeIn()).togetherWith(
                                            slideOutVertically { height -> height } + fadeOut()
                                        )
                                    } else {
                                        // 从周视图切换到月视图
                                        (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                            slideOutVertically { height -> -height } + fadeOut()
                                        )
                                    }
                                },
                                label = "CalendarViewTransition"
                            ) { isWeek ->
                                if (isWeek) {
                                    // 周视图
                                    WeekCalendarView(
                                        selectedDate = selectedDate,
                                        onDateSelected = { selectedDate = it },
                                        dateCellStates = dateCellStates
                                    )
                                } else {
                                    // 月视图
                                    MonthCalendarView(
                                        yearMonth = currentYearMonth,
                                        selectedDate = selectedDate,
                                        onDateSelected = { selectedDate = it },
                                        dateCellStates = dateCellStates
                                    )
                                }
                            }
                        }
                        
                        // 视图模式提示
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isWeekView) "下滑查看月视图" else "上滑查看周视图",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                
                // 日期指示器
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            val dateText = when {
                                selectedDate.equals(LocalDate.now()) -> "今天"
                                selectedDate.equals(LocalDate.now().minusDays(1)) -> "昨天"
                                selectedDate.equals(LocalDate.now().plusDays(1)) -> "明天"
                                else -> selectedDate.format(DateTimeFormatter.ofPattern("MM月dd日"))
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (!selectedDate.equals(LocalDate.now())) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "周${
                                            when (selectedDate.dayOfWeek.value) {
                                                1 -> "一"
                                                2 -> "二"
                                                3 -> "三"
                                                4 -> "四"
                                                5 -> "五"
                                                6 -> "六"
                                                7 -> "日"
                                                else -> ""
                                            }
                                        }",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
                
                // 检查是否有内容
                if (selectedDateTodos.isEmpty() && selectedDateDiaries.isEmpty()) {
                    // 没有内容 - 显示添加按钮
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(48.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "这一天还没有安排",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "创建待办事项或记录日记来规划你的一天",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // 添加按钮行
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // 添加待办按钮
                                    Button(
                                        onClick = { showAddTodoDialog = true },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AddTask,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "添加待办",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    
                                    // 添加日记按钮
                                    Button(
                                        onClick = { showAddDiaryDialog = true },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "写日记",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 待办事项
                    if (selectedDateTodos.isNotEmpty()) {
                        item {
                            CollapsibleSection(
                                title = "待办事项",
                                icon = Icons.Outlined.CheckCircle,
                                iconTint = MaterialTheme.colorScheme.primary,
                                count = selectedDateTodos.size,
                                initialExpanded = todoExpanded,
                                onExpandedChange = { todoExpanded = it },
                                onAddClick = { showAddTodoDialog = true }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedDateTodos.forEach { todo ->
                                        TodoItem(todo = todo)
                                    }
                                }
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    
                    // 日记
                    if (selectedDateDiaries.isNotEmpty()) {
                        item {
                            CollapsibleSection(
                                title = "日记",
                                icon = Icons.Outlined.Book,
                                iconTint = MaterialTheme.colorScheme.tertiary,
                                count = selectedDateDiaries.size,
                                initialExpanded = diaryExpanded,
                                onExpandedChange = { diaryExpanded = it },
                                onAddClick = { showAddDiaryDialog = true }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedDateDiaries.forEach { diary ->
                                        DiaryItem(diary = diary)
                                    }
                                }
                            }
                        }
                    }
                    
                    // 添加"添加"按钮的区域
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 如果没有待办，显示添加待办按钮
                            if (selectedDateTodos.isEmpty()) {
                                OutlinedButton(
                                    onClick = { showAddTodoDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AddTask,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("添加待办")
                                    }
                                }
                            }
                            
                            // 如果没有日记，显示添加日记按钮
                            if (selectedDateDiaries.isEmpty()) {
                                OutlinedButton(
                                    onClick = { showAddDiaryDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.tertiary
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("写日记")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 添加待办对话框
    if (showAddTodoDialog) {
        AddTodoDialog(
            onDismiss = { showAddTodoDialog = false },
            onAddTodo = { title, description, priority, dueDate ->
                handleAddTodo(title, description, priority, dueDate)
                showAddTodoDialog = false
            }
        )
    }
    
    // 添加日记对话框
    if (showAddDiaryDialog) {
        AddEditDiaryDialog(
            diary = null,
            onDismiss = { showAddDiaryDialog = false },
            onSave = { date, content, mood ->
                handleAddDiary(selectedDate, content, mood) // 确保使用选中的日期
                showAddDiaryDialog = false
            }
        )
    }
}

@Composable
fun CalendarTopBar(
    currentYearMonth: YearMonth,
    selectedDate: LocalDate,
    isWeekView: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 根据视图模式设置标题文本
    val titleText = if (isWeekView) {
        // 根据当前所选日期计算周的开始和结束
        val weekStart = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
        val weekEnd = weekStart.plusDays(6)
        // 如果同一个月，只显示月份一次
        if (weekStart.month == weekEnd.month) {
            "${weekStart.year}年${weekStart.monthValue}月"
        } else {
            // 跨月显示
            "${weekStart.format(DateTimeFormatter.ofPattern("yyyy年MM月"))} - ${weekEnd.format(DateTimeFormatter.ofPattern("MM月"))}"
        }
    } else {
        "${currentYearMonth.year}年${currentYearMonth.monthValue}月"
    }
    
    Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 日期显示
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 按钮组 - 使用背景色
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 快速操作按钮 - 使用背景色
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.background),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.KeyboardArrowLeft,
                            contentDescription = "上一个",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    Divider(
                        modifier = Modifier
                            .height(16.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                    
                    IconButton(
                        onClick = onTodayClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Today,
                            contentDescription = "今天",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    Divider(
                        modifier = Modifier
                            .height(16.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                    
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.KeyboardArrowRight,
                            contentDescription = "下一个",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    count: Int,
    initialExpanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit,
    onAddClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    
    // 通知父组件扩展状态变化
    LaunchedEffect(expanded) {
        onExpandedChange(expanded)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .padding(12.dp)
    ) {
        // 标题行，可点击切换展开/收起
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 添加按钮
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "添加",
                    tint = iconTint
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "${count}项",
                style = MaterialTheme.typography.bodySmall,
                color = iconTint
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 内容区域，动画展开/收起
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                content()
            }
        }
    }
}

// 扩展函数：检查日期是否在同一个月内
fun LocalDate.isInSameMonthAs(yearMonth: YearMonth): Boolean {
    return this.year == yearMonth.year && this.month == yearMonth.month
}

@Composable
fun WeekCalendarView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    dateCellStates: Map<LocalDate, DateCellState>,
    modifier: Modifier = Modifier
) {
    // 计算当前周的起始日和结束日
    val monday = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // 星期标题行
        WeekdayHeader()
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 增加周视图日期行的视觉效果
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            // 显示一周的日期
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0..6) {
                    val date = monday.plusDays(i.toLong())
                    DateCell(
                        date = date,
                        isSelected = date.equals(selectedDate),
                        isToday = date.equals(LocalDate.now()),
                        onClick = { onDateSelected(date) },
                        dateCellState = dateCellStates[date]
                    )
                }
            }
        }
    }
}

@Composable
fun MonthCalendarView(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    dateCellStates: Map<LocalDate, DateCellState>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // 星期标题行
        WeekdayHeader()
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 日期网格
        val firstDayOfMonth = yearMonth.atDay(1)
        val dayOfWeek = firstDayOfMonth.dayOfWeek.value
        
        // 计算需要显示的天数
        val daysInMonth = yearMonth.lengthOfMonth()
        
        // 计算需要多少行来显示整个月
        // 第一行可能不完整，所以先计算第一天是周几
        // 然后加上月份总天数，再除以7向上取整
        val rows = ((dayOfWeek - 1 + daysInMonth) + 6) / 7
        
        // 添加卡片包裹日历网格，提升视觉效果
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // 日历网格
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 1..7) {
                            val day = row * 7 + col - (dayOfWeek - 1)
                            
                            if (day in 1..daysInMonth) {
                                // 计算实际日期
                                val date = yearMonth.atDay(day)
                                // 显示日期单元格
                                DateCell(
                                    date = date,
                                    isSelected = date.equals(selectedDate),
                                    isToday = date.equals(LocalDate.now()),
                                    onClick = { onDateSelected(date) },
                                    dateCellState = dateCellStates[date]
                                )
                            } else {
                                // 显示空白占位符
                                Spacer(modifier = Modifier.size(44.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun WeekdayHeader(modifier: Modifier = Modifier) {
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    val today = LocalDate.now().dayOfWeek.value
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekdays.forEachIndexed { index, day ->
            val isToday = index + 1 == today
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .padding(4.dp)
                    .background(
                        if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) 
                        MaterialTheme.colorScheme.primary 
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    dateCellState: DateCellState? = null,
    modifier: Modifier = Modifier
) {
    val hasTodo = dateCellState?.hasTodo ?: false
    val hasCompletedTodo = dateCellState?.hasCompletedTodo ?: false
    val hasDiary = dateCellState?.hasDiary ?: false
    
    // 添加动画效果
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.background  // 改为背景色
            else -> MaterialTheme.colorScheme.surface
        },
        label = "backgroundColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday -> MaterialTheme.colorScheme.primary
            hasCompletedTodo -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "textColor"
    )
    
    Box(
        modifier = modifier
            .size(44.dp)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // 背景
        Surface(
            modifier = Modifier
                .size(36.dp)
                // 去除波纹效果，改用无波纹的点击
                .clickable(
                    indication = null,  // 去除波纹
                    interactionSource = remember { MutableInteractionSource() }, 
                    onClick = onClick
                ),
            color = backgroundColor,
            shape = CircleShape,
            border = if (isToday && !isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
            shadowElevation = if (isSelected || isToday) 4.dp else 0.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 日期文本
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = textColor
                )
                
                // 如果有完成的待办，添加勾选图标
                if (hasCompletedTodo) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.TopEnd)
                            .padding(1.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // 标记容器 - 修改为竖直布局，显示在日期的底部
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(top = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 如果有待办，显示待办标记
            if (hasTodo) {
                Box(
                    modifier = Modifier
                        .width(24.dp)  // 进一步增加宽度
                        .height(2.5.dp)
                        .background(
                            Color.Red.copy(alpha = 0.85f),  // 改为红色
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
            
            // 如果有日记，显示日记标记
            if (hasDiary) {
                Box(
                    modifier = Modifier
                        .width(24.dp)  // 进一步增加宽度
                        .height(2.5.dp)
                        .background(
                            Color.Yellow.copy(alpha = 0.85f),  // 改为黄色
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun TodoItem(
    todo: Todo,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.isCompleted) 
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 完成状态图标
                Icon(
                    imageVector = if (todo.isCompleted) 
                        Icons.Filled.CheckCircle 
                    else 
                        Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (todo.isCompleted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 内容
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (todo.isCompleted) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    
                    // 优先级指示器 - 使用星星
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        // 优先级星星
                        Row {
                            repeat(todo.priority + 1) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = when (todo.priority) {
                                        0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        2 -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        // 删除优先级文字标签
                        
                        // 如果有截止日期，显示
                        if (todo.dueDate != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Icon(
                                imageVector = Icons.Outlined.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(2.dp))
                            
                            Text(
                                text = todo.dueDate.format(DateTimeFormatter.ofPattern("M月d日")),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                // 展开/折叠图标
                if (todo.description.isNotBlank()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 描述展开区域
            AnimatedVisibility(
                visible = expanded && todo.description.isNotBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DiaryItem(
    diary: Diary,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 心情图标和文本
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val moodIcons = listOf(
                    Icons.Outlined.Face,               // 一般
                    Icons.Outlined.Mood,               // 开心
                    Icons.Outlined.SentimentDissatisfied, // 难过
                    Icons.Outlined.Celebration,        // 兴奋
                    Icons.Outlined.NightShelter         // 疲惫
                )
                
                val moodNames = listOf("一般", "开心", "难过", "兴奋", "疲惫")
                
                val moodColors = listOf(
                    MaterialTheme.colorScheme.surfaceVariant,  // 一般
                    MaterialTheme.colorScheme.tertiary,        // 开心
                    MaterialTheme.colorScheme.error,           // 难过
                    MaterialTheme.colorScheme.primary,         // 兴奋
                    MaterialTheme.colorScheme.outline          // 疲惫
                )
                
                // 带有背景的心情指示器
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(moodColors[diary.mood].copy(alpha = 0.15f))
                        .padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = moodIcons[diary.mood],
                            contentDescription = null,
                            tint = moodColors[diary.mood],
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(
                            text = moodNames[diary.mood],
                            style = MaterialTheme.typography.labelMedium,
                            color = moodColors[diary.mood]
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 日记时间
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = diary.createdAt.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                
                // 展开/折叠图标
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 日记内容预览
            val firstLine = diary.content.lines().firstOrNull() ?: ""
            Text(
                text = firstLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
            )
            
            // 展开的内容
            AnimatedVisibility(
                visible = expanded && diary.content.lines().size > 1,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = diary.content.lines().drop(1).joinToString("\n"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MonthCard(
    year: Int,
    month: Int,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthName = when (month) {
        1 -> "一月"
        2 -> "二月"
        3 -> "三月"
        4 -> "四月"
        5 -> "五月"
        6 -> "六月"
        7 -> "七月"
        8 -> "八月"
        9 -> "九月"
        10 -> "十月"
        11 -> "十一月"
        12 -> "十二月"
        else -> ""
    }
    
    // 添加动画效果
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isCurrentMonth -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "cardBackgroundColor"
    )
    
    Card(
        modifier = modifier
            .size(
                width = 104.dp,
                height = 80.dp
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected || isCurrentMonth) 4.dp else 1.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 月份数字
                Text(
                    text = month.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isCurrentMonth -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (isCurrentMonth || isSelected) FontWeight.Bold else FontWeight.Normal
                )
                
                // 月份名称
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        isCurrentMonth -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            
            // 如果是当前月份，添加一个标记
            if (isCurrentMonth) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            MaterialTheme.colorScheme.secondary,
                            CircleShape
                        )
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }
    }
}

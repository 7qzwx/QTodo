package qzwx.app.qtodo.page.todopage

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import qzwx.app.qtodo.data.AppDatabase
import qzwx.app.qtodo.data.Todo
import qzwx.app.qtodo.repository.TodoRepository
import qzwx.app.qtodo.utils.SnackbarManager
import qzwx.app.viewmodel.TodoViewModel
import qzwx.app.viewmodel.TodoViewModelFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailPage(
    todoId: Long,
    onNavigateBack: () -> Unit
) {
    // 获取数据库和ViewModel
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = TodoRepository(database.todoDao())
    val viewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(repository)
    )
    
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 当前Todo状态
    var todo by remember { mutableStateOf<Todo?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    
    // 编辑状态
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(0) }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var isCompleted by remember { mutableStateOf(false) }
    
    // 处理完成状态变化
    val handleCompletedChange = { newValue: Boolean ->
        isCompleted = newValue
        // 如果不是编辑模式，立即更新数据库
        if (!isEditing && todo != null) {
            val updatedTodo = todo!!.copy(
                isCompleted = newValue,
                updatedAt = LocalDateTime.now()
            )
            viewModel.updateTodo(updatedTodo)
            todo = updatedTodo
            
            // 显示提示信息
            coroutineScope.launch {
                SnackbarManager.showSnackbar(
                    snackbarHostState,
                    if (newValue) "已标记为完成" else "已标记为未完成"
                )
            }
        }
    }
    
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // 加载Todo数据
    LaunchedEffect(todoId) {
        viewModel.getTodoById(todoId).firstOrNull()?.let {
            todo = it
            // 初始化编辑状态
            title = it.title
            description = it.description
            priority = it.priority
            dueDate = it.dueDate
            isCompleted = it.isCompleted
        }
    }
    
    // 删除确认对话框
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个待办事项吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        todo?.let {
                            viewModel.deleteTodo(it)
                            showDeleteConfirmation = false
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 日期选择对话框
    if (showDatePicker) {
        // 实现日期时间选择器
        var selectedDate by remember { mutableStateOf(LocalDate.now()) }
        var selectedTime by remember { mutableStateOf(LocalTime.of(12, 0)) }
        var showTimePicker by remember { mutableStateOf(false) }
        
        if (!showTimePicker) {
            // 日期选择对话框
            AlertDialog(containerColor = MaterialTheme.colorScheme.surface,
                onDismissRequest = { showDatePicker = false },
                title = { Text("选择截止日期") },
                text = { 
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // 简单的日期选择器
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 当前选择的日期显示
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            // 年月选择器
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 上一月按钮
                                IconButton(onClick = { 
                                    selectedDate = selectedDate.minusMonths(1) 
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowLeft,
                                        contentDescription = "上个月"
                                    )
                                }
                                
                                // 年月显示
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                // 下一月按钮
                                IconButton(onClick = { 
                                    selectedDate = selectedDate.plusMonths(1) 
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "下个月"
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 星期标题
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                            
                            // 日历网格
                            val firstDayOfMonth = selectedDate.withDayOfMonth(1)
                            val dayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 调整周日为0
                            val daysInMonth = selectedDate.month.length(selectedDate.isLeapYear)
                            val today = LocalDate.now()
                            
                            // 计算显示的行数
                            val numRows = ((daysInMonth + dayOfWeek - 1) / 7) + 1
                            
                            Column(modifier = Modifier.fillMaxWidth()) {
                                for (row in 0 until numRows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        for (col in 0..6) {
                                            val day = row * 7 + col - dayOfWeek + 1
                                            if (day in 1..daysInMonth) {
                                                val date = selectedDate.withDayOfMonth(day)
                                                val isSelected = date.equals(selectedDate)
                                                val isToday = date.equals(today)
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                isSelected -> MaterialTheme.colorScheme.primary
                                                                isToday    -> MaterialTheme.colorScheme.secondary
                                                                else       -> MaterialTheme.colorScheme.surface
                                                            }
                                                        )
                                                        .clickable {
                                                            selectedDate = date
                                                        }
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = day.toString(),
                                                        color = when {
                                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                                            isToday    -> MaterialTheme.colorScheme.primary
                                                            else -> MaterialTheme.colorScheme.onBackground
                                                        }
                                                    )
                                                }
                                            } else {
                                                // 空白占位
                                                Box(modifier = Modifier.size(36.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTimePicker = true }) {
                        Text("下一步")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("取消")
                    }
                }
            )
        } else {
            // 时间选择对话框
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("选择时间") },
                text = { 
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 当前选择的日期显示
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // 时间选择器
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 小时选择
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                IconButton(onClick = { 
                                    val newHour = (selectedTime.hour + 1) % 24
                                    selectedTime = selectedTime.withHour(newHour)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "增加小时"
                                    )
                                }
                                
                                Text(
                                    text = String.format("%02d", selectedTime.hour),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                IconButton(onClick = { 
                                    val newHour = if (selectedTime.hour == 0) 23 else selectedTime.hour - 1
                                    selectedTime = selectedTime.withHour(newHour)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "减少小时"
                                    )
                                }
                            }
                            
                            Text(
                                text = ":",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // 分钟选择
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                IconButton(onClick = { 
                                    val newMinute = (selectedTime.minute + 5) % 60
                                    selectedTime = selectedTime.withMinute(newMinute)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "增加分钟"
                                    )
                                }
                                
                                Text(
                                    text = String.format("%02d", selectedTime.minute),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                IconButton(onClick = { 
                                    val newMinute = if (selectedTime.minute < 5) 55 else selectedTime.minute - 5
                                    selectedTime = selectedTime.withMinute(newMinute)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "减少分钟"
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // 设置最终日期时间
                            dueDate = LocalDateTime.of(selectedDate, selectedTime)
                            showDatePicker = false
                            showTimePicker = false
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showTimePicker = false 
                        }
                    ) {
                        Text("返回")
                    }
                }
            )
        }
    }
    
    Scaffold(
        topBar = {
            // 优化的顶部栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
            ) {
                // 返回按钮
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                // 标题
                Text(
                    text = if (isEditing) "编辑待办" else "待办详情",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // 右侧操作按钮
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isEditing) {
                        // 取消按钮
                        IconButton(
                            onClick = {
                                isEditing = false
                                title = todo?.title ?: ""
                                description = todo?.description ?: ""
                                priority = todo?.priority ?: 0
                                dueDate = todo?.dueDate
                                isCompleted = todo?.isCompleted ?: false
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "取消",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        // 保存按钮
                        IconButton(
                            onClick = {
                                todo?.let {
                                    val updatedTodo = it.copy(
                                        title = title,
                                        description = description,
                                        priority = priority,
                                        dueDate = dueDate,
                                        isCompleted = isCompleted,
                                        updatedAt = LocalDateTime.now()
                                    )
                                    viewModel.updateTodo(updatedTodo)
                                    isEditing = false
                                    todo = updatedTodo
                                    coroutineScope.launch {
                                        SnackbarManager.showSnackbar(
                                            snackbarHostState,
                                            "更新成功"
                                        )
                                    }
                                }
                            },
                            enabled = title.isNotBlank(),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "保存",
                                tint = if (title.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        // 编辑按钮
                        IconButton(
                            onClick = { isEditing = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "编辑",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        // 删除按钮
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        todo?.let {
            TodoDetailContent(
                todo = it,
                isEditing = isEditing,
                title = title,
                onTitleChange = { title = it },
                description = description,
                onDescriptionChange = { description = it },
                priority = priority,
                onPriorityChange = { priority = it },
                dueDate = dueDate,
                onDueDateClick = { showDatePicker = true },
                isCompleted = isCompleted,
                onIsCompletedChange = handleCompletedChange,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailContent(
    todo: Todo,
    isEditing: Boolean,
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    priority: Int,
    onPriorityChange: (Int) -> Unit,
    dueDate: LocalDateTime?,
    onDueDateClick: () -> Unit,
    isCompleted: Boolean,
    onIsCompletedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityLabels = listOf("低", "中", "高")
    val priorityColors = listOf(
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error
    )
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isEditing) {
            // 编辑模式
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "任务详情",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text("标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        label = { Text("描述 (可选)") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Start
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 属性卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "任务属性",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 优先级选择
                    Text(
                        text = "优先级",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 优先级选择卡片
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        priorityLabels.forEachIndexed { index, label ->
                            val isSelected = priority == index
                            val backgroundColor = when {
                                isSelected -> when(index) {
                                    0 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                    1 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                }
                                else -> Color.Transparent
                            }
                            
                            val textColor = when {
                                isSelected -> when(index) {
                                    0 -> MaterialTheme.colorScheme.tertiary
                                    1 -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.error
                                }
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(backgroundColor)
                                    .clickable { onPriorityChange(index) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                // 优先级图标
                                Row {
                                    repeat(index + 1) {
                                        Icon(
                                            imageVector = Icons.Outlined.Star,
                                            contentDescription = null,
                                            tint = textColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // 优先级文本
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )
                            }
                            
                            if (index < priorityLabels.size - 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }
                    
                    // 显示当前选择的优先级状态
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val priorityColor = when(priority) {
                        0 -> MaterialTheme.colorScheme.tertiary
                        1 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = priorityColor.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PriorityHigh,
                                contentDescription = null,
                                tint = priorityColor,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = "任务优先级",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    repeat(priority + 1) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = priorityColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = "${priorityLabels[priority]}优先级",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = priorityColor
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 截止日期选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "截止日期: ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = dueDate?.let { formatDateTime(it) } ?: "未设置",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        IconButton(onClick = onDueDateClick) {
                            Icon(
                                imageVector = Icons.Outlined.DateRange,
                                contentDescription = "选择日期"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 完成状态
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 优先级指示器
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            repeat(todo.priority + 1) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = priorityLabels[todo.priority],
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // 完成状态开关
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    MaterialTheme.colorScheme.surface
                                )
                                .padding(
                                    start = 12.dp,
                                    end = 8.dp,
                                    top = 4.dp,
                                    bottom = 4.dp
                                )
                        ) {

                            Text(
                                text = if (isCompleted) "已完成" else "未完成",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCompleted) 
                                   Color.Green.copy(0.5f)
                                else 
                                    Color.Red.copy(0.5f)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Switch(
                                checked = isCompleted,
                                onCheckedChange = { newValue ->
                                    onIsCompletedChange(newValue)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Green.copy(0.5f),
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor =  Color.Red.copy(0.5f)
                                ),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            
        } else {
            // 查看模式 - 主信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 创建时间和优先级
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 优先级指示器
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            repeat(todo.priority + 1) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = priorityLabels[todo.priority],
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // 完成状态开关
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    MaterialTheme.colorScheme.surface
                                )
                                .padding(
                                    start = 12.dp,
                                    end = 8.dp,
                                    top = 4.dp,
                                    bottom = 4.dp
                                )
                        ) {
                            Text(
                                text = if (isCompleted) "已完成" else "未完成",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCompleted) 
                                    Color.Green.copy(0.5f)
                                else 
                                    Color.Red.copy(0.5f)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Switch(
                                checked = isCompleted,
                                onCheckedChange = { newValue ->
                                    onIsCompletedChange(newValue)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Green.copy(0.5f),
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = Color.Red.copy(0.5f)
                                )
                            )
                        }
                    }
                    
                    // 标题
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            textDecoration = TextDecoration.None
                        ),
                        color = if (todo.isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // 完成状态标签
                    if (todo.isCompleted) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "已完成",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 截止日期
                    todo.dueDate?.let {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "截止日期: ${formatDateTime(it)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    
                    // 创建日期
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "创建于 ${formatDateTime(todo.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 描述卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 描述
                    Text(
                        text = "描述",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (todo.description.isNotBlank()) {
                        Text(
                            text = todo.description,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                lineHeight = 28.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        )
                    } else {
                        Text(
                            text = "没有描述",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 最后更新时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Update,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "最后更新: ${formatDateTime(todo.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// 格式化日期时间
private fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return dateTime.format(formatter)
} 
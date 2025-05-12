package qzwx.app.qtodo.page.todopage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import qzwx.app.qtodo.data.AppDatabase
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
fun AddTodoPage(
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 获取数据库和ViewModel
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = TodoRepository(database.todoDao())
    val viewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(repository)
    )
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // 表单状态
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(0) }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // 日期选择器状态
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(12, 0)) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    val priorityLabels = listOf("低", "中", "高")
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("添加新待办") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题输入
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 描述输入
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述 (可选)") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            
            // 优先级选择
            Text(
                text = "优先级",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                priorityLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = priority == index,
                        onClick = { priority = index },
                        label = { Text(label) },
                        leadingIcon = {
                            Row {
                                repeat(index + 1) {
                                    Icon(
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
            
            // 截止日期选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "截止日期",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { showDatePicker = true }
                        .padding(8.dp)
                ) {
                    Text(
                        text = dueDate?.let { formatDateTime(it) } ?: "未设置",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "选择日期"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 提交按钮
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addTodo(title, description, priority, dueDate)
                        coroutineScope.launch {
                            SnackbarManager.showSnackbar(
                                snackbarHostState,
                                "添加成功：$title"
                            )
                            onSuccess()
                        }
                    } else {
                        coroutineScope.launch {
                            SnackbarManager.showSnackbar(
                                snackbarHostState,
                                "请输入标题"
                            )
                        }
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("添加")
            }
        }
    }
    
    // 日期选择对话框
    if (showDatePicker && !showTimePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                showTimePicker = true
            },
            onDismiss = { showDatePicker = false }
        )
    }
    
    // 时间选择对话框
    if (showTimePicker) {
        TimePickerDialog(
            selectedDate = selectedDate,
            selectedTime = selectedTime,
            onTimeSelected = { time ->
                selectedTime = time
                dueDate = LocalDateTime.of(selectedDate, selectedTime)
                showTimePicker = false
                showDatePicker = false
            },
            onBack = {
                showTimePicker = false
            },
            onDismiss = {
                showTimePicker = false
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun DatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    // 使用现有的日期选择器对话框逻辑
    var currentDate by remember { mutableStateOf(selectedDate) }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
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
                        text = currentDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
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
                            currentDate = currentDate.minusMonths(1)
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "上个月"
                            )
                        }

                        // 年月显示
                        Text(
                            text = currentDate.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
                            style = MaterialTheme.typography.titleMedium
                        )

                        // 下一月按钮
                        IconButton(onClick = {
                            currentDate = currentDate.plusMonths(1)
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
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
                    val firstDayOfMonth = currentDate.withDayOfMonth(1)
                    val dayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 调整周日为0
                    val daysInMonth = currentDate.month.length(currentDate.isLeapYear)
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
                                        val date = currentDate.withDayOfMonth(day)
                                        val isSelected = date.equals(currentDate)
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
                                                    currentDate = date
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
            Button(onClick = { onDateSelected(currentDate) }) {
                Text("下一步")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun TimePickerDialog(
    selectedDate: LocalDate,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    // 使用现有的时间选择器对话框逻辑
    // 这里你需要从TodoPage_UI.kt中提取相关代码
    // todo: 从TodoPage_UI.kt提取时间选择器实现
    var currentTime by remember { mutableStateOf(selectedTime) }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
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
                            val newHour = (currentTime.hour + 1) % 24
                            currentTime = currentTime.withHour(newHour)
                        }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "增加小时"
                            )
                        }

                        Text(
                            text = String.format("%02d", currentTime.hour),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = {
                            val newHour = if (currentTime.hour == 0) 23 else currentTime.hour - 1
                            currentTime = currentTime.withHour(newHour)
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
                            val newMinute = (currentTime.minute + 5) % 60
                            currentTime = currentTime.withMinute(newMinute)
                        }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "增加分钟"
                            )
                        }

                        Text(
                            text = String.format("%02d", currentTime.minute),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = {
                            val newMinute = if (currentTime.minute < 5) 55 else currentTime.minute - 5
                            currentTime = currentTime.withMinute(newMinute)
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
            Button(onClick = { onTimeSelected(currentTime) }) {
                Text("确定")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onBack) {
                Text("返回")
            }
        }
    )
}

// 格式化日期时间
private fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return dateTime.format(formatter)
} 
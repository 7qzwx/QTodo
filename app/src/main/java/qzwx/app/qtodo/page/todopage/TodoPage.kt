package qzwx.app.qtodo.page.todopage

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import qzwx.app.qtodo.data.AppDatabase
import qzwx.app.qtodo.data.Todo
import qzwx.app.qtodo.repository.TodoRepository
import qzwx.app.viewmodel.TodoFilterType
import qzwx.app.viewmodel.TodoViewModel
import qzwx.app.viewmodel.TodoViewModelFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoPage(
    modifier: Modifier = Modifier,
    onTodoClick: (Long) -> Unit = {}
) {
    // 获取数据库和ViewModel
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = TodoRepository(database.todoDao())
    val viewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(repository)
    )

    val uiState by viewModel.uiState.collectAsState()
    val todos by when (uiState.filterType) {
        TodoFilterType.ALL -> viewModel.allTodos.collectAsState(initial = emptyList())
        TodoFilterType.ACTIVE -> viewModel.activeTodos.collectAsState(initial = emptyList())
        TodoFilterType.COMPLETED -> viewModel.completedTodos.collectAsState(initial = emptyList())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val showAddTodoDialog = remember { mutableStateOf(false) }

    // 用于计算每天的todo数量
    val todosCountByDate = remember(todos) {
        todos.groupBy { LocalDate.from(it.createdAt) }
            .mapValues { it.value.size }
    }

    if (showAddTodoDialog.value) {
        AddTodoDialog(
            onDismiss = { showAddTodoDialog.value = false },
            onAddTodo = { title, description, priority, dueDate ->
                viewModel.addTodo(title, description, priority, dueDate)
                showAddTodoDialog.value = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("添加成功：$title")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // 简化版的TopBar
            SmallTopBar(
                title = when (uiState.filterType) {
                    TodoFilterType.ALL -> "所有待办"
                    TodoFilterType.ACTIVE -> "进行中"
                    TodoFilterType.COMPLETED -> "已完成"
                },
                onFilterChanged = viewModel::updateFilterType,
                currentFilter = uiState.filterType
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTodoDialog.value = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加待办"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (todos.isEmpty()) {
                EmptyTodoList(filterType = uiState.filterType)
            } else {
                val groupedTodos = todos.groupBy { 
                    LocalDate.from(it.createdAt)
                }.toSortedMap(compareByDescending { it })
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    groupedTodos.forEach { (date, todosForDate) ->
                        stickyHeader(key = "date-$date") {
                            Surface(
                                color = MaterialTheme.colorScheme.background
                            ) {
                                DateHeader(date = date, todoCount = todosForDate.size)
                            }
                        }
                        
                        // 按照创建时间排序
                        val sortedTodos = todosForDate.sortedByDescending { it.createdAt }
                        
                        items(sortedTodos, key = { it.id }) { todo ->
                            TimelineItem(
                                todo = todo,
                                onToggleCompleted = { 
                                    viewModel.toggleTodoCompleted(todo)
                                },
                                onDelete = {
                                    viewModel.deleteTodo(todo)
                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "已删除：${todo.title}",
                                            actionLabel = "撤销",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.addTodo(
                                                todo.title,
                                                todo.description,
                                                todo.priority,
                                                todo.dueDate
                                            )
                                        }
                                    }
                                },
                                onTodoClick = { onTodoClick(todo.id) },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallTopBar(
    title: String,
    onFilterChanged: (TodoFilterType) -> Unit,
    currentFilter: TodoFilterType
) {
    var showFilterMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Box {
            IconButton(
                onClick = { showFilterMenu = true },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "筛选",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            DropdownMenu(modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("全部") },
                    onClick = {
                        onFilterChanged(TodoFilterType.ALL)
                        showFilterMenu = false
                    },
                    leadingIcon = {
                        RadioButton(
                            selected = currentFilter == TodoFilterType.ALL,
                            onClick = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("进行中") },
                    onClick = {
                        onFilterChanged(TodoFilterType.ACTIVE)
                        showFilterMenu = false
                    },
                    leadingIcon = {
                        RadioButton(
                            selected = currentFilter == TodoFilterType.ACTIVE,
                            onClick = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("已完成") },
                    onClick = {
                        onFilterChanged(TodoFilterType.COMPLETED)
                        showFilterMenu = false
                    },
                    leadingIcon = {
                        RadioButton(
                            selected = currentFilter == TodoFilterType.COMPLETED,
                            onClick = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun DateHeader(date: LocalDate, todoCount: Int) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    
    val headerText = when {
        date.isEqual(today) -> "今天"
        date.isEqual(yesterday) -> "昨天"
        date.year == today.year -> "${date.monthValue}月${date.dayOfMonth}日 ${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}"
        else -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日 ${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA)}"
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headerText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // 待办计数
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$todoCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    }
}

// 创建FlipCard组件实现3D翻转效果
@Composable
fun FlipCard(
    cardFace: CardFace,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    back: @Composable () -> Unit = {},
    front: @Composable () -> Unit = {},
) {
    val rotation = animateFloatAsState(
        targetValue = if (cardFace == CardFace.Front) 0f else 180f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "cardRotation"
    )
    
    Box(
        modifier = modifier,
    ) {
        // 前面
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation.value
                    cameraDistance = 12f * density
                    alpha = if (rotation.value <= 90f) 1f else 0f
                }
        ) {
            front()
        }
        
        // 背面 - 再次旋转180度抵消整体翻转，使内容保持正向
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation.value - 180f // 抵消整体翻转
                    cameraDistance = 12f * density
                    alpha = if (rotation.value > 90f) 1f else 0f
                }
        ) {
            back()
        }
    }
}

enum class CardFace {
    Front, Back
}

@Composable
fun TimelineItem(
    todo: Todo,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
    onTodoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cardHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    
    // 使用自己的状态控制卡片面
    var manualCardFace by remember { mutableStateOf(if (todo.isCompleted) CardFace.Back else CardFace.Front) }
    
    // 确保在todo.isCompleted变化时重置卡片面
    LaunchedEffect(todo.isCompleted) {
        manualCardFace = if (todo.isCompleted) CardFace.Back else CardFace.Front
    }
    
    // 正面完成按钮处理函数 - 仅翻转到背面，不改变完成状态
    val onFrontCompletedClick = {
        // 只翻转到背面，不改变完成状态
        manualCardFace = CardFace.Back
    }
    
    // 背面按钮处理函数 - 翻转到正面同时标记为未完成
    val onBackCompletedClick = {
        // 先标记为未完成
        onToggleCompleted()
        // 然后翻转到正面
        manualCardFace = CardFace.Front
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间轴线和时间点 - 放在卡片的中间左侧
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(with(density) { cardHeight.toDp() })
                .padding(end = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // 竖线贯穿整个高度
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
            
            // 时间和时间点
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                // 时间点
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (todo.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.primary
                        )
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 时间
                Text(
                    text = formatTime(todo.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Todo卡片 - 使用FlipCard
        SwipeableActionsBox(
            startActions = listOf(
                SwipeAction(
                    icon = rememberVectorPainter(Icons.Default.Check),
                    background = MaterialTheme.colorScheme.primaryContainer,
                    onSwipe = { onToggleCompleted() }
                ),
                SwipeAction(
                    icon = rememberVectorPainter(Icons.Outlined.Edit),
                    background = MaterialTheme.colorScheme.secondaryContainer,
                    onSwipe = { onTodoClick() }
                )
            ),
            endActions = listOf(
                SwipeAction(
                    icon = rememberVectorPainter(Icons.Outlined.Delete),
                    background = MaterialTheme.colorScheme.errorContainer,
                    onSwipe = { onDelete() }
                )
            ),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            FlipCard(
                cardFace = manualCardFace,
                onClick = { onTodoClick() },
                onToggleCompleted = onToggleCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 70.dp)
                    .onGloballyPositioned { coordinates ->
                        cardHeight = coordinates.size.height
                    },
                front = {
                    // 正面 - 未完成状态
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onTodoClick),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(1.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = if (todo.isCompleted)
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        else
                            null
                    ) {
                        // 卡片内容 - 正面使用自定义点击处理函数实现点击翻转
                        TodoCardContent(
                            todo = todo,
                            onToggleCompleted = onFrontCompletedClick, // 使用自定义处理函数
                            isCompleted = false,
                            alphaMultiplier = 1f
                        )
                    }
                },
                back = {
                    // 背面 - 已完成状态
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = onTodoClick),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(1.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        // 使用Box包装，以便添加水印图标
                        Box(modifier = Modifier.fillMaxSize()) {
                            // 大型完成标记水印
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), // 降低透明度
                                modifier = Modifier
                                    .size(120.dp)
                                    .align(Alignment.Center)
                                    .zIndex(1f)
                            )
                            
                            // 卡片内容 - 背面使用普通完成处理函数
                            TodoCardContent(
                                todo = todo,
                                onToggleCompleted = onBackCompletedClick,
                                isCompleted = true,
                                alphaMultiplier = 0.3f, // 只影响文本，不影响图标
                                modifier = Modifier.zIndex(2f)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun TodoCardContent(
    todo: Todo,
    onToggleCompleted: () -> Unit,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    alphaMultiplier: Float = 1f
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // 顶部行：优先级星星和完成状态
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 优先级星星指示器
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(todo.priority + 1) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        // 不管正面还是背面，星星图标颜色保持不变
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // 复选框图标
            IconButton(
                onClick = { onToggleCompleted() },
                modifier = Modifier.size(32.dp)
            ) {
                // 不同面显示不同图标
                Icon(
                    imageVector = if (todo.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (todo.isCompleted) "标记为未完成" else "标记为已完成",
                    // 完成图标保持完整颜色，不降低透明度
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // 内容部分，减小与顶部行的间距
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp), // 减小了顶部间距
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题 - 无论正反面都不显示删除线
            Text(
                text = todo.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    // 不使用删除线
                    textDecoration = TextDecoration.None
                ),
                color = if (isCompleted)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f * alphaMultiplier)
                else 
                    MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            
            // 描述（如果有）
            if (todo.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            ParagraphStyle(
                                textIndent = TextIndent(firstLine = 32.sp)
                            )
                        ) {
                            append(todo.description)
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp
                    ),
                    color = if (isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f * alphaMultiplier)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 截止日期（如果有）
            todo.dueDate?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = if (isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f * alphaMultiplier)
                        else
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDateTime(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCompleted)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f * alphaMultiplier)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    onDismiss: () -> Unit,
    onAddTodo: (title: String, description: String, priority: Int, dueDate: LocalDateTime?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(0) }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val priorityLabels = listOf("低", "中", "高")


    AlertDialog(containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismiss,
        title = { Text("添加新待办") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述 (可选)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 优先级选择
                Text(
                    text = "优先级",
                    style = MaterialTheme.typography.bodyMedium,
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 截止日期选择
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "截止日期",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAddTodo(title, description, priority, dueDate)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
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
}

@Composable
fun EmptyTodoList(filterType: TodoFilterType) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val message = when (filterType) {
            TodoFilterType.ALL -> "没有待办事项"
            TodoFilterType.ACTIVE -> "没有进行中的待办"
            TodoFilterType.COMPLETED -> "没有已完成的待办"
        }
        
        val icon = when (filterType) {
            TodoFilterType.ALL -> Icons.Default.Assignment
            TodoFilterType.ACTIVE -> Icons.Default.WorkOutline
            TodoFilterType.COMPLETED -> Icons.Default.CheckCircleOutline
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (filterType != TodoFilterType.COMPLETED) {
                "点击右下角 + 按钮添加新待办"
            } else {
                "完成一些待办事项后会显示在这里"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// 格式化日期时间
private fun formatDateTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return dateTime.format(formatter)
}

// 格式化时间
private fun formatTime(dateTime: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return dateTime.format(formatter)
}
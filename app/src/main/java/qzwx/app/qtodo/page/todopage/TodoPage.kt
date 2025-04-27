package qzwx.app.qtodo.page.todopage

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.Dp
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
import qzwx.app.qtodo.utils.SnackbarManager

// 添加时间轴节点位置枚举
enum class TimelineNodePosition {
    FIRST,
    MIDDLE,
    LAST
}

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

    // 获取所有未完成的待办事项用于表格展示
    val activeTodos by viewModel.activeTodos.collectAsState(initial = emptyList())
    
    // 是否显示待办概览表格
    var showTodoTable by remember { mutableStateOf(true) }

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
                    SnackbarManager.showSnackbar(
                        snackbarHostState,
                        "添加成功：$title"
                    )
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
            Column {
                // 添加未完成待办表格
                TodoOverviewTable(
                    activeTodos = activeTodos,
                    showTable = showTodoTable,
                    onToggleTableVisibility = { showTodoTable = !showTodoTable },
                    onTodoClick = onTodoClick,
                    onViewAllActive = { 
                        // 将过滤器切换到"进行中"类型
                        viewModel.updateFilterType(TodoFilterType.ACTIVE)
                    }
                )
                
                // 时间轴显示待办内容
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
                                // 确定当前Todo在列表中的位置
                                val position = when {
                                    todo == sortedTodos.first() -> TimelineNodePosition.FIRST
                                    todo == sortedTodos.last() -> TimelineNodePosition.LAST
                                    else -> TimelineNodePosition.MIDDLE
                                }
                                
                                TimelineItem(
                                    todo = todo,
                                    onToggleCompleted = { 
                                        viewModel.toggleTodoCompleted(todo)
                                    },
                                    onDelete = {
                                        viewModel.deleteTodo(todo)
                                        coroutineScope.launch {
                                            val result = SnackbarManager.showSnackbar(
                                                snackbarHostState,
                                                "已删除：${todo.title}",
                                                "撤销"
                                            )
                                            if (result) {
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
                                    position = position,
                                )
                            }
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
    position: TimelineNodePosition,
    modifier: Modifier = Modifier,
    nodeColor: Color = MaterialTheme.colorScheme.primary,
    nodeSize: Dp = 10.dp,
    lineWidth: Dp = 2.dp,
    contentStartOffset: Dp = 16.dp,
    spacerBetweenNodes: Dp = 8.dp
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
                .height(with(density) { cardHeight.toDp() + spacerBetweenNodes })
                .padding(end = 8.dp)
                .drawBehind {
                    // 绘制时间轴线
                    val lineColor = nodeColor.copy(alpha = 0.3f)
                    // 计算圆点中心
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    
                    // 绘制时间轴线
                    when (position) {
                        TimelineNodePosition.FIRST -> {
                            // 对于第一个节点，只绘制从中心到底部的线
                            drawLine(
                                color = lineColor,
                                start = Offset(centerX, centerY),
                                end = Offset(centerX, size.height),
                                strokeWidth = lineWidth.toPx()
                            )
                        }
                        TimelineNodePosition.MIDDLE -> {
                            // 对于中间节点，绘制贯穿整个高度的线
                            drawLine(
                                color = lineColor,
                                start = Offset(centerX, 0f),
                                end = Offset(centerX, size.height),
                                strokeWidth = lineWidth.toPx()
                            )
                        }
                        TimelineNodePosition.LAST -> {
                            // 对于最后一个节点，只绘制从顶部到中心的线
                            drawLine(
                                color = lineColor,
                                start = Offset(centerX, 0f),
                                end = Offset(centerX, centerY),
                                strokeWidth = lineWidth.toPx()
                            )
                        }
                    }
                    
                    // 绘制节点圆点
                    drawCircle(
                        color = if (todo.isCompleted) nodeColor.copy(alpha = 0.6f) else nodeColor,
                        radius = nodeSize.toPx() / 2,
                        center = Offset(centerX, centerY)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // 时间
            Text(
                text = formatTime(todo.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = nodeSize + 4.dp)
            )
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
                .padding(bottom = spacerBetweenNodes)
        ) {
            FlipCard(
                cardFace = manualCardFace,
                onClick = { onTodoClick() },
                onToggleCompleted = onToggleCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        // 确保每次布局时都更新高度
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
            
            // 如果没有描述和截止日期，添加一个最小高度空间
            if (todo.description.isBlank() && todo.dueDate == null) {
                Spacer(modifier = Modifier.height(24.dp))
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

@Composable
fun TodoOverviewTable(
    activeTodos: List<Todo>,
    showTable: Boolean,
    onToggleTableVisibility: () -> Unit,
    onTodoClick: (Long) -> Unit,
    onViewAllActive: () -> Unit
) {
    // 可折叠部分
    AnimatedVisibility(
        visible = showTable,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 表格标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "待办概览（${activeTodos.size}）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 展开/折叠按钮
                IconButton(
                    onClick = onToggleTableVisibility,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = "折叠表格",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (activeTodos.isEmpty()) {
                // 没有未完成待办的情况
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "恭喜！暂无未完成的待办",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // 表格头部
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = "优先级",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(60.dp)
                    )
                    Text(
                        text = "内容",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "截止时间",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(120.dp)
                    )
                }
                
                // 表格内容 - 最多显示5项，有更多显示"查看更多"按钮
                val displayTodos = if (activeTodos.size > 5) activeTodos.take(5) else activeTodos
                
                displayTodos.forEach { todo ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTodoClick(todo.id) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 优先级
                        Row(
                            modifier = Modifier.width(60.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(todo.priority + 1) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        // 标题
                        Text(
                            text = todo.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 截止时间
                        todo.dueDate?.let { dueDate ->
                            val today = LocalDate.now()
                            val dueDateLocalDate = LocalDate.from(dueDate)
                            val isOverdue = dueDateLocalDate.isBefore(today)
                            
                            Text(
                                text = formatDateTime(dueDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverdue) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(120.dp)
                            )
                        } ?: Text(
                            text = "无截止日期",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.width(120.dp)
                        )
                    }
                    
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp
                    )
                }
                
                // 如果有更多待办，显示"查看更多"按钮
                if (activeTodos.size > 5) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = onViewAllActive
                        ) {
                            Text(
                                text = "查看全部${activeTodos.size}个待办 >",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 当表格处于折叠状态时，显示一个简洁的小条
    AnimatedVisibility(
        visible = !showTable,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onToggleTableVisibility)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "待办概览（${activeTodos.size}）",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "展开表格",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
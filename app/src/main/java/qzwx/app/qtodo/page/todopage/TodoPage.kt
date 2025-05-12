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
import qzwx.app.qtodo.navigation.NavRoutes
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
    onTodoClick: (Long) -> Unit = {},
    onAddTodoClick: () -> Unit = {}
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

    // 获取所有未完成的待办事项用于表格展示
    val activeTodos by viewModel.activeTodos.collectAsState(initial = emptyList())
    
    // 是否显示待办概览表格
    var showTodoTable by remember { mutableStateOf(true) }

    // 用于计算每天的todo数量
    val todosCountByDate = remember(todos) {
        todos.groupBy { LocalDate.from(it.createdAt) }
            .mapValues { it.value.size }
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
                onClick = { onAddTodoClick() },
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

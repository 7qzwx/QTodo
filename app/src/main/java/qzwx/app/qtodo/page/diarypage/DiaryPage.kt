package qzwx.app.qtodo.page.diarypage

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import qzwx.app.qtodo.data.AppDatabase
import qzwx.app.qtodo.data.Diary
import qzwx.app.qtodo.repository.DiaryRepository
import qzwx.app.viewmodel.DiaryViewModel
import qzwx.app.viewmodel.DiaryViewModelFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiaryPage(
    modifier: Modifier = Modifier,
    onDiaryClick: (Long) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    // 获取数据库和仓库
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = DiaryRepository(database.diaryDao())
    
    // 使用工厂创建ViewModel
    val viewModel: DiaryViewModel = viewModel(
        factory = DiaryViewModelFactory(repository)
    )
    
    val diariesState = viewModel.allDiaries.collectAsState(initial = emptyList())
    val diaries = diariesState.value
    
    // 本地状态
    var selectedMood by remember { mutableStateOf(-1) } // -1表示全部
    var showAddDialog by remember { mutableStateOf(false) }
    
    // 心情颜色定义 - 增强颜色对比
    val moodColors = listOf(
        MaterialTheme.colorScheme.outline,          // 一般 - 使用更中性的颜色
        Color(0xFF4CAF50),                         // 开心 - 鲜亮的绿色
        Color(0xFFE91E63),                         // 难过 - 粉红色
        Color(0xFF2196F3),                         // 兴奋 - 亮蓝色
        Color(0xFF9C27B0)                          // 疲惫 - 紫色
    )
    
    // 心情图标
    val moodIcons = listOf(
        Icons.Outlined.Face,                 // 一般
        Icons.Outlined.Mood,                 // 开心
        Icons.Outlined.SentimentDissatisfied,// 难过
        Icons.Outlined.Celebration,          // 兴奋
        Icons.Outlined.NightShelter           // 疲惫
    )
    
    // 心情标签
    val moodLabels = listOf("一般", "开心", "难过", "兴奋", "疲惫")
    
    // 添加视图模式切换
    var isGridView by remember { mutableStateOf(false) } // false表示列表视图，true表示瀑布流
    
    // 过滤后的日记
    val filteredDiaries = remember(diaries, selectedMood) {
        diaries.filter { diary ->
            (selectedMood == -1 || diary.mood == selectedMood)
        }.sortedByDescending { it.date }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 简化的操作栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 添加页面标题
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "我的日记",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 操作按钮区域
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 切换视图模式按钮
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.ViewModule,
                            contentDescription = if (isGridView) "切换到列表视图" else "切换到瀑布流视图",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    // 当前选中的心情过滤器文本（如果有选择）
                    if (selectedMood >= 0) {
                        val moodLabels = listOf("一般", "开心", "难过", "兴奋", "疲惫")
                        val moodColors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,  // 一般
                            MaterialTheme.colorScheme.tertiary,        // 开心
                            MaterialTheme.colorScheme.error,           // 难过
                            MaterialTheme.colorScheme.primary,         // 兴奋
                            MaterialTheme.colorScheme.outline          // 疲惫
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(moodColors[selectedMood].copy(alpha = 0.1f))
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                        ) {
                            Text(
                                text = moodLabels[selectedMood],
                                style = MaterialTheme.typography.bodySmall,
                                color = moodColors[selectedMood]
                            )
                        }
                    }
                
                    // 导航到搜索页面
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Search, 
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    Box {
                        var showMoodFilter by remember { mutableStateOf(false) }
                        
                        IconButton(
                            onClick = { showMoodFilter = !showMoodFilter },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Outlined.FilterList, 
                                contentDescription = "筛选",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMoodFilter,
                            onDismissRequest = { showMoodFilter = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("全部") },
                                onClick = { selectedMood = -1; showMoodFilter = false },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("一般") },
                                onClick = { selectedMood = 0; showMoodFilter = false },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Face,
                                        contentDescription = null,
                                        tint = moodColors[0]
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("开心") },
                                onClick = { selectedMood = 1; showMoodFilter = false },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Mood,
                                        contentDescription = null,
                                        tint = moodColors[1]
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("难过") },
                                onClick = { selectedMood = 2; showMoodFilter = false },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.SentimentDissatisfied,
                                        contentDescription = null,
                                        tint = moodColors[2]
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("兴奋") },
                                onClick = { selectedMood = 3; showMoodFilter = false },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Celebration,
                                        contentDescription = null,
                                        tint = moodColors[3]
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("疲惫") },
                                onClick = { selectedMood = 4; showMoodFilter = false },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.NightShelter,
                                        contentDescription = null,
                                        tint = moodColors[4]
                                    )
                                }
                            )
                        }
                    }
                }
            }
            
            // 内容区域
            if (filteredDiaries.isEmpty()) {
                EmptyDiaryContent()
            } else {
                DiaryList(
                    diaries = filteredDiaries,
                    onDiaryClick = { diary -> onDiaryClick(diary.id) },
                    isGridView = isGridView,
                    moodColors = moodColors,
                    moodIcons = moodIcons,
                    moodLabels = moodLabels
                )
            }
        }
        
        // 悬浮按钮
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "添加日记",
                modifier = Modifier
                    .size(26.dp)
                    .padding(4.dp)
            )
        }
    }
    
    if (showAddDialog) {
        AddEditDiaryDialog(
            diary = null,
            onDismiss = { showAddDialog = false },
            onSave = { date, content, mood ->
                viewModel.saveDiary(
                    date = date,
                    content = content,
                    mood = mood
                )
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiaryList(
    diaries: List<Diary>,
    onDiaryClick: (Diary) -> Unit,
    isGridView: Boolean = false,
    moodColors: List<Color> = List(5) { MaterialTheme.colorScheme.primary },
    moodIcons: List<ImageVector> = List(5) { Icons.Outlined.Face },
    moodLabels: List<String> = List(5) { "未知" }
) {
    if (!isGridView) {
        // 原始的列表视图
        val groupedDiaries = diaries.groupBy { it.date }
        
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupedDiaries.forEach { (date, diariesForDate) ->
                stickyHeader {
                    DateHeader(date = date)
                }
                items(diariesForDate) { diary ->
                    DiaryItem(
                        diary = diary,
                        onClick = { onDiaryClick(diary) },
                        moodColors = moodColors,
                        moodIcons = moodIcons,
                        moodLabels = moodLabels,
                        maxPreviewLength = null // 使用默认预览长度
                    )
                }
            }
        }
    } else {
        // 瀑布流视图
        val groupedDiaries = diaries.groupBy { it.date }
        
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupedDiaries.forEach { (date, diariesForDate) ->
                stickyHeader {
                    DateHeader(date = date)
                }
                
                item {
                    StaggeredGrid(
                        items = diariesForDate,
                        columns = 2,
                        modifier = Modifier.fillMaxWidth()
                    ) { diary ->
                        // 为瀑布流生成一个随机的预览长度限制（30-120字之间）
                        val previewLength = remember(diary.id) {
                            (30..120).random()
                        }
                        
                        DiaryItem(
                            diary = diary,
                            onClick = { onDiaryClick(diary) },
                            moodColors = moodColors,
                            moodIcons = moodIcons,
                            moodLabels = moodLabels,
                            maxPreviewLength = previewLength
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun <T> StaggeredGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (T) -> Unit
) {
    Row(
        modifier = modifier.padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val chunkedItems = items.chunked(items.size / columns + 1)
        for (i in 0 until minOf(columns, items.size)) {
            if (i < chunkedItems.size) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (item in chunkedItems[i]) {
                        content(item)
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: LocalDate) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    
    val displayText = when (date) {
        today -> "今天"
        yesterday -> "昨天"
        else -> date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 4.dp,
                    end = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 日期图标和文本
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 分隔线
            Divider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun DiaryItem(
    diary: Diary,
    onClick: () -> Unit,
    moodColors: List<Color>,
    moodIcons: List<ImageVector>,
    moodLabels: List<String>,
    maxPreviewLength: Int?
) {
    // 获取内容的第一行作为标题
    val title = diary.content.lines().firstOrNull() ?: ""
    
    // 获取除了第一行以外的内容作为预览
    val previewContent = diary.content.lines().drop(1).joinToString(" ").let {
        if (maxPreviewLength == null || it.length <= maxPreviewLength) it else it.take(maxPreviewLength) + "..."
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = moodColors[diary.mood].copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 顶部区域：标题和心情标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题区域
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 心情标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(moodColors[diary.mood].copy(alpha = 0.1f))
                        .padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = moodIcons[diary.mood],
                            contentDescription = null,
                            tint = moodColors[diary.mood],
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = moodLabels[diary.mood],
                            style = MaterialTheme.typography.labelSmall,
                            color = moodColors[diary.mood]
                        )
                    }
                }
            }
            
            // 显示内容预览
            if (previewContent.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                
                // 内容预览
                Text(
                    text = buildAnnotatedString {
                        pushStyle(
                            ParagraphStyle(
                                textIndent = TextIndent(firstLine = 16.sp)
                            )
                        )
                        append(previewContent)
                        pop()
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 底部信息：日期和字数统计
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 显示日期
                Text(
                    text = diary.date.format(DateTimeFormatter.ofPattern("MM月dd日")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                // 显示字数
                Text(
                    text = "${diary.content.length}字",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun EmptyDiaryContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 添加一个卡片背景
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(4.dp),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 添加波浪形装饰
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "还没有日记",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "记录你的一天，点击右下角的按钮开始写日记",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 添加一个引导按钮
                Button(
                    onClick = { /* 无操作 */ },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始写日记")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDiaryDialog(
    diary: Diary?,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, content: String, mood: Int) -> Unit
) {
    val isNewDiary = diary == null
    val title = if (isNewDiary) "新建日记" else "编辑日记"
    
    var content by remember { mutableStateOf(diary?.content ?: "") }
    var selectedMood by remember { mutableStateOf(diary?.mood ?: 0) }
    var selectedDate by remember { mutableStateOf(diary?.date ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // 添加首行缩进的辅助函数
    fun addIndentation(text: String): String {
        val lines = text.lines()
        if (lines.isEmpty()) return text
        
        val firstLine = lines.first()
        // 如果第一行开头已经有空格，不再添加
        val indentedFirstLine = if (firstLine.startsWith("    ")) firstLine else "    $firstLine"
        
        return if (lines.size > 1) {
            indentedFirstLine + "\n" + lines.drop(1).joinToString("\n")
        } else {
            indentedFirstLine
        }
    }
    
    val moodNames = listOf("一般", "开心", "难过", "兴奋", "疲惫")
    val moodIcons = listOf(
        Icons.Outlined.Face,
        Icons.Outlined.Mood,
        Icons.Outlined.SentimentDissatisfied,
        Icons.Outlined.Celebration,
        Icons.Outlined.NightShelter
    )
    
    // 心情颜色定义
    val moodColors = listOf(
        MaterialTheme.colorScheme.outline,          // 一般 - 使用更中性的颜色
        Color(0xFF4CAF50),                         // 开心 - 鲜亮的绿色
        Color(0xFFE91E63),                         // 难过 - 粉红色
        Color(0xFF2196F3),                         // 兴奋 - 亮蓝色
        Color(0xFF9C27B0)                          // 疲惫 - 紫色
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        text = {
            Column {
                // 日期选择
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("日期:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(
                                selectedDate.format(DateTimeFormatter.ofPattern("MM月dd日")),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("日记内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("今天发生了什么...") },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("今天的心情")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    moodNames.forEachIndexed { index, name ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedMood = index }
                                .background(
                                    if (selectedMood == index) moodColors[index].copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = moodIcons[index],
                                contentDescription = name,
                                tint = if (selectedMood == index)
                                    moodColors[index]
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedMood == index)
                                    moodColors[index]
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 自动添加首行缩进
                    val processedContent = addIndentation(content)
                    onSave(selectedDate, processedContent, selectedMood)
                },
                enabled = content.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
    
    // 日期选择器对话框
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("选择日期") },
            containerColor = MaterialTheme.colorScheme.background,
            text = { 
                Text("当前选择的日期: ${selectedDate.format(DateTimeFormatter.ofPattern("MM月dd日"))}")
                
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    // 日期选择按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { selectedDate = selectedDate.minusDays(1) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("前一天")
                        }
                        
                        Button(
                            onClick = { selectedDate = LocalDate.now() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("今天")
                        }
                        
                        Button(
                            onClick = { 
                                // 限制不能选择未来日期
                                if (selectedDate.isBefore(LocalDate.now())) {
                                    selectedDate = selectedDate.plusDays(1) 
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            enabled = selectedDate.isBefore(LocalDate.now())
                        ) {
                            Text("后一天")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text("确定")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { 
                    // 重置为今天
                    selectedDate = LocalDate.now()
                    showDatePicker = false 
                }) {
                    Text("取消")
                }
            }
        )
    }
}
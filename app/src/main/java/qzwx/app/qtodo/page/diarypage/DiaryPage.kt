package qzwx.app.qtodo.page.diarypage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.collectLatest
import qzwx.app.qtodo.data.Diary
import qzwx.app.qtodo.data.AppDatabase
import qzwx.app.qtodo.repository.DiaryRepository
import qzwx.app.viewmodel.DiaryViewModel
import qzwx.app.viewmodel.DiaryViewModelFactory
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.foundation.BorderStroke

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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 添加页面标题
                Text(
                    text = "日记页面",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 导航到搜索页面
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Outlined.Search, "搜索")
                }
                
                Box {
                    var showMoodFilter by remember { mutableStateOf(false) }
                    
                    IconButton(onClick = { showMoodFilter = !showMoodFilter }) {
                        Icon(Icons.Outlined.FilterList, "筛选")
                    }
                    
                    DropdownMenu(
                        expanded = showMoodFilter,
                        onDismissRequest = { showMoodFilter = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("全部") },
                            onClick = { selectedMood = -1; showMoodFilter = false }
                        )
                        DropdownMenuItem(
                            text = { Text("一般") },
                            onClick = { selectedMood = 0; showMoodFilter = false }
                        )
                        DropdownMenuItem(
                            text = { Text("开心") },
                            onClick = { selectedMood = 1; showMoodFilter = false }
                        )
                        DropdownMenuItem(
                            text = { Text("难过") },
                            onClick = { selectedMood = 2; showMoodFilter = false }
                        )
                        DropdownMenuItem(
                            text = { Text("兴奋") },
                            onClick = { selectedMood = 3; showMoodFilter = false }
                        )
                        DropdownMenuItem(
                            text = { Text("疲惫") },
                            onClick = { selectedMood = 4; showMoodFilter = false }
                        )
                    }
                }
            }
            
            // 内容区域
            if (filteredDiaries.isEmpty()) {
                EmptyDiaryContent()
            } else {
                DiaryList(
                    diaries = filteredDiaries,
                    onDiaryClick = { diary -> onDiaryClick(diary.id) }
                )
            }
        }
        
        // 悬浮按钮
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "添加日记",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
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
    onDiaryClick: (Diary) -> Unit
) {
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
                    onClick = { onDiaryClick(diary) }
                )
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
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun DiaryItem(
    diary: Diary,
    onClick: () -> Unit
) {
    val moodColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,  // 一般
        MaterialTheme.colorScheme.tertiary,        // 开心
        MaterialTheme.colorScheme.error,           // 难过
        MaterialTheme.colorScheme.primary,         // 兴奋
        MaterialTheme.colorScheme.outline          // 疲惫
    )
    
    val moodIcons = listOf(
        Icons.Outlined.Face,         // 一般
        Icons.Outlined.Mood,         // 开心
        Icons.Outlined.SentimentDissatisfied,  // 难过
        Icons.Outlined.Celebration,  // 兴奋
        Icons.Outlined.NightShelter   // 疲惫
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(moodColors[diary.mood].copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = moodIcons[diary.mood],
                    contentDescription = null,
                    tint = moodColors[diary.mood]
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = diary.content.lines().firstOrNull() ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
        Icon(
            imageVector = Icons.Outlined.AutoStories,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "还没有日记",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "记录你的一天，点击右下角的按钮开始写日记",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
                                    if (selectedMood == index)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        Color.Transparent
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = moodIcons[index],
                                contentDescription = name,
                                tint = if (selectedMood == index)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedMood == index)
                                    MaterialTheme.colorScheme.onPrimaryContainer
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
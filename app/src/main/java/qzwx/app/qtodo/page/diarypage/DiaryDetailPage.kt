package qzwx.app.qtodo.page.diarypage

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import qzwx.app.qtodo.data.AppDatabase
import qzwx.app.qtodo.data.Diary
import qzwx.app.qtodo.repository.DiaryRepository
import qzwx.app.viewmodel.DiaryViewModel
import qzwx.app.viewmodel.DiaryViewModelFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDetailPage(
    diaryId: Long,
    onNavigateBack: () -> Unit
) {
    // 获取数据库和仓库
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = DiaryRepository(database.diaryDao())
    
    // 使用工厂创建ViewModel
    val viewModel: DiaryViewModel = viewModel(
        factory = DiaryViewModelFactory(repository)
    )
    
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 日记状态
    var diary by remember { mutableStateOf<Diary?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // 编辑状态
    var content by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf(0) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    
    // 加载日记数据
    LaunchedEffect(diaryId) {
        viewModel.getDiaryById(diaryId).first()?.let {
            diary = it
            // 初始化编辑状态
            content = it.content
            mood = it.mood
            date = it.date
        }
    }
    
    val moodIcons = listOf(
        Icons.Outlined.Face,         // 一般
        Icons.Outlined.Mood,         // 开心
        Icons.Outlined.SentimentDissatisfied,  // 难过
        Icons.Outlined.Celebration,  // 兴奋
        Icons.Outlined.NightShelter   // 疲惫
    )
    
    val moodNames = listOf("一般", "开心", "难过", "兴奋", "疲惫")
    
    val moodColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,  // 一般
        MaterialTheme.colorScheme.tertiary,        // 开心
        MaterialTheme.colorScheme.error,           // 难过
        MaterialTheme.colorScheme.primary,         // 兴奋
        MaterialTheme.colorScheme.outline          // 疲惫
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        diary?.let { currentDiary ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 顶部操作区域
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                    
                    // 标题
                    Text(
                        text = if (isEditing) "编辑日记" else "日记详情",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    // 编辑/保存按钮
                    if (isEditing) {
                        Row {
                            // 取消按钮
                            IconButton(onClick = {
                                isEditing = false
                                // 重置为原始值
                                diary?.let {
                                    content = it.content
                                    mood = it.mood
                                    date = it.date
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "取消"
                                )
                            }
                            
                            // 保存按钮
                            IconButton(onClick = {
                                diary?.let {
                                    // 应用首行缩进
                                    val processedContent = addIndentation(content)
                                    val updatedDiary = it.copy(
                                        content = processedContent,
                                        mood = mood,
                                        date = date,
                                        updatedAt = LocalDateTime.now()
                                    )
                                    viewModel.updateDiary(updatedDiary)
                                    diary = updatedDiary
                                    isEditing = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("日记已更新")
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Save,
                                    contentDescription = "保存"
                                )
                            }
                        }
                    } else {
                        Row {
                            // 编辑按钮
                            IconButton(onClick = { isEditing = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "编辑"
                                )
                            }
                            
                            // 删除按钮
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "删除"
                                )
                            }
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(bottom = 16.dp))
                
                // 心情图标
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(moodColors[mood].copy(alpha = 0.2f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = moodIcons[mood],
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = moodColors[mood]
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 心情文字
                Text(
                    text = "心情: ${moodNames[currentDiary.mood]}",
                    style = MaterialTheme.typography.titleMedium,
                    color = moodColors[currentDiary.mood],
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 日期显示/编辑
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (isEditing) {
                        TextButton(
                            onClick = { showDatePicker = true }
                        ) {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("MM月dd日")),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("MM月dd日")),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 内容区域
                if (isEditing) {
                    // 编辑模式 - 文本框
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("日记内容") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp), // 固定高度
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 心情选择器
                    Text(
                        text = "选择心情",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        moodNames.forEachIndexed { index, name ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { mood = index }
                                    .background(
                                        if (mood == index) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = moodIcons[index],
                                    contentDescription = name,
                                    tint = if (mood == index)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (mood == index)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // 阅读模式 - 显示内容
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp) // 固定最小高度
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        // 直接显示内容，不添加额外缩进
                        Text(
                            text = currentDiary.content,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f)) // 填充剩余空间
                
                // 创建和更新时间
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider()
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "创建于",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentDiary.createdAt.format(
                                    DateTimeFormatter.ofPattern("MM-dd HH:mm")
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "更新于",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentDiary.updatedAt.format(
                                    DateTimeFormatter.ofPattern("MM-dd HH:mm")
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        } ?: run {
            // 日记不存在
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onNavigateBack
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "返回"
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "未找到此日记",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Snackbar显示
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除日记") },
            text = { Text("确定要删除这篇日记吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        diary?.let {
                            viewModel.deleteDiary(it)
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    // 日期选择器
    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("选择日期") },
            text = { 
                Text("当前选择的日期: ${date.format(DateTimeFormatter.ofPattern("MM月dd日"))}")
                
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    // 日期选择按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { date = date.minusDays(1) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("前一天")
                        }
                        
                        Button(
                            onClick = { date = LocalDate.now() },
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
                                if (date.isBefore(LocalDate.now())) {
                                    date = date.plusDays(1) 
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            enabled = date.isBefore(LocalDate.now())
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
                    // 重置为原日期
                    date = diary?.date ?: LocalDate.now()
                    showDatePicker = false 
                }) {
                    Text("取消")
                }
            }
        )
    }
} 
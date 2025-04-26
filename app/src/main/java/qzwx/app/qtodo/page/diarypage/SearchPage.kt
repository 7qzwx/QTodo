package qzwx.app.qtodo.page.diarypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import qzwx.app.qtodo.data.AppDatabase
import qzwx.app.qtodo.data.Diary
import qzwx.app.qtodo.repository.DiaryRepository
import qzwx.app.viewmodel.DiaryViewModel
import qzwx.app.viewmodel.DiaryViewModelFactory
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    modifier: Modifier = Modifier,
    onDiaryClick: (Long) -> Unit = {},
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
    
    val diariesState = viewModel.allDiaries.collectAsState(initial = emptyList())
    val diaries = diariesState.value
    
    // 搜索状态
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    // 过滤后的日记
    val filteredDiaries = remember(diaries, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            diaries.filter { diary ->
                diary.content.contains(searchQuery, ignoreCase = true)
            }.sortedByDescending { it.date }
        }
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Column(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        // 搜索栏
            Row(
                modifier = Modifier
                    .fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索日记内容...") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.onSurface)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close,
                                     contentDescription = "清除",
                                     tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                )
            }

        
        // 搜索结果
        if (searchQuery.isBlank()) {
            // 未输入搜索词时的提示
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "请输入搜索关键词",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (filteredDiaries.isEmpty()) {
            // 无搜索结果时的提示
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "没有找到相关内容",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // 显示搜索结果
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "找到 ${filteredDiaries.size} 条结果",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                items(filteredDiaries) { diary ->
                    SearchResultItem(
                        diary = diary,
                        searchQuery = searchQuery,
                        onClick = { onDiaryClick(diary.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    diary: Diary,
    searchQuery: String,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 日记日期和心情
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 日期
                Text(
                    text = diary.date.format(DateTimeFormatter.ofPattern("MM月dd日")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 心情
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = moodIcons[diary.mood],
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = moodColors[diary.mood]
                    )
                }
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            // 日记内容（只显示包含搜索词的部分）
            val content = diary.content
            val contentLines = content.lines()
            val matchingLines = contentLines.filter { it.contains(searchQuery, ignoreCase = true) }
            
            if (matchingLines.isNotEmpty()) {
                // 优先显示匹配的行
                Text(
                    text = matchingLines.first(),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (matchingLines.size > 1) {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // 如果没有匹配的行（通常不会发生，因为我们已经过滤过了）
                Text(
                    text = contentLines.first(),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
} 
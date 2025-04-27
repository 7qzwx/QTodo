package qzwx.app.qtodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import qzwx.app.qtodo.navigation.BottomNavBar
import qzwx.app.qtodo.navigation.NavRoutes
import qzwx.app.qtodo.navigation.SimpleNavHost
import qzwx.app.qtodo.page.calendarpage.CalendarPage
import qzwx.app.qtodo.page.diarypage.DiaryPage
import qzwx.app.qtodo.page.todopage.TodoPage
import qzwx.app.qtodo.theme.QDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QDemoTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // 判断是否在主页面
                val isMainScreen = currentRoute in NavRoutes.bottomNavScreens
                
                // 路由与页面索引之间的映射
                val routeToIndex = mapOf(
                    NavRoutes.TodoPage to 0,
                    NavRoutes.CalandarPage to 1,
                    NavRoutes.NotePage to 2
                )
                
                val indexToRoute = mapOf(
                    0 to NavRoutes.TodoPage,
                    1 to NavRoutes.CalandarPage,
                    2 to NavRoutes.NotePage
                )
                
                // 当前应选中的页面索引
                val currentPageIndex = remember(currentRoute) {
                    routeToIndex[currentRoute] ?: 0
                }
                
                // 使用可变状态记录当前页面索引
                var selectedPageIndex by remember { mutableStateOf(currentPageIndex) }
                
                // 当导航路由改变时更新选中页面
                LaunchedEffect(currentRoute) {
                    routeToIndex[currentRoute]?.let { index ->
                        selectedPageIndex = index
                    }
                }
                
                // 创建协程作用域用于页面切换
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (isMainScreen) {
                            BottomNavBar(
                                selectedPageIndex = selectedPageIndex,
                                onPageSelected = { index ->
                                    // 只在索引真正变化时才执行导航
                                    if (index != selectedPageIndex) {
                                        // 先更新选中索引，确保底部导航立即响应
                                        selectedPageIndex = index
                                        
                                        // 然后导航到对应路由
                                        val route = indexToRoute[index] ?: NavRoutes.TodoPage
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    if (isMainScreen) {
                        MainContent(
                            selectedPageIndex = selectedPageIndex,
                            onSelectedIndexChange = { index -> 
                                // 更新选中索引
                                selectedPageIndex = index
                                
                                // 导航到对应路由
                                val route = indexToRoute[index] ?: NavRoutes.TodoPage
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToDetail = { route -> navController.navigate(route) },
                            modifier = Modifier.padding(paddingValues)
                        )
                    } else {
                        // 详情页等使用常规NavHost
                        SimpleNavHost(
                            navController = navController,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(
    selectedPageIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    
    // 使用pagerState来控制ViewPager
    val pagerState = rememberPagerState(
        initialPage = selectedPageIndex,
        pageCount = { 3 } // 对应三个底部导航页面
    )
    
    // 添加一个标志位，防止循环更新
    var isPageChangeFromClick by remember { mutableStateOf(false) }
    
    // 当外部selectedPageIndex变化时，滚动ViewPager到目标页面
    LaunchedEffect(selectedPageIndex) {
        if (selectedPageIndex != pagerState.currentPage) {
            // 标记这次页面变化是由点击引起的
            isPageChangeFromClick = true
            // 使用动画滚动到新页面，避免跳跃感
            pagerState.animateScrollToPage(selectedPageIndex)
        }
    }
    
    // 当ViewPager页面变化时更新导航状态，但避免因点击按钮导致的重复更新
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        // 只处理由滑动引起的页面变化，而不是点击引起的
        // 当滑动停止且不是由点击引起的页面变化时，才更新状态
        if (!pagerState.isScrollInProgress && pagerState.currentPage != selectedPageIndex && !isPageChangeFromClick) {
            onSelectedIndexChange(pagerState.currentPage)
        } else if (!pagerState.isScrollInProgress && isPageChangeFromClick) {
            // 滑动停止后重置标志位
            isPageChangeFromClick = false
        }
    }
    
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = true // 允许用户滑动
    ) { page ->
        when (page) {
            0 -> TodoPage(
                onTodoClick = { todoId ->
                    onNavigateToDetail("${NavRoutes.TodoDetailPage}/$todoId")
                }
            )
            1 -> CalendarPage()
            2 -> DiaryPage(
                onDiaryClick = { diaryId ->
                    onNavigateToDetail("${NavRoutes.DiaryDetailPage}/$diaryId")
                },
                onSearchClick = {
                    onNavigateToDetail(NavRoutes.DiarySearchPage)
                }
            )
        }
    }
}



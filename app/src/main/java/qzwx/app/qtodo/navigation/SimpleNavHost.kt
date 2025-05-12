package qzwx.app.qtodo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import qzwx.app.qtodo.page.diarypage.DiaryDetailPage
import qzwx.app.qtodo.page.diarypage.SearchPage
import qzwx.app.qtodo.page.todopage.AddTodoPage
import qzwx.app.qtodo.page.todopage.TodoDetailPage
import qzwx.app.qtodo.page.todopage.TodoPage

/**
 * 简化版的NavHost，只处理详情页和搜索页等非主页面的导航
 * 主页面的导航和滑动由MainActivity中的ViewPager直接处理
 */
@Composable
fun SimpleNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.TodoPage,
        modifier = modifier.fillMaxSize()
    ) {
        // 为主页面设置导航目标
        composable(NavRoutes.TodoPage) { 
            TodoPage(
                onTodoClick = { todoId ->
                    navController.navigate("${NavRoutes.TodoDetailPage}/$todoId")
                },
                onAddTodoClick = {
                    navController.navigate(NavRoutes.AddTodoPage)
                }
            )
        }
        composable(NavRoutes.CalandarPage) { }
        composable(NavRoutes.NotePage) { }
        
        // Todo详情页面路由
        composable(
            route = "${NavRoutes.TodoDetailPage}/{todoId}",
            arguments = listOf(
                navArgument("todoId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val todoId = backStackEntry.arguments?.getLong("todoId") ?: 0L
            TodoDetailPage(
                todoId = todoId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // 添加待办页面路由
        composable(NavRoutes.AddTodoPage) {
            AddTodoPage(
                onNavigateBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
        
        // 日记详情页面路由
        composable(
            route = "${NavRoutes.DiaryDetailPage}/{diaryId}",
            arguments = listOf(
                navArgument("diaryId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val diaryId = backStackEntry.arguments?.getLong("diaryId") ?: 0L
            DiaryDetailPage(
                diaryId = diaryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // 日记搜索页面路由
        composable(NavRoutes.DiarySearchPage) {
            SearchPage(
                onDiaryClick = { diaryId ->
                    navController.navigate("${NavRoutes.DiaryDetailPage}/$diaryId") {
                        // 避免在导航栈中创建多个详情页面实例
                        popUpTo(NavRoutes.DiarySearchPage)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
} 
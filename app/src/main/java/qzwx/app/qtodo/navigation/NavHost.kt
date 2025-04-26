package qzwx.app.qtodo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import qzwx.app.qtodo.page.calendarpage.CalendarPage
import qzwx.app.qtodo.page.diarypage.DiaryDetailPage
import qzwx.app.qtodo.page.diarypage.DiaryPage
import qzwx.app.qtodo.page.diarypage.SearchPage
import qzwx.app.qtodo.page.todopage.TodoDetailPage
import qzwx.app.qtodo.page.todopage.TodoPage


@Composable
fun Q_NavHost(navController : NavHostController, modifier : Modifier) {
    NavHost(
        modifier = modifier.fillMaxSize(),
        navController = navController,
        startDestination = NavRoutes.TodoPage
    ) {
        composable(NavRoutes.NotePage) { 
            DiaryPage(
                onDiaryClick = { diaryId ->
                    navController.navigate("${NavRoutes.DiaryDetailPage}/$diaryId")
                },
                onSearchClick = {
                    navController.navigate(NavRoutes.DiarySearchPage)
                }
            ) 
        }
        
        composable(NavRoutes.CalandarPage) { CalendarPage() }
        
        composable(NavRoutes.TodoPage) { 
            TodoPage(
                onTodoClick = { todoId ->
                    navController.navigate("${NavRoutes.TodoDetailPage}/$todoId")
                }
            ) 
        }
        
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


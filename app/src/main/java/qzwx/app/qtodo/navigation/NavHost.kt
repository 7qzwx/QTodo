package qzwx.app.qtodo.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import qzwx.app.qtodo.page.calendarpage.CalendarPage
import qzwx.app.qtodo.page.notepage.NotePage
import qzwx.app.qtodo.page.todopage.TodoPage

@Composable
fun Q_NavHost(navController : NavHostController,modifier : Modifier) {
    NavHost(
        modifier = modifier.fillMaxSize(),
        navController = navController,
        startDestination = NavRoutes.NotePage
    ) {
        composable(NavRoutes.NotePage) { NotePage() }
        composable(NavRoutes.CalandarPage) { CalendarPage() }
        composable(NavRoutes.TodoPage) { TodoPage() }
    }
}


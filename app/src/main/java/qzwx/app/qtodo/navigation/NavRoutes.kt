package qzwx.app.qtodo.navigation

object NavRoutes {
    // 主底部导航的页面
    const val NotePage = "NotePage"
    const val CalandarPage = "CalandarPage"
    const val TodoPage = "TodoPage"

    // 不需要底部导航的页面
//    const val AiAccount = "AiAccount"

    const val TodoDetailPage = "TodoDetailPage"
    const val DiaryDetailPage = "DiaryDetailPage"
    const val DiarySearchPage = "DiarySearchPage"
    const val AddTodoPage = "AddTodoPage"

    // 需要显示底部导航的路径集合
    val bottomNavScreens = setOf(
       TodoPage,
       CalandarPage,
       NotePage
    )
}
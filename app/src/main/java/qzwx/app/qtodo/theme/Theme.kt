package qzwx.app.qtodo.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xff00574A),
    background = Color(0xFF141C25),
    surface = Color(0xFF222A33),
    onSurface = Color(0xFFF2F2F2),
)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xff48AB93),
    background = Color(0xFFF2F2F2),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun QDemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,  //取消系统颜色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }
        // 设置系统 状态栏颜色跟背景一致
    val systemUiController = rememberSystemUiController()
    val syscolor = if (isSystemInDarkTheme()) {
        Color(0xFF141C25)
    } else {
        Color(0xFFF2F2F2)
    }
    val darkiconscolor = if (isSystemInDarkTheme()) {
        false
    } else true
    SideEffect {
        systemUiController.setStatusBarColor(
            color = syscolor, // 状态栏
            darkIcons = darkiconscolor, // 根据主题设置图标颜色
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
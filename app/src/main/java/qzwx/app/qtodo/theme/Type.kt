package qzwx.app.qtodo.theme

import androidx.compose.material3.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import qzwx.app.qtodo.R
// 定义一些常用的字体字重
val light = FontWeight.Light
val regular = FontWeight.Normal
val medium = FontWeight.Medium
val semiBold = FontWeight.SemiBold
val bold = FontWeight.Bold

// 加载自定义字体
val QcustomFont = FontFamily.Default
/**
 * 使用方法：
 *
 * Text(
 *
 *     text = "这是一个标题",
 *
 *     style = MaterialTheme.typography.titleLarge
 *
 * )
 *
 * 样式分类和用途：
 *
 * Display系列：用于最大、最显眼的文本展示
 * - displayLarge: 大型展示文本（28sp）
 * - displayMedium: 中型展示文本（24sp）
 * - displaySmall: 小型展示文本（20sp）
 *
 * Title系列：用于标题
 * - titleLarge: 大型标题（22sp）
 * - titleMedium: 中型标题（18sp）
 * - titleSmall: 小型标题（16sp）
 *
 * Body系列：用于正文内容
 * - bodyLarge: 大号正文（16sp）
 * - bodyMedium: 中号正文（14sp）
 * - bodySmall: 小号正文（12sp）
 *
 * Label系列：用于标签、提示等小文本
 * - labelLarge: 大号标签（14sp）
 * - labelMedium: 中号标签（12sp）
 * - labelSmall: 小号标签（11sp）
 */
val Typography = Typography(
    // 大标题
    displayLarge = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    // 中标题
    displayMedium = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = semiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    // 小标题
    displaySmall = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // 标题大
    titleLarge = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = semiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // 标题中
    titleMedium = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    // 标题小
    titleSmall = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    // 正文大
    bodyLarge = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = regular,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // 正文中
    bodyMedium = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = regular,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // 正文小
    bodySmall = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = regular,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // 标签大
    labelLarge = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // 标签中
    labelMedium = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    // 标签小
    labelSmall = TextStyle(
        fontFamily = QcustomFont,
        fontWeight = medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
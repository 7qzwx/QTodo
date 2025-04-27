package qzwx.app.qtodo.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 简化版的底部导航栏，不依赖于NavController
 * 使用页面索引而不是路由来跟踪当前选中的页�?
 */
@Composable
fun BottomNavBar(
    selectedPageIndex: Int,
    onPageSelected: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // 底部导航�?
    val items = listOf(
        NavItem(
            label = "待办", 
            iconOutlined = Icons.Outlined.Today,
            iconFilled = Icons.Filled.Today
        ),
        NavItem(
            label = "日历", 
            iconOutlined = Icons.Outlined.CalendarMonth,
            iconFilled = Icons.Filled.CalendarMonth
        ),
        NavItem(
            label = "笔记", 
            iconOutlined = Icons.Outlined.NoteAlt,
            iconFilled = Icons.Filled.NoteAlt
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedPageIndex
                
                // 使用动画颜色渐变提升视觉效果
                val iconTintColor by animateColorAsState(
                    targetValue = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                    label = "iconTintColor"
                )
                
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                    label = "textColor"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null, 
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            // 只有在非选中状态下才需要处理点�?
                            if (!isSelected) {
                                // 触觉反馈提升用户体验
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                
                                // 通知页面索引变化
                                onPageSelected(index)
                            }
                        }
                        .padding(vertical = 8.dp)
                ) {
                    // 使用填充图标表示选中状�?
                    Icon(
                        imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                        contentDescription = item.label,
                        tint = iconTintColor,
                        modifier = Modifier.size(26.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}

/**
 * 导航项数据类，移除了route属�?
 */
data class NavItem(
    val label: String,
    val iconOutlined: ImageVector,  // 未选中状态图�?
    val iconFilled: ImageVector     // 选中状态图�?
)

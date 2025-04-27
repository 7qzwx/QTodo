package qzwx.app.qtodo.utils

import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 全局Snackbar管理器
 * 用于防止短时间内重复显示相同的消息
 */
object SnackbarManager {
    // 用于存储最近显示的消息及其时间戳
    private val recentMessages = ConcurrentHashMap<String, Long>()
    
    // 防止并发冲突
    private val mutex = Mutex()
    
    // 消息防抖时间（毫秒）
    private const val DEBOUNCE_TIME_MS = 2000L
    
    /**
     * 显示一条Snackbar消息，如果相同消息在短时间内已经显示过，则不再显示
     * 
     * @param snackbarHostState 当前SnackbarHostState
     * @param message 要显示的消息
     * @param actionLabel 操作按钮文本（可选）
     * @return 如果消息被显示，返回Snackbar的结果；如果消息被抑制，返回null
     */
    suspend fun showSnackbar(
        snackbarHostState: SnackbarHostState,
        message: String,
        actionLabel: String? = null
    ): Boolean {
        return mutex.withLock {
            val currentTime = System.currentTimeMillis()
            val lastShownTime = recentMessages[message] ?: 0L
            
            // 检查是否在防抖时间内
            if (currentTime - lastShownTime > DEBOUNCE_TIME_MS) {
                // 更新最后显示时间
                recentMessages[message] = currentTime
                
                // 显示消息
                snackbarHostState.showSnackbar(message, actionLabel)
                
                // 清理过期消息
                cleanupOldMessages()
                
                true
            } else {
                // 消息在防抖时间内已经显示过，跳过
                false
            }
        }
    }
    
    /**
     * 清理超过防抖时间的旧消息记录
     */
    private fun cleanupOldMessages() {
        val currentTime = System.currentTimeMillis()
        val expiredTime = currentTime - DEBOUNCE_TIME_MS
        
        recentMessages.entries.removeIf { (_, timestamp) -> 
            timestamp < expiredTime 
        }
    }
} 
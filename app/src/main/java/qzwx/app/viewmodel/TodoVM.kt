package qzwx.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import qzwx.app.qtodo.data.Todo
import qzwx.app.qtodo.repository.TodoRepository
import java.time.LocalDateTime

class TodoViewModel(private val repository: TodoRepository) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()

    // 所有Todo列表
    val allTodos = repository.allTodos
    
    // 活动的Todo列表
    val activeTodos = repository.activeTodos
    
    // 已完成的Todo列表
    val completedTodos = repository.completedTodos

    // 获取单个Todo
    fun getTodoById(id: Long): Flow<Todo?> {
        return repository.getTodoById(id)
    }

    // 添加新Todo
    fun addTodo(title: String, description: String = "", priority: Int = 0, dueDate: LocalDateTime? = null) {
        if (title.isBlank()) return
        
        val todo = Todo(
            title = title,
            description = description,
            priority = priority,
            dueDate = dueDate,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        viewModelScope.launch {
            repository.insertTodo(todo)
        }
    }

    // 更新Todo
    fun updateTodo(todo: Todo) {
        viewModelScope.launch {
            val updatedTodo = todo.copy(updatedAt = LocalDateTime.now())
            repository.updateTodo(updatedTodo)
        }
    }

    // 删除Todo
    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }

    // 切换Todo完成状态
    fun toggleTodoCompleted(todo: Todo) {
        viewModelScope.launch {
            val updatedTodo = todo.copy(
                isCompleted = !todo.isCompleted,
                updatedAt = LocalDateTime.now()
            )
            repository.updateTodo(updatedTodo)
        }
    }

    // 更新UI状态
    fun updateFilterType(filterType: TodoFilterType) {
        _uiState.value = _uiState.value.copy(filterType = filterType)
    }
}

// UI状态数据类
data class TodoUiState(
    val filterType: TodoFilterType = TodoFilterType.ALL
)

// 过滤类型枚举
enum class TodoFilterType {
    ALL, ACTIVE, COMPLETED
}

// ViewModel工厂
class TodoViewModelFactory(private val repository: TodoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


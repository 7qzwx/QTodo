package qzwx.app.qtodo.repository

import kotlinx.coroutines.flow.Flow
import qzwx.app.qtodo.data.Todo
import qzwx.app.qtodo.data.TodoDao

class TodoRepository(private val todoDao: TodoDao) {
    
    val allTodos: Flow<List<Todo>> = todoDao.getAllTodos()
    
    val activeTodos: Flow<List<Todo>> = todoDao.getActiveTodos()
    
    val completedTodos: Flow<List<Todo>> = todoDao.getCompletedTodos()
    
    fun getTodoById(id: Long): Flow<Todo?> {
        return todoDao.getTodoById(id)
    }
    
    suspend fun insertTodo(todo: Todo): Long {
        return todoDao.insertTodo(todo)
    }
    
    suspend fun updateTodo(todo: Todo) {
        todoDao.updateTodo(todo)
    }
    
    suspend fun deleteTodo(todo: Todo) {
        todoDao.deleteTodo(todo)
    }
    
    suspend fun toggleTodoCompleted(todo: Todo) {
        val updatedTodo = todo.copy(isCompleted = !todo.isCompleted)
        todoDao.updateTodo(updatedTodo)
    }
} 
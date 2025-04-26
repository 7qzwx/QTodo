package qzwx.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import qzwx.app.qtodo.data.Diary
import qzwx.app.qtodo.repository.DiaryRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class DiaryViewModel(private val repository: DiaryRepository) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    // 所有日记列表
    val allDiaries = repository.allDiaries

    // 获取指定日期的日记
    fun getDiaryByDate(date: LocalDate): Flow<Diary?> {
        return repository.getDiaryByDate(date)
    }

    // 获取指定ID的日记
    fun getDiaryById(id: Long): Flow<Diary?> {
        return repository.getDiaryById(id)
    }

    // 获取日期范围内的日记
    fun getDiariesBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Diary>> {
        return repository.getDiariesBetweenDates(startDate, endDate)
    }

    // 添加或更新日记
    fun saveDiary(date: LocalDate = LocalDate.now(), content: String, mood: Int = 0) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            val existingDiary = repository.getDiaryByDate(date)
            
            val diary = Diary(
                date = date,
                content = content,
                mood = mood,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            
            repository.insertDiary(diary)
        }
    }

    // 更新日记
    fun updateDiary(diary: Diary) {
        viewModelScope.launch {
            val updatedDiary = diary.copy(updatedAt = LocalDateTime.now())
            repository.updateDiary(updatedDiary)
        }
    }

    // 删除日记
    fun deleteDiary(diary: Diary) {
        viewModelScope.launch {
            repository.deleteDiary(diary)
        }
    }

    // 更新UI状态 - 选择日期
    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
    }

    // 更新UI状态 - 设置日期范围
    fun setDateRange(range: DiaryDateRange) {
        val today = LocalDate.now()
        
        val (startDate, endDate) = when (range) {
            DiaryDateRange.LAST_WEEK -> Pair(
                today.minus(7, ChronoUnit.DAYS),
                today
            )
            DiaryDateRange.LAST_MONTH -> Pair(
                today.minus(1, ChronoUnit.MONTHS),
                today
            )
            DiaryDateRange.LAST_YEAR -> Pair(
                today.minus(1, ChronoUnit.YEARS),
                today
            )
            DiaryDateRange.CUSTOM -> Pair(
                _uiState.value.customStartDate,
                _uiState.value.customEndDate
            )
        }
        
        _uiState.value = _uiState.value.copy(
            dateRange = range,
            effectiveStartDate = startDate,
            effectiveEndDate = endDate
        )
    }

    // 更新UI状态 - 设置自定义日期范围
    fun setCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        _uiState.value = _uiState.value.copy(
            customStartDate = startDate,
            customEndDate = endDate,
            effectiveStartDate = startDate,
            effectiveEndDate = endDate,
            dateRange = DiaryDateRange.CUSTOM
        )
    }
}

// UI状态数据类
data class DiaryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val dateRange: DiaryDateRange = DiaryDateRange.LAST_WEEK,
    val customStartDate: LocalDate = LocalDate.now().minus(7, ChronoUnit.DAYS),
    val customEndDate: LocalDate = LocalDate.now(),
    val effectiveStartDate: LocalDate = LocalDate.now().minus(7, ChronoUnit.DAYS),
    val effectiveEndDate: LocalDate = LocalDate.now()
)

// 日记日期范围枚举
enum class DiaryDateRange {
    LAST_WEEK, LAST_MONTH, LAST_YEAR, CUSTOM
}

// ViewModel工厂
class DiaryViewModelFactory(private val repository: DiaryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiaryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


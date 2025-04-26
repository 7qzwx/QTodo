package qzwx.app.qtodo.repository

import kotlinx.coroutines.flow.Flow
import qzwx.app.qtodo.data.Diary
import qzwx.app.qtodo.data.DiaryDao
import java.time.LocalDate

class DiaryRepository(private val diaryDao: DiaryDao) {
    
    val allDiaries: Flow<List<Diary>> = diaryDao.getAllDiaries()
    
    fun getDiaryById(id: Long): Flow<Diary?> {
        return diaryDao.getDiaryById(id)
    }
    
    fun getDiaryByDate(date: LocalDate): Flow<Diary?> {
        return diaryDao.getDiaryByDate(date)
    }
    
    fun getDiariesBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Diary>> {
        return diaryDao.getDiariesBetweenDates(startDate, endDate)
    }
    
    suspend fun insertDiary(diary: Diary): Long {
        return diaryDao.insertDiary(diary)
    }
    
    suspend fun updateDiary(diary: Diary) {
        diaryDao.updateDiary(diary)
    }
    
    suspend fun deleteDiary(diary: Diary) {
        diaryDao.deleteDiary(diary)
    }
} 
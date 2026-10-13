package duoc.cl.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import duoc.cl.data.local.LogbookEntity
import duoc.cl.data.local.LogbookEntryEntity
import duoc.cl.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository.getInstance()

    val courses = repository.courses
    val subjects = repository.subjects

    private val _selectedCourseId = MutableStateFlow<Long?>(null)
    val selectedCourseId: StateFlow<Long?> = _selectedCourseId.asStateFlow()

    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    val selectedSubjectId: StateFlow<Long?> = _selectedSubjectId.asStateFlow()

    fun selectCourseAndSubject(courseId: Long, subjectId: Long) {
        _selectedCourseId.value = courseId
        _selectedSubjectId.value = subjectId
    }

    fun getLogbooks(courseId: Long, subjectId: Long): Flow<List<LogbookEntity>> {
        return repository.getLogbooks(courseId, subjectId)
    }

    fun getEntries(logbookId: Long): Flow<List<LogbookEntryEntity>> {
        return repository.getEntriesByLogbook(logbookId)
    }

    data class LogbookFormState(
        val month: String = "",
        val title: String = "",
        val purpose: String = "",
        val instructions: String = "",
        val guidingQuestions: String = "",
        val monthError: String? = null,
        val titleError: String? = null,
        val purposeError: String? = null,
        val instructionsError: String? = null,
        val isFormValid: Boolean = false
    )

    private val _formState = MutableStateFlow(LogbookFormState())
    val formState: StateFlow<LogbookFormState> = _formState.asStateFlow()

    fun onMonthChanged(value: String) {
        val error = if (value.isBlank()) "El mes es obligatorio" else null
        _formState.update { it.copy(month = value, monthError = error).validate() }
    }

    fun onTitleChanged(value: String) {
        val error = if (value.isBlank()) "El título es obligatorio" else null
        _formState.update { it.copy(title = value, titleError = error).validate() }
    }

    fun onPurposeChanged(value: String) {
        val error = if (value.isBlank()) "El propósito es obligatorio" else null
        _formState.update { it.copy(purpose = value, purposeError = error).validate() }
    }

    fun onInstructionsChanged(value: String) {
        val error = if (value.isBlank()) "Las instrucciones son obligatorias" else null
        _formState.update { it.copy(instructions = value, instructionsError = error).validate() }
    }

    fun onGuidingQuestionsChanged(value: String) {
        _formState.update { it.copy(guidingQuestions = value) }
    }

    private fun LogbookFormState.validate(): LogbookFormState {
        val valid = month.isNotBlank() && title.isNotBlank() && purpose.isNotBlank() && instructions.isNotBlank() &&
                monthError == null && titleError == null && purposeError == null && instructionsError == null
        return copy(isFormValid = valid)
    }

    fun createLogbook(courseId: Long, subjectId: Long, onResult: (Boolean) -> Unit) {
        val state = _formState.value
        if (!state.isFormValid) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            repository.insertLogbook(
                LogbookEntity(
                    courseId = courseId,
                    subjectId = subjectId,
                    month = state.month,
                    title = state.title,
                    purpose = state.purpose,
                    instructions = state.instructions,
                    guidingQuestions = state.guidingQuestions
                )
            )
            _formState.value = LogbookFormState()
            onResult(true)
        }
    }

    fun updateFeedback(entryId: Long, feedback: String, isReviewed: Boolean) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)
            entry?.let {
                repository.updateEntry(it.copy(feedback = feedback, isReviewed = isReviewed))
            }
        }
    }

    fun toggleCloseLogbook(logbook: LogbookEntity) {
        viewModelScope.launch {
            repository.updateLogbook(logbook.copy(isClosed = !logbook.isClosed))
        }
    }
}

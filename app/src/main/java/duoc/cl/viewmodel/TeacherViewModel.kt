package duoc.cl.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import duoc.cl.data.local.LogbookEntity
import duoc.cl.data.local.LogbookEntryEntity
import duoc.cl.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class LogbookStatusFilter(val label: String) {
    ALL("Todas"),
    ACTIVE("Activas"),
    CLOSED("Cerradas")
}

enum class EntryStatusFilter(val label: String) {
    ALL("Todas"),
    PENDING("Pendientes"),
    REVIEWED("Revisadas")
}

class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository.getInstance(application)

    val courses = repository.courses
    val subjects = repository.subjects

    private val _selectedCourseId = MutableStateFlow<Long?>(null)
    val selectedCourseId: StateFlow<Long?> = _selectedCourseId.asStateFlow()

    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    val selectedSubjectId: StateFlow<Long?> = _selectedSubjectId.asStateFlow()

    private val _logbookSearchQuery = MutableStateFlow("")
    val logbookSearchQuery: StateFlow<String> = _logbookSearchQuery.asStateFlow()

    private val _logbookStatusFilter = MutableStateFlow(LogbookStatusFilter.ALL)
    val logbookStatusFilter: StateFlow<LogbookStatusFilter> = _logbookStatusFilter.asStateFlow()

    private val _entrySearchQuery = MutableStateFlow("")
    val entrySearchQuery: StateFlow<String> = _entrySearchQuery.asStateFlow()

    private val _entryStatusFilter = MutableStateFlow(EntryStatusFilter.ALL)
    val entryStatusFilter: StateFlow<EntryStatusFilter> = _entryStatusFilter.asStateFlow()

    fun setLogbookSearchQuery(query: String) {
        _logbookSearchQuery.value = query
    }

    fun setLogbookStatusFilter(filter: LogbookStatusFilter) {
        _logbookStatusFilter.value = filter
    }

    fun setEntrySearchQuery(query: String) {
        _entrySearchQuery.value = query
    }

    fun setEntryStatusFilter(filter: EntryStatusFilter) {
        _entryStatusFilter.value = filter
    }

    fun selectCourseAndSubject(courseId: Long, subjectId: Long) {
        _selectedCourseId.value = courseId
        _selectedSubjectId.value = subjectId
    }

    fun getLogbooks(courseId: Long, subjectId: Long): Flow<List<LogbookEntity>> {
        return combine(
            repository.getLogbooks(courseId, subjectId),
            _logbookSearchQuery,
            _logbookStatusFilter
        ) { logbooks, query, statusFilter ->
            logbooks.filter { logbook ->
                val matchesQuery = query.isBlank() ||
                        logbook.title.contains(query, ignoreCase = true) ||
                        logbook.month.contains(query, ignoreCase = true) ||
                        logbook.purpose.contains(query, ignoreCase = true)
                val matchesStatus = when (statusFilter) {
                    LogbookStatusFilter.ALL -> true
                    LogbookStatusFilter.ACTIVE -> !logbook.isClosed
                    LogbookStatusFilter.CLOSED -> logbook.isClosed
                }
                matchesQuery && matchesStatus
            }
        }
    }

    fun getEntries(logbookId: Long): Flow<List<LogbookEntryEntity>> {
        return combine(
            repository.getEntriesByLogbook(logbookId),
            _entrySearchQuery,
            _entryStatusFilter
        ) { entries, query, statusFilter ->
            entries.filter { entry ->
                val matchesQuery = query.isBlank() ||
                        entry.studentName.contains(query, ignoreCase = true) ||
                        entry.contents.contains(query, ignoreCase = true) ||
                        entry.activities.contains(query, ignoreCase = true) ||
                        entry.learnings.contains(query, ignoreCase = true)
                val matchesStatus = when (statusFilter) {
                    EntryStatusFilter.ALL -> true
                    EntryStatusFilter.PENDING -> !entry.isReviewed
                    EntryStatusFilter.REVIEWED -> entry.isReviewed
                }
                matchesQuery && matchesStatus
            }
        }
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

    private var editingLogbook: LogbookEntity? = null

    val isEditingLogbook: Boolean
        get() = editingLogbook != null

    fun prepareCreateLogbook() {
        editingLogbook = null
        _formState.value = LogbookFormState()
    }

    fun prepareEditLogbook(logbook: LogbookEntity) {
        editingLogbook = logbook
        _formState.value = LogbookFormState(
            month = logbook.month,
            title = logbook.title,
            purpose = logbook.purpose,
            instructions = logbook.instructions,
            guidingQuestions = logbook.guidingQuestions
        ).validate()
    }

    fun loadAndPrepareEditLogbook(logbookId: Long) {
        viewModelScope.launch {
            val logbook = repository.getLogbookById(logbookId)
            if (logbook != null) {
                prepareEditLogbook(logbook)
            } else {
                prepareCreateLogbook()
            }
        }
    }

    fun createLogbook(courseId: Long, subjectId: Long, onResult: (Boolean) -> Unit) {
        saveLogbook(courseId, subjectId, onResult)
    }

    fun saveLogbook(courseId: Long, subjectId: Long, onResult: (Boolean) -> Unit) {
        val state = _formState.value
        if (!state.isFormValid) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val currentEditing = editingLogbook
            if (currentEditing != null) {
                val updated = currentEditing.copy(
                    month = state.month,
                    title = state.title,
                    purpose = state.purpose,
                    instructions = state.instructions,
                    guidingQuestions = state.guidingQuestions
                )
                repository.updateLogbook(updated)
            } else {
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
            }
            editingLogbook = null
            _formState.value = LogbookFormState()
            onResult(true)
        }
    }

    fun deleteLogbook(logbookId: Long, onResult: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteLogbook(logbookId)
            onResult()
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

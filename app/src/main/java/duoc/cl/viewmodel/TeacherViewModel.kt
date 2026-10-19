package duoc.cl.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import duoc.cl.data.local.LogbookEntity
import duoc.cl.data.local.LogbookEntryEntity
import duoc.cl.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Filtro de estado de las Bitácoras Mensuales.
 */
enum class LogbookStatusFilter(val label: String) {
    ALL("Todas"),
    ACTIVE("Activas"),
    CLOSED("Cerradas")
}

/**
 * Filtro de estado de los Aportes/Registros de Estudiantes.
 */
enum class EntryStatusFilter(val label: String) {
    ALL("Todas"),
    PENDING("Pendientes"),
    REVIEWED("Revisadas")
}

/**
 * ViewModel encargado del módulo de Docentes.
 * Administra la creación, edición, borrado, cierre/apertura de bitácoras,
 * la revisión y retroalimentación de aportes de estudiantes, y los filtros de búsqueda por mes y estado.
 */
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

    private val _selectedMonthFilter = MutableStateFlow("Todos")
    val selectedMonthFilter: StateFlow<String> = _selectedMonthFilter.asStateFlow()

    private val _logbookStatusFilter = MutableStateFlow(LogbookStatusFilter.ALL)
    val logbookStatusFilter: StateFlow<LogbookStatusFilter> = _logbookStatusFilter.asStateFlow()

    private val _entrySearchQuery = MutableStateFlow("")
    val entrySearchQuery: StateFlow<String> = _entrySearchQuery.asStateFlow()

    private val _entryStatusFilter = MutableStateFlow(EntryStatusFilter.ALL)
    val entryStatusFilter: StateFlow<EntryStatusFilter> = _entryStatusFilter.asStateFlow()

    // Opciones de filtrado por mes
    val monthOptions = listOf("Todos", "Marzo 2025", "Abril 2025", "Mayo 2025", "Junio 2025", "Julio 2025", "Agosto 2025", "Septiembre 2025", "Octubre 2025", "Noviembre 2025", "Diciembre 2025")

    /**
     * Establece la consulta de búsqueda textual para bitácoras.
     */
    fun setLogbookSearchQuery(query: String) {
        _logbookSearchQuery.value = query
    }

    /**
     * Establece el mes seleccionado para filtrar bitácoras.
     */
    fun setSelectedMonthFilter(month: String) {
        _selectedMonthFilter.value = month
    }

    /**
     * Establece el filtro de estado (Todas/Activas/Cerradas).
     */
    fun setLogbookStatusFilter(filter: LogbookStatusFilter) {
        _logbookStatusFilter.value = filter
    }

    /**
     * Establece la consulta de búsqueda de aportes de estudiantes.
     */
    fun setEntrySearchQuery(query: String) {
        _entrySearchQuery.value = query
    }

    /**
     * Establece el filtro de revisión de aportes (Todas/Pendientes/Revisadas).
     */
    fun setEntryStatusFilter(filter: EntryStatusFilter) {
        _entryStatusFilter.value = filter
    }

    /**
     * Selecciona el curso y asignatura para mostrar las bitácoras correspondientes.
     */
    fun selectCourseAndSubject(courseId: Long, subjectId: Long) {
        _selectedCourseId.value = courseId
        _selectedSubjectId.value = subjectId
    }

    /**
     * Obtiene el flujo de bitácoras filtradas dinámicamente por título, mes y estado (Activa/Cerrada).
     */
    fun getLogbooks(courseId: Long, subjectId: Long): Flow<List<LogbookEntity>> {
        return combine(
            repository.getLogbooks(courseId, subjectId),
            _logbookSearchQuery,
            _selectedMonthFilter,
            _logbookStatusFilter
        ) { logbooks, query, month, statusFilter ->
            logbooks.filter { logbook ->
                val matchesQuery = query.isBlank() ||
                        logbook.title.contains(query, ignoreCase = true) ||
                        logbook.month.contains(query, ignoreCase = true) ||
                        logbook.purpose.contains(query, ignoreCase = true)
                val matchesMonth = month == "Todos" || logbook.month.contains(month, ignoreCase = true)
                val matchesStatus = when (statusFilter) {
                    LogbookStatusFilter.ALL -> true
                    LogbookStatusFilter.ACTIVE -> !logbook.isClosed
                    LogbookStatusFilter.CLOSED -> logbook.isClosed
                }
                matchesQuery && matchesMonth && matchesStatus
            }
        }
    }

    /**
     * Obtiene los aportes de estudiantes para una bitácora específica con filtros por nombre/contenido y estado de revisión.
     */
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

    /**
     * Estado del formulario de creación y edición de bitácoras.
     */
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

    private val _editingLogbook = MutableStateFlow<LogbookEntity?>(null)
    val editingLogbook: StateFlow<LogbookEntity?> = _editingLogbook.asStateFlow()

    val isEditingLogbook: Boolean
        get() = _editingLogbook.value != null

    /**
     * Manejador para el campo de mes/año con validación.
     */
    fun onMonthChanged(value: String) {
        val error = if (value.isBlank()) "El mes es obligatorio" else null
        _formState.update { it.copy(month = value, monthError = error).validate() }
    }

    /**
     * Manejador para el título de la bitácora.
     */
    fun onTitleChanged(value: String) {
        val error = if (value.isBlank()) "El título es obligatorio" else null
        _formState.update { it.copy(title = value, titleError = error).validate() }
    }

    /**
     * Manejador para el propósito pedagógico.
     */
    fun onPurposeChanged(value: String) {
        val error = if (value.isBlank()) "El propósito es obligatorio" else null
        _formState.update { it.copy(purpose = value, purposeError = error).validate() }
    }

    /**
     * Manejador para las instrucciones para estudiantes.
     */
    fun onInstructionsChanged(value: String) {
        val error = if (value.isBlank()) "Las instrucciones son obligatorias" else null
        _formState.update { it.copy(instructions = value, instructionsError = error).validate() }
    }

    /**
     * Manejador para las preguntas guiadas opcionales.
     */
    fun onGuidingQuestionsChanged(value: String) {
        _formState.update { it.copy(guidingQuestions = value) }
    }

    private fun LogbookFormState.validate(): LogbookFormState {
        val valid = month.isNotBlank() && title.isNotBlank() && purpose.isNotBlank() && instructions.isNotBlank() &&
                monthError == null && titleError == null && purposeError == null && instructionsError == null
        return copy(isFormValid = valid)
    }

    /**
     * Prepara el formulario para crear una nueva bitácora.
     */
    fun prepareCreateLogbook() {
        _editingLogbook.value = null
        _formState.value = LogbookFormState()
    }

    /**
     * Carga una bitácora existente en el formulario para modificar sus datos.
     */
    fun prepareEditLogbook(logbook: LogbookEntity) {
        _editingLogbook.value = logbook
        _formState.value = LogbookFormState(
            month = logbook.month,
            title = logbook.title,
            purpose = logbook.purpose,
            instructions = logbook.instructions,
            guidingQuestions = logbook.guidingQuestions
        ).validate()
    }

    /**
     * Carga desde el repositorio una bitácora por ID y prepara el estado de edición.
     */
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

    /**
     * Guarda la bitácora creada o los cambios realizados en una bitácora existente.
     */
    fun saveLogbook(courseId: Long, subjectId: Long, onResult: (Boolean) -> Unit) {
        val state = _formState.value
        if (!state.isFormValid) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val currentEditing = _editingLogbook.value
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
            _editingLogbook.value = null
            _formState.value = LogbookFormState()
            onResult(true)
        }
    }

    /**
     * Elimina una bitácora por su ID.
     */
    fun deleteLogbook(logbookId: Long, onResult: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteLogbook(logbookId)
            onResult()
        }
    }

    /**
     * Registra o actualiza los comentarios de retroalimentación entregados por el docente.
     */
    fun updateFeedback(entryId: Long, feedback: String, isReviewed: Boolean) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)
            entry?.let {
                repository.updateEntry(it.copy(feedback = feedback, isReviewed = isReviewed))
            }
        }
    }

    /**
     * Cambia el estado de una bitácora entre Abierta (permite aportes) y Cerrada (bloquea aportes).
     */
    fun toggleCloseLogbook(logbook: LogbookEntity) {
        viewModelScope.launch {
            repository.updateLogbook(logbook.copy(isClosed = !logbook.isClosed))
        }
    }
}

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
 * ViewModel encargado del módulo de Estudiantes.
 * Gestiona la visualización de bitácoras por curso y asignatura,
 * el envío y edición de respuestas/aportes, y la integración de fotos y GPS.
 */
class StudentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository.getInstance(application)

    val courses = repository.courses
    val subjects = repository.subjects

    private val _logbookSearchQuery = MutableStateFlow("")
    val logbookSearchQuery: StateFlow<String> = _logbookSearchQuery.asStateFlow()

    private val _selectedMonthFilter = MutableStateFlow("Todos")
    val selectedMonthFilter: StateFlow<String> = _selectedMonthFilter.asStateFlow()

    private val _logbookStatusFilter = MutableStateFlow(LogbookStatusFilter.ALL)
    val logbookStatusFilter: StateFlow<LogbookStatusFilter> = _logbookStatusFilter.asStateFlow()

    val monthOptions = listOf("Todos", "Marzo 2025", "Abril 2025", "Mayo 2025", "Junio 2025", "Julio 2025", "Agosto 2025", "Septiembre 2025", "Octubre 2025", "Noviembre 2025", "Diciembre 2025")

    /**
     * Establece la búsqueda por texto.
     */
    fun setLogbookSearchQuery(query: String) {
        _logbookSearchQuery.value = query
    }

    /**
     * Establece el mes seleccionado en el filtro.
     */
    fun setSelectedMonthFilter(month: String) {
        _selectedMonthFilter.value = month
    }

    /**
     * Establece el filtro de estado (Todas/Abiertas/Cerradas).
     */
    fun setLogbookStatusFilter(filter: LogbookStatusFilter) {
        _logbookStatusFilter.value = filter
    }

    /**
     * Obtiene el flujo de bitácoras filtradas por curso, asignatura, mes y estado.
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
     * Obtiene los registros existentes del estudiante para una bitácora determinada.
     */
    fun getEntriesForLogbook(logbookId: Long) = repository.getEntriesByLogbook(logbookId)

    /**
     * Estado del formulario de registro de aporte por parte del estudiante.
     */
    data class EntryFormState(
        val studentName: String = "",
        val contents: String = "",
        val activities: String = "",
        val learnings: String = "",
        val doubts: String = "",
        val photoUri: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val studentNameError: String? = null,
        val contentsError: String? = null,
        val activitiesError: String? = null,
        val learningsError: String? = null,
        val isFormValid: Boolean = false
    )

    private val _entryFormState = MutableStateFlow(EntryFormState())
    val entryFormState: StateFlow<EntryFormState> = _entryFormState.asStateFlow()

    private val _editingEntryId = MutableStateFlow<Long?>(null)
    val editingEntryId: StateFlow<Long?> = _editingEntryId.asStateFlow()

    val isEditingEntry: Boolean
        get() = _editingEntryId.value != null

    /**
     * Actualiza el nombre del estudiante en el formulario.
     */
    fun onStudentNameChanged(value: String) {
        val error = if (value.isBlank()) "Tu nombre y apellido es obligatorio" else null
        _entryFormState.update { it.copy(studentName = value, studentNameError = error).validate() }
    }

    /**
     * Actualiza los contenidos trabajados.
     */
    fun onContentsChanged(value: String) {
        val error = if (value.isBlank()) "Los contenidos son obligatorios" else null
        _entryFormState.update { it.copy(contents = value, contentsError = error).validate() }
    }

    /**
     * Actualiza las actividades realizadas.
     */
    fun onActivitiesChanged(value: String) {
        val error = if (value.isBlank()) "Las actividades son obligatorias" else null
        _entryFormState.update { it.copy(activities = value, activitiesError = error).validate() }
    }

    /**
     * Actualiza los aprendizajes obtenidos.
     */
    fun onLearningsChanged(value: String) {
        val error = if (value.isBlank()) "Los aprendizajes son obligatorios" else null
        _entryFormState.update { it.copy(learnings = value, learningsError = error).validate() }
    }

    /**
     * Actualiza las dudas o dificultades opcionales.
     */
    fun onDoubtsChanged(value: String) {
        _entryFormState.update { it.copy(doubts = value) }
    }

    /**
     * Asigna la URI de la foto capturada con cámara o elegida de la galería.
     */
    fun setPhotoUri(uri: String?) {
        _entryFormState.update { it.copy(photoUri = uri) }
    }

    /**
     * Asigna las coordenadas GPS registradas.
     */
    fun setLocation(lat: Double?, lon: Double?) {
        _entryFormState.update { it.copy(latitude = lat, longitude = lon) }
    }

    private fun EntryFormState.validate(): EntryFormState {
        val valid = studentName.isNotBlank() && contents.isNotBlank() && activities.isNotBlank() && learnings.isNotBlank() &&
                studentNameError == null && contentsError == null && activitiesError == null && learningsError == null
        return copy(isFormValid = valid)
    }

    /**
     * Prepara el formulario para ingresar un nuevo registro.
     */
    fun prepareNewEntry(defaultStudentName: String = "") {
        _editingEntryId.value = null
        val currentName = if (defaultStudentName.isNotBlank()) defaultStudentName else _entryFormState.value.studentName
        val newState = EntryFormState(studentName = currentName)
        _entryFormState.value = if (currentName.isNotBlank()) newState.copy(studentNameError = null).validate() else newState
    }

    /**
     * Carga un registro previo en el formulario para editarlo.
     */
    fun prepareEditEntry(entry: LogbookEntryEntity) {
        _editingEntryId.value = entry.id
        _entryFormState.value = EntryFormState(
            studentName = entry.studentName,
            contents = entry.contents,
            activities = entry.activities,
            learnings = entry.learnings,
            doubts = entry.doubts,
            photoUri = entry.photoUri,
            latitude = entry.latitude,
            longitude = entry.longitude
        ).validate()
    }

    /**
     * Carga un registro por ID para preparar la edición.
     */
    fun loadAndPrepareEditEntry(entryId: Long) {
        viewModelScope.launch {
            val entry = repository.getEntryById(entryId)
            if (entry != null) {
                prepareEditEntry(entry)
            }
        }
    }

    /**
     * Envía un nuevo registro o guarda las modificaciones hechas a un registro existente.
     */
    fun submitEntry(logbookId: Long, onResult: (Boolean) -> Unit) {
        val state = _entryFormState.value
        if (!state.isFormValid) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val currentEditingId = _editingEntryId.value
            if (currentEditingId != null) {
                val existingEntry = repository.getEntryById(currentEditingId)
                if (existingEntry != null) {
                    val updated = existingEntry.copy(
                        studentName = state.studentName,
                        contents = state.contents,
                        activities = state.activities,
                        learnings = state.learnings,
                        doubts = state.doubts,
                        photoUri = state.photoUri,
                        latitude = state.latitude,
                        longitude = state.longitude
                    )
                    repository.updateEntry(updated)
                }
            } else {
                repository.insertEntry(
                    LogbookEntryEntity(
                        logbookId = logbookId,
                        studentName = state.studentName,
                        contents = state.contents,
                        activities = state.activities,
                        learnings = state.learnings,
                        doubts = state.doubts,
                        photoUri = state.photoUri,
                        latitude = state.latitude,
                        longitude = state.longitude
                    )
                )
            }
            _editingEntryId.value = null
            _entryFormState.value = EntryFormState(studentName = state.studentName)
            onResult(true)
        }
    }
}

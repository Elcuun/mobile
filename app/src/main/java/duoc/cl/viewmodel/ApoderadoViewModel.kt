package duoc.cl.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import duoc.cl.data.local.LogbookEntity
import duoc.cl.data.local.LogbookEntryEntity
import duoc.cl.data.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

/**
 * ViewModel del módulo para Apoderados.
 * Permite buscar las actividades registradas de un estudiante por su nombre o correo electrónico
 * y consultar las bitácoras, evidencias y retroalimentaciones entregadas por el profesor.
 */
class ApoderadoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository.getInstance(application)

    private val _studentName = MutableStateFlow("")
    val studentName: StateFlow<String> = _studentName.asStateFlow()

    private val _studentEmail = MutableStateFlow("")
    val studentEmail: StateFlow<String> = _studentEmail.asStateFlow()

    private val _isSearchSubmitted = MutableStateFlow(false)
    val isSearchSubmitted: StateFlow<Boolean> = _isSearchSubmitted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _entrySearchQuery = MutableStateFlow("")
    val entrySearchQuery: StateFlow<String> = _entrySearchQuery.asStateFlow()

    private val _entryStatusFilter = MutableStateFlow(EntryStatusFilter.ALL)
    val entryStatusFilter: StateFlow<EntryStatusFilter> = _entryStatusFilter.asStateFlow()

    /**
     * Establece el texto de búsqueda interna de las bitácoras.
     */
    fun setEntrySearchQuery(query: String) {
        _entrySearchQuery.value = query
    }

    /**
     * Establece el filtro de estado (Todas / Pendientes / Revisadas).
     */
    fun setEntryStatusFilter(filter: EntryStatusFilter) {
        _entryStatusFilter.value = filter
    }

    /**
     * Actualiza el nombre del estudiante ingresado por el apoderado.
     */
    fun onStudentNameChanged(value: String) {
        _studentName.value = value
        _errorMessage.value = null
    }

    /**
     * Actualiza el correo del estudiante ingresado por el apoderado.
     */
    fun onStudentEmailChanged(value: String) {
        _studentEmail.value = value
        _errorMessage.value = null
    }

    /**
     * Inicia la búsqueda del estudiante validando que se ingrese al menos el nombre o el correo.
     */
    fun searchStudent() {
        val name = _studentName.value.trim()
        val email = _studentEmail.value.trim()

        if (name.isBlank() && email.isBlank()) {
            _errorMessage.value = "Por favor ingrese el nombre o correo del estudiante"
            return
        }

        _errorMessage.value = null
        _isSearchSubmitted.value = true
    }

    /**
     * Reinicia la búsqueda para consultar otro estudiante.
     */
    fun resetSearch() {
        _isSearchSubmitted.value = false
        _entrySearchQuery.value = ""
        _entryStatusFilter.value = EntryStatusFilter.ALL
    }

    /**
     * Obtiene las bitácoras y registros del estudiante consultado por el apoderado,
     * aplicando coincidencia flexible por nombre o correo.
     */
    fun getStudentEntries(): Flow<List<Pair<LogbookEntity, LogbookEntryEntity>>> {
        val nameQuery = _studentName.value.trim()
        val emailQuery = _studentEmail.value.trim()
        val query = if (nameQuery.isNotBlank()) nameQuery else emailQuery
        return combine(
            repository.getEntriesByStudentQuery(query),
            _entrySearchQuery,
            _entryStatusFilter
        ) { list, searchQuery, statusFilter ->
            list.filter { (logbook, entry) ->
                val matchesSearch = searchQuery.isBlank() ||
                        logbook.title.contains(searchQuery, ignoreCase = true) ||
                        logbook.month.contains(searchQuery, ignoreCase = true) ||
                        entry.contents.contains(searchQuery, ignoreCase = true)
                val matchesStatus = when (statusFilter) {
                    EntryStatusFilter.ALL -> true
                    EntryStatusFilter.PENDING -> !entry.isReviewed
                    EntryStatusFilter.REVIEWED -> entry.isReviewed
                }
                matchesSearch && matchesStatus
            }
        }
    }

    /**
     * Obtiene el nombre legible del curso.
     */
    fun getCourseName(courseId: Long): String {
        return repository.getCourseById(courseId)?.name ?: "Curso #$courseId"
    }

    /**
     * Obtiene el nombre legible de la asignatura.
     */
    fun getSubjectName(subjectId: Long): String {
        return repository.getSubjectById(subjectId)?.name ?: "Asignatura #$subjectId"
    }
}

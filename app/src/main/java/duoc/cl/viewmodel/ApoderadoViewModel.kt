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

    fun onStudentNameChanged(value: String) {
        _studentName.value = value
        _errorMessage.value = null
    }

    fun onStudentEmailChanged(value: String) {
        _studentEmail.value = value
        _errorMessage.value = null
    }

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

    fun resetSearch() {
        _isSearchSubmitted.value = false
    }

    fun getStudentEntries(): Flow<List<Pair<LogbookEntity, LogbookEntryEntity>>> {
        val nameQuery = _studentName.value.trim()
        val emailQuery = _studentEmail.value.trim()
        val query = if (nameQuery.isNotBlank()) nameQuery else emailQuery
        return repository.getEntriesByStudentQuery(query)
    }

    fun getCourseName(courseId: Long): String {
        return repository.getCourseById(courseId)?.name ?: "Curso #$courseId"
    }

    fun getSubjectName(subjectId: Long): String {
        return repository.getSubjectById(subjectId)?.name ?: "Asignatura #$subjectId"
    }
}

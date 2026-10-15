package duoc.cl.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import duoc.cl.data.local.LogbookEntryEntity
import duoc.cl.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StudentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository.getInstance(application)

    val courses = repository.courses
    val subjects = repository.subjects

    fun getLogbooks(courseId: Long, subjectId: Long) = repository.getLogbooks(courseId, subjectId)

    fun getEntriesForLogbook(logbookId: Long) = repository.getEntriesByLogbook(logbookId)

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

    fun onStudentNameChanged(value: String) {
        val error = if (value.isBlank()) "Tu nombre y apellido es obligatorio" else null
        _entryFormState.update { it.copy(studentName = value, studentNameError = error).validate() }
    }

    fun onContentsChanged(value: String) {
        val error = if (value.isBlank()) "Los contenidos son obligatorios" else null
        _entryFormState.update { it.copy(contents = value, contentsError = error).validate() }
    }

    fun onActivitiesChanged(value: String) {
        val error = if (value.isBlank()) "Las actividades son obligatorias" else null
        _entryFormState.update { it.copy(activities = value, activitiesError = error).validate() }
    }

    fun onLearningsChanged(value: String) {
        val error = if (value.isBlank()) "Los aprendizajes son obligatorios" else null
        _entryFormState.update { it.copy(learnings = value, learningsError = error).validate() }
    }

    fun onDoubtsChanged(value: String) {
        _entryFormState.update { it.copy(doubts = value) }
    }

    fun setPhotoUri(uri: String?) {
        _entryFormState.update { it.copy(photoUri = uri) }
    }

    fun setLocation(lat: Double?, lon: Double?) {
        _entryFormState.update { it.copy(latitude = lat, longitude = lon) }
    }

    private fun EntryFormState.validate(): EntryFormState {
        val valid = studentName.isNotBlank() && contents.isNotBlank() && activities.isNotBlank() && learnings.isNotBlank() &&
                studentNameError == null && contentsError == null && activitiesError == null && learningsError == null
        return copy(isFormValid = valid)
    }

    fun submitEntry(logbookId: Long, onResult: (Boolean) -> Unit) {
        val state = _entryFormState.value
        if (!state.isFormValid) {
            onResult(false)
            return
        }
        viewModelScope.launch {
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
            _entryFormState.value = EntryFormState()
            onResult(true)
        }
    }
}

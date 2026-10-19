package duoc.cl.data.repository

import android.content.Context
import android.content.SharedPreferences
import duoc.cl.data.local.Course
import duoc.cl.data.local.LogbookEntity
import duoc.cl.data.local.LogbookEntryEntity
import duoc.cl.data.local.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repositorio principal de la aplicación para gestionar la persistencia local
 * de Cursos, Asignaturas, Bitácoras Mensuales y Registros/Aportes de Estudiantes.
 * Utiliza SharedPreferences con serialización JSON para garantizar persistencia offline.
 */
class AppRepository private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Lista reactiva de Cursos disponibles
    private val _courses = MutableStateFlow(
        listOf(
            Course(1L, "5.º Básico A"),
            Course(2L, "6.º Básico A"),
            Course(3L, "7.º Básico A"),
            Course(4L, "8.º Básico A")
        )
    )
    val courses: Flow<List<Course>> = _courses.asStateFlow()

    // Lista reactiva de Asignaturas disponibles
    private val _subjects = MutableStateFlow(
        listOf(
            Subject(1L, "Matemáticas"),
            Subject(2L, "Lenguaje y Comunicación"),
            Subject(3L, "Ciencias Naturales"),
            Subject(4L, "Historia y Geografía")
        )
    )
    val subjects: Flow<List<Subject>> = _subjects.asStateFlow()

    private val _logbooks = MutableStateFlow<List<LogbookEntity>>(emptyList())
    private val _entries = MutableStateFlow<List<LogbookEntryEntity>>(emptyList())

    init {
        loadLogbooks()
        loadEntries()
    }

    // --- PERSISTENCIA DE BITÁCORAS ---

    /**
     * Carga las bitácoras almacenadas localmente en SharedPreferences.
     * Si no existen registros guardados, inicializa el conjunto con datos demo.
     */
    private fun loadLogbooks() {
        val jsonString = prefs.getString(KEY_LOGBOOKS, null)
        if (jsonString.isNullOrBlank()) {
            val initial = getInitialLogbooks()
            _logbooks.value = initial
            saveLogbooksToPrefs(initial)
        } else {
            val list = parseLogbooksJson(jsonString)
            _logbooks.value = list
        }
    }

    /**
     * Retorna el conjunto inicial de bitácoras de ejemplo para demostración.
     */
    private fun getInitialLogbooks(): List<LogbookEntity> {
        return listOf(
            LogbookEntity(
                id = 101L,
                courseId = 1L,
                subjectId = 1L,
                month = "Marzo 2025",
                title = "Bitácora Mensual: Números y Operaciones",
                purpose = "Registrar avances y comprensión de fracciones y decimales en clases.",
                instructions = "Ingrese detalladamente los contenidos trabajados, actividades, aprendizajes logrados y dudas.",
                guidingQuestions = "¿Qué te pareció más desafiante de las fracciones? ¿Cómo lo resolviste?"
            ),
            LogbookEntity(
                id = 102L,
                courseId = 4L,
                subjectId = 3L,
                month = "Marzo 2025",
                title = "Bitácora Mensual: Ecosistemas y Biodiversidad",
                purpose = "Analizar las interacciones entre organismos, flujo de energía y materia en ecosistemas de Chile.",
                instructions = "Registre las observaciones de clases, experimentos de laboratorio y conclusiones sobre las cadenas tróficas.",
                guidingQuestions = "¿Cómo influye la alteración de un componente en el equilibrio del ecosistema de San Bernardo?"
            ),
            LogbookEntity(
                id = 103L,
                courseId = 4L,
                subjectId = 1L,
                month = "Marzo 2025",
                title = "Bitácora Mensual: Álgebra y Funciones",
                purpose = "Comprender y aplicar expresiones algebraicas y ecuaciones de primer grado.",
                instructions = "Anote los pasos seguidos para resolver ecuaciones y plantear problemas matemáticos.",
                guidingQuestions = "¿Qué estrategia utilizaste para despejar la incógnita en la ecuación?"
            )
        )
    }

    /**
     * Parsea una cadena JSON en una lista de objetos LogbookEntity.
     */
    private fun parseLogbooksJson(jsonString: String): List<LogbookEntity> {
        val list = mutableListOf<LogbookEntity>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    LogbookEntity(
                        id = obj.optLong("id"),
                        courseId = obj.optLong("courseId"),
                        subjectId = obj.optLong("subjectId"),
                        month = obj.optString("month"),
                        title = obj.optString("title"),
                        purpose = obj.optString("purpose"),
                        instructions = obj.optString("instructions"),
                        guidingQuestions = obj.optString("guidingQuestions"),
                        isClosed = obj.optBoolean("isClosed"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Guarda la lista actual de bitácoras en SharedPreferences en formato JSON.
     */
    private fun saveLogbooksToPrefs(list: List<LogbookEntity>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("courseId", item.courseId)
                    put("subjectId", item.subjectId)
                    put("month", item.month)
                    put("title", item.title)
                    put("purpose", item.purpose)
                    put("instructions", item.instructions)
                    put("guidingQuestions", item.guidingQuestions)
                    put("isClosed", item.isClosed)
                    put("createdAt", item.createdAt)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_LOGBOOKS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- PERSISTENCIA DE REGISTROS DE ESTUDIANTES ---

    /**
     * Carga los registros/aportes de estudiantes desde SharedPreferences.
     * Si no existen registros previos, carga los de ejemplo asegurando el nombre correcto.
     */
    private fun loadEntries() {
        val jsonString = prefs.getString(KEY_ENTRIES, null)
        if (jsonString.isNullOrBlank()) {
            val initial = getInitialEntries()
            _entries.value = initial
            saveEntriesToPrefs(initial)
        } else {
            val list = parseEntriesJson(jsonString)
            // Corregir variaciones de nombres de estudiantes en datos previos
            val updatedList = list.map { entry ->
                if (entry.studentName.contains("Lucas Prez", ignoreCase = true)) {
                    entry.copy(studentName = "Lucas Pérez")
                } else {
                    entry
                }
            }
            _entries.value = updatedList
            if (updatedList != list) {
                saveEntriesToPrefs(updatedList)
            }
        }
    }

    /**
     * Retorna los registros iniciales de estudiantes con nombres estandarizados ("Lucas Pérez").
     */
    private fun getInitialEntries(): List<LogbookEntryEntity> {
        return listOf(
            LogbookEntryEntity(
                id = 1001L,
                logbookId = 101L,
                studentName = "Ignacia Soto",
                contents = "Fracciones propias e impropias",
                activities = "Ejercicios prácticos en guía de trabajo y pizarra.",
                learnings = "Comprendí cómo amplificar y simplificar fracciones.",
                doubts = "Me costó un poco la suma con distinto denominador.",
                isReviewed = true,
                feedback = "¡Muy buen trabajo Ignacia! Repasaremos mínimo común múltiplo la próxima clase."
            ),
            LogbookEntryEntity(
                id = 1002L,
                logbookId = 102L,
                studentName = "Lucas Pérez",
                contents = "Cadenas tróficas y flujo de energía",
                activities = "Esquema y análisis de red alimentaria en la reserva nacional.",
                learnings = "Entendí que los productores son la base de todo ecosistema.",
                doubts = "¿Qué ocurre cuando una especie clave desaparece?",
                isReviewed = false,
                feedback = null
            )
        )
    }

    /**
     * Parsea una cadena JSON en una lista de LogbookEntryEntity.
     */
    private fun parseEntriesJson(jsonString: String): List<LogbookEntryEntity> {
        val list = mutableListOf<LogbookEntryEntity>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawName = obj.optString("studentName")
                val cleanName = if (rawName.contains("Lucas Prez", ignoreCase = true)) "Lucas Pérez" else rawName
                list.add(
                    LogbookEntryEntity(
                        id = obj.optLong("id"),
                        logbookId = obj.optLong("logbookId"),
                        studentName = cleanName,
                        contents = obj.optString("contents"),
                        activities = obj.optString("activities"),
                        learnings = obj.optString("learnings"),
                        doubts = obj.optString("doubts"),
                        photoUri = if (obj.has("photoUri") && !obj.isNull("photoUri")) obj.optString("photoUri") else null,
                        latitude = if (obj.has("latitude") && !obj.isNull("latitude")) obj.optDouble("latitude") else null,
                        longitude = if (obj.has("longitude") && !obj.isNull("longitude")) obj.optDouble("longitude") else null,
                        feedback = if (obj.has("feedback") && !obj.isNull("feedback")) obj.optString("feedback") else null,
                        isReviewed = obj.optBoolean("isReviewed"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Guarda la lista de registros de estudiantes en SharedPreferences.
     */
    private fun saveEntriesToPrefs(list: List<LogbookEntryEntity>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("logbookId", item.logbookId)
                    put("studentName", item.studentName)
                    put("contents", item.contents)
                    put("activities", item.activities)
                    put("learnings", item.learnings)
                    put("doubts", item.doubts)
                    put("photoUri", item.photoUri ?: JSONObject.NULL)
                    put("latitude", item.latitude ?: JSONObject.NULL)
                    put("longitude", item.longitude ?: JSONObject.NULL)
                    put("feedback", item.feedback ?: JSONObject.NULL)
                    put("isReviewed", item.isReviewed)
                    put("createdAt", item.createdAt)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_ENTRIES, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- MÉTODOS PÚBLICOS DEL REPOSITORIO ---

    /**
     * Obtiene el flujo continuo de bitácoras filtradas por curso y asignatura.
     */
    fun getLogbooks(courseId: Long, subjectId: Long): Flow<List<LogbookEntity>> {
        return _logbooks.map { list -> list.filter { it.courseId == courseId && it.subjectId == subjectId } }
    }

    /**
     * Inserta una nueva bitácora mensual creada por el docente.
     */
    suspend fun insertLogbook(logbook: LogbookEntity): Long {
        val newLogbook = logbook.copy(id = System.currentTimeMillis())
        val current = _logbooks.value.toMutableList()
        current.add(newLogbook)
        _logbooks.value = current
        saveLogbooksToPrefs(current)
        return newLogbook.id
    }

    /**
     * Actualiza una bitácora mensual existente.
     */
    suspend fun updateLogbook(logbook: LogbookEntity) {
        val current = _logbooks.value.toMutableList()
        val index = current.indexOfFirst { it.id == logbook.id }
        if (index != -1) {
            current[index] = logbook
            _logbooks.value = current
            saveLogbooksToPrefs(current)
        }
    }

    /**
     * Elimina una bitácora y todos sus registros asociados.
     */
    suspend fun deleteLogbook(id: Long) {
        val currentLogbooks = _logbooks.value.toMutableList()
        currentLogbooks.removeAll { it.id == id }
        _logbooks.value = currentLogbooks
        saveLogbooksToPrefs(currentLogbooks)

        val currentEntries = _entries.value.toMutableList()
        currentEntries.removeAll { it.logbookId == id }
        _entries.value = currentEntries
        saveEntriesToPrefs(currentEntries)
    }

    /**
     * Busca una bitácora específica por su identificador único.
     */
    suspend fun getLogbookById(id: Long): LogbookEntity? {
        return _logbooks.value.firstOrNull { it.id == id }
    }

    /**
     * Obtiene el flujo de registros asociados a una bitácora.
     */
    fun getEntriesByLogbook(logbookId: Long): Flow<List<LogbookEntryEntity>> {
        return _entries.map { list -> list.filter { it.logbookId == logbookId } }
    }

    /**
     * Busca los registros de estudiantes mediante coincidencia flexible de nombre o correo (para el Apoderado).
     * Normaliza tildes y mayúsculas/minúsculas para garantizar resultados exactos.
     */
    fun getEntriesByStudentQuery(studentQuery: String): Flow<List<Pair<LogbookEntity, LogbookEntryEntity>>> {
        return _entries.map { entriesList ->
            val cleanQuery = normalizeText(studentQuery)
            val filtered = if (cleanQuery.isEmpty()) {
                entriesList
            } else {
                entriesList.filter { entry ->
                    val cleanStudentName = normalizeText(entry.studentName)
                    // Coincidencia bidireccional tolerante a acentos
                    cleanStudentName.contains(cleanQuery) || cleanQuery.contains(cleanStudentName) ||
                            cleanQuery.split(" ").any { part -> part.length > 2 && cleanStudentName.contains(part) }
                }
            }
            filtered.mapNotNull { entry ->
                val logbook = _logbooks.value.firstOrNull { it.id == entry.logbookId }
                if (logbook != null) logbook to entry else null
            }
        }
    }

    /**
     * Función auxiliar para remover tildes y caracteres especiales en búsquedas.
     */
    private fun normalizeText(text: String): String {
        return text.trim().lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
    }

    /**
     * Obtiene la entidad Curso por su ID.
     */
    fun getCourseById(courseId: Long): Course? {
        return _courses.value.firstOrNull { it.id == courseId }
    }

    /**
     * Obtiene la entidad Asignatura por su ID.
     */
    fun getSubjectById(subjectId: Long): Subject? {
        return _subjects.value.firstOrNull { it.id == subjectId }
    }

    /**
     * Inserta un nuevo registro/aporte de un estudiante.
     */
    suspend fun insertEntry(entry: LogbookEntryEntity): Long {
        val newEntry = entry.copy(id = System.currentTimeMillis())
        val current = _entries.value.toMutableList()
        current.add(newEntry)
        _entries.value = current
        saveEntriesToPrefs(current)
        return newEntry.id
    }

    /**
     * Actualiza un registro/aporte existente de un estudiante.
     */
    suspend fun updateEntry(entry: LogbookEntryEntity) {
        val current = _entries.value.toMutableList()
        val index = current.indexOfFirst { it.id == entry.id }
        if (index != -1) {
            current[index] = entry
            _entries.value = current
            saveEntriesToPrefs(current)
        }
    }

    /**
     * Busca un registro por su ID.
     */
    suspend fun getEntryById(id: Long): LogbookEntryEntity? {
        return _entries.value.firstOrNull { it.id == id }
    }

    companion object {
        private const val PREFS_NAME = "app_logbook_prefs_v2"
        private const val KEY_LOGBOOKS = "saved_logbooks_v2"
        private const val KEY_ENTRIES = "saved_entries_v2"

        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AppRepository(context)
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(): AppRepository {
            return INSTANCE ?: throw IllegalStateException("AppRepository no está inicializado. Llame a getInstance(context) primero.")
        }
    }
}

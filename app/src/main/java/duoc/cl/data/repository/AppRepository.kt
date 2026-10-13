package duoc.cl.data.repository

import duoc.cl.data.local.Course
import duoc.cl.data.local.LogbookEntity
import duoc.cl.data.local.LogbookEntryEntity
import duoc.cl.data.local.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class AppRepository private constructor() {

    private val _courses = MutableStateFlow(
        listOf(
            Course(1L, "5.º Básico A"),
            Course(2L, "6.º Básico A"),
            Course(3L, "7.º Básico A"),
            Course(4L, "8.º Básico A")
        )
    )
    val courses: Flow<List<Course>> = _courses.asStateFlow()

    private val _subjects = MutableStateFlow(
        listOf(
            Subject(1L, "Matemáticas"),
            Subject(2L, "Lenguaje y Comunicación"),
            Subject(3L, "Ciencias Naturales"),
            Subject(4L, "Historia y Geografía")
        )
    )
    val subjects: Flow<List<Subject>> = _subjects.asStateFlow()

    private val _logbooks = MutableStateFlow(
        mutableListOf(
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
    )

    private val _entries = MutableStateFlow(
        mutableListOf(
            LogbookEntryEntity(
                id = 1001L,
                logbookId = 101L,
                studentName = "Ignacia Soto",
                contents = "Fracciones propias y impropias",
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
    )

    fun getLogbooks(courseId: Long, subjectId: Long): Flow<List<LogbookEntity>> {
        return _logbooks.map { list -> list.filter { it.courseId == courseId && it.subjectId == subjectId } }
    }

    suspend fun insertLogbook(logbook: LogbookEntity): Long {
        val newLogbook = logbook.copy(id = System.currentTimeMillis())
        val current = _logbooks.value.toMutableList()
        current.add(newLogbook)
        _logbooks.value = current
        return newLogbook.id
    }

    suspend fun updateLogbook(logbook: LogbookEntity) {
        val current = _logbooks.value.toMutableList()
        val index = current.indexOfFirst { it.id == logbook.id }
        if (index != -1) {
            current[index] = logbook
            _logbooks.value = current
        }
    }

    suspend fun getLogbookById(id: Long): LogbookEntity? {
        return _logbooks.value.firstOrNull { it.id == id }
    }

    fun getEntriesByLogbook(logbookId: Long): Flow<List<LogbookEntryEntity>> {
        return _entries.map { list -> list.filter { it.logbookId == logbookId } }
    }

    suspend fun insertEntry(entry: LogbookEntryEntity): Long {
        val newEntry = entry.copy(id = System.currentTimeMillis())
        val current = _entries.value.toMutableList()
        current.add(newEntry)
        _entries.value = current
        return newEntry.id
    }

    suspend fun updateEntry(entry: LogbookEntryEntity) {
        val current = _entries.value.toMutableList()
        val index = current.indexOfFirst { it.id == entry.id }
        if (index != -1) {
            current[index] = entry
            _entries.value = current
        }
    }

    suspend fun getEntryById(id: Long): LogbookEntryEntity? {
        return _entries.value.firstOrNull { it.id == id }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AppRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}

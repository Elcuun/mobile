package duoc.cl.data.local

import java.util.UUID

enum class Role(val displayName: String) {
    PROFESOR("Docente"),
    ALUMNO("Estudiante"),
    APODERADO("Apoderado");

    companion object {
        fun fromKey(key: String?): Role {
            return when (key?.uppercase()) {
                "PROFESOR", "TEACHER", "DOCENTE" -> PROFESOR
                "ALUMNO", "STUDENT", "ESTUDIANTE" -> ALUMNO
                "APODERADO", "PARENT", "GUARDIAN" -> APODERADO
                else -> ALUMNO
            }
        }
    }
}

data class User(
    val id: String = UUID.randomUUID().toString(),
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val role: Role,
    val courseId: Long? = null
) {
    val fullName: String
        get() = "$firstName $lastName"
}
